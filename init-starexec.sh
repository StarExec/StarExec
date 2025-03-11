#!/bin/bash

# Immediately exit on errors, treat unset vars as errors, and fail on pipe errors
set -euo pipefail

function error() {
  echo "[ERROR] $1"
  exit 1
}

function cleanup() {
  echo "Container stopped, performing cleanup..."
  
  # Attempt a graceful shutdown of Tomcat
  /project/apache-tomcat-7/bin/shutdown.sh || true
  
  # Wait briefly for Tomcat to finish cleaning up
  sleep 5
  
  # Forcibly kill any remaining Tomcat processes (matching the Bootstrap class)
  pkill -f 'org.apache.catalina.startup.Bootstrap' || true
  
  /usr/bin/mysqladmin -u root shutdown || true
  /usr/sbin/apache2ctl -k graceful-stop || true
  exit 0
}

# Trap signals for cleanup
trap cleanup SIGINT SIGTERM

# Verify essential environment variables are set
: "${DB_NAME:?DB_NAME is not set}"
: "${DB_USER:?DB_USER is not set}"
: "${DB_PASS:?DB_PASS is not set}"
: "${DEPLOY_DIR:?DEPLOY_DIR is not set}"
: "${BUILD_FILE:?BUILD_FILE is not set}"
: "${SQL_FILE:?SQL_FILE is not set}"

# Configure permissions
chown -R tomcat:star-web /project
chown -R tomcat:star-web /home/starexec
chown -R tomcat:star-web /home/sandbox
chown -R mysql:mysql /var/lib/mysql
chmod 755 -R /home/starexec

# Start Apache in the background
echo "Starting Apache..."
/usr/sbin/apache2ctl -D FOREGROUND &

# Initialize MySQL data directory if not already initialized
if [ ! -d "/var/lib/mysql/mysql" ]; then
  echo "Initializing MySQL data directory..."
  mysql_install_db --user=mysql --ldata=/var/lib/mysql
  chown -R mysql:mysql /var/lib/mysql
fi

# Start MySQL in the background
echo "Starting MySQL..."
/usr/bin/mysqld_safe --basedir=/usr --user=mysql &

# Wait for MySQL to start
MYSQL_START_TIMEOUT=60
MYSQL_START_INTERVAL=1
MYSQL_START_ELAPSED=0

until mysqladmin ping &>/dev/null; do
  if [ "$MYSQL_START_ELAPSED" -ge "$MYSQL_START_TIMEOUT" ]; then
    error "MySQL failed to start within $MYSQL_START_TIMEOUT seconds."
  fi
  echo "Waiting for MySQL to start... ($MYSQL_START_ELAPSED/$MYSQL_START_TIMEOUT)"
  sleep "$MYSQL_START_INTERVAL"
  MYSQL_START_ELAPSED=$((MYSQL_START_ELAPSED + MYSQL_START_INTERVAL))
done

# Configure the database
echo "Configuring database..."
mysql -u root -e "
  DROP DATABASE IF EXISTS $DB_NAME;
  CREATE DATABASE $DB_NAME;
  GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
  FLUSH PRIVILEGES;
"

# Configure Podman connection
echo "Configuring Podman connection..."
podman system connection add host-machine-podman-connection \
  ssh://${SSH_USERNAME}@${HOST_MACHINE}:${SSH_PORT}${SOCKET_PATH} \
  --identity=/root/.ssh/starexec_podman_key || echo "Podman connection configuration failed"

# Start Tomcat
echo "Starting Tomcat..."
CATALINA_OPTS="${CATALINA_OPTS:-} -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false"
/project/apache-tomcat-7/bin/catalina.sh run &

# Wait for Tomcat to start
until curl -s http://localhost:8080 >/dev/null; do
  echo "Waiting for Tomcat to start..."
  sleep 1
done

# Soft deploy StarExec
cd "$DEPLOY_DIR" || error "Cannot change directory to $DEPLOY_DIR"
echo "Running ant build -buildfile $BUILD_FILE reload-sql update-sql..."

if ! ant build -buildfile "$BUILD_FILE" reload-sql update-sql; then
  echo "reload-sql/update-sql failed, applying NewInstall.sql..."
  cd "$DEPLOY_DIR/sql" || error "Cannot change directory to $DEPLOY_DIR/sql"
  mysql -u root "$DB_NAME" < "$SQL_FILE"
  cd "$DEPLOY_DIR" || error "Cannot change directory back to $DEPLOY_DIR"
  
  if ! ant build -buildfile "$BUILD_FILE" reload-sql update-sql; then
    error "ERROR: Build still failing after applying NewInstall.sql. Please rebuild the Docker image."
  fi
fi

script/soft-deploy.sh && printf "SUCCESS! VISIT IN YOUR BROWSER: http://localhost\n\nuser: admin\npassword: admin\n\n"

# Keep the container running; wait on background jobs
wait
