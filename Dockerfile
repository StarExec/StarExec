# ---------------------------
# Stage 1: Build runsolver
# ---------------------------
FROM --platform=linux/amd64 ubuntu:22.04 AS builder

# Install build dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    build-essential \
    make \
    gcc \
    g++ \
    git \
    libnuma-dev && \
    rm -rf /var/lib/apt/lists/*

# Clone StarExec repository specifically for runsolver
COPY . /StarExec

# Build runsolver
WORKDIR /StarExec/src/org/starexec/config/sge/RunSolverSource
RUN make clean && make

# ---------------------------
# Stage 2: Final runtime
# ---------------------------
FROM --platform=linux/amd64 ubuntu:22.04

LABEL maintainer="Starexec Team"

ENV DB_USER se_admin
ENV DB_PASS dfsdf34RFerfg3TFGRfrF3edFVg12few2

ENV DEBIAN_FRONTEND=noninteractive \
    TOMCAT_VERSION=7.0.109 \
    MYSQL_CON_VERSION=8.0.30 \
    DB_NAME=starexec \
    DB_USER=${DB_USER} \
    DB_PASS=${DB_PASS} \
    DEPLOY_DIR=/home/starexec/StarExec-deploy \
    SQL_FILE=/home/starexec/StarExec-deploy/sql/NewInstall.sql \
    BUILD_FILE=build.xml \
    SSH_USERNAME=starexec \
    HOST_MACHINE=localhost \
    SSH_PORT=22 \
    SOCKET_PATH=/run/user/1000/podman/podman.sock \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8" \
    TZ=UTC

# Update and install runtime dependencies
RUN apt-get update --fix-missing && \
    apt-get install -y --no-install-recommends --no-install-suggests \
    ca-certificates tzdata sudo git unzip file apache2 tcsh libnuma-dev \
    openssl dnsutils curl ant ant-optional mariadb-client mariadb-server podman locales && \
    # Download and install Temurin JDK 16 directly
    curl -fsSL -o /tmp/jdk16.tar.gz https://github.com/adoptium/temurin16-binaries/releases/download/jdk-16.0.2%2B7/OpenJDK16U-jdk_x64_linux_hotspot_16.0.2_7.tar.gz && \
    mkdir -p /usr/lib/jvm && \
    tar xzf /tmp/jdk16.tar.gz -C /usr/lib/jvm && \
    rm /tmp/jdk16.tar.gz && \
    # Set Java alternatives
    update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-16.0.2+7/bin/java 1 && \
    update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-16.0.2+7/bin/javac 1 && \
    # Cleanup and final steps
    a2enmod ssl && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Configure timezone
RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata

# Install and configure locales
RUN sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/g' /etc/locale.gen && \
    locale-gen en_US.UTF-8

# Generate local SSL key and certificate
RUN printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth" > /tmp/openssl.cnf && \
    openssl req -x509 -out /etc/ssl/certs/localhost.crt -keyout /etc/ssl/private/localhost.key \
    -newkey rsa:2048 -nodes -sha256 \
    -subj '/CN=localhost' -extensions EXT -config /tmp/openssl.cnf && \
    rm /tmp/openssl.cnf

# Install Node.js and Sass
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs --no-install-recommends --no-install-suggests && \
    npm install -g npm@latest && \
    npm install -g sass

# Configure Apache2
COPY conf/ssl.conf /etc/apache2/sites-available/
COPY conf/starexec.conf /etc/apache2/sites-available/
RUN a2dissite 000-default.conf default-ssl.conf && \
    a2ensite ssl starexec && \
    a2enmod proxy headers proxy_http rewrite && \
    mkdir -p /etc/apache2/logs/ && \
    chown -R www-data:www-data /etc/apache2/logs/ && \
    chmod 755 /etc/apache2/logs/

# Configure Tomcat
RUN mkdir -p /project && cd /project && \
    curl -fsSL https://archive.apache.org/dist/tomcat/tomcat-7/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz | tar -xz && \
    mv apache-tomcat-${TOMCAT_VERSION} apache-tomcat-7 && \
    curl -fsSL -o /project/apache-tomcat-7/lib/mysql-connector-java-${MYSQL_CON_VERSION}.jar \
    https://search.maven.org/remotecontent?filepath=mysql/mysql-connector-java/${MYSQL_CON_VERSION}/mysql-connector-java-${MYSQL_CON_VERSION}.jar

# Create groups and users
RUN groupadd -g 160 star-web && \
    groupadd -g 153 tomcat && \
    groupadd -g 111 sandbox && \
    groupadd -g 112 sandbox2 && \
    useradd -r -m -d /home/tomcat -s /bin/bash -c "Tomcat User" -u 153 -g 160 tomcat && \
    useradd -r -m -d /home/starexec -s /bin/bash -c "Starexec User" -u 152 -g 160 starexec && \
    useradd -r -m -d /home/sandbox -s /bin/bash -c "Cluster UserOne" -u 111 -g 111 sandbox && \
    useradd -r -m -d /home/sandbox2 -s /bin/bash -c "Cluster UserTwo" -u 112 -g 112 sandbox2 && \
    usermod -aG star-web sandbox && \
    usermod -aG star-web sandbox2 && \
    usermod -aG star-web tomcat && \
    usermod -aG star-web starexec

# Configure directories and permissions
RUN mkdir -p /export/starexec/sandbox /export/starexec/sandbox2 && \
    chown -R tomcat:star-web /export/starexec && \
    mkdir -p /local/sandbox /local/sandbox2 && \
    chown sandbox:sandbox /local/sandbox && \
    chown sandbox2:sandbox2 /local/sandbox2 && \
    chmod 770 /local/sandbox /local/sandbox2 && \
    chmod g+s /local/sandbox /local/sandbox2 && \
    usermod -aG sandbox tomcat && \
    usermod -aG sandbox2 tomcat

# Clone StarExec repository
COPY . ${DEPLOY_DIR}
RUN git config --global --add safe.directory ${DEPLOY_DIR} && \
    cd ${DEPLOY_DIR}/WebContent/css/details && ln -s ../shared && \
    chown -R tomcat:star-web /home/starexec

COPY overrides.properties /tmp/
RUN mkdir -p ${DEPLOY_DIR}/build && \
    touch ${DEPLOY_DIR}/build/Cluster.MachineSpecs.txt && \
    chown starexec:star-web ${DEPLOY_DIR}/build/Cluster.MachineSpecs.txt && \
    touch ${DEPLOY_DIR}/build/overrides.properties && \
    cat /tmp/overrides.properties >> ${DEPLOY_DIR}/build/overrides.properties && \
    chown starexec:star-web ${DEPLOY_DIR}/build/overrides.properties && \
    rm /tmp/overrides.properties

# Configure sudo
COPY conf/sudoRules.txt /etc/sudoers.d/starexec
RUN chmod 0440 /etc/sudoers.d/starexec && \
    visudo -c -f /etc/sudoers.d/starexec

# Configure GetComputerInfo
RUN mkdir -p /home/starexec/bin && \
    chown tomcat:star-web /home/starexec/bin && \
    chmod 755 /home/starexec/bin
COPY solverAdditions/GetComputerInfo /home/starexec/bin/
RUN chown tomcat:star-web /home/starexec/bin/GetComputerInfo && \
    chmod 755 /home/starexec/bin/GetComputerInfo

# Copy the compiled runsolver from the builder stage
COPY --from=builder /StarExec/src/org/starexec/config/sge/RunSolverSource/runsolver ${DEPLOY_DIR}/src/org/starexec/config/sge/

# Install kubectl
RUN if [ "$(uname -m)" = "x86_64" ]; then \
        curl -fsSLO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"; \
    else \
        curl -fsSLO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/arm64/kubectl"; \
    fi && \
    chmod +x ./kubectl && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl && \
    rm kubectl

# Copy initialization script
COPY init-starexec.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/init-starexec.sh

# Copy SSH key for Podman
COPY starexec_podman_key /root/.ssh/starexec_podman_key
RUN chmod 600 /root/.ssh/starexec_podman_key

# Expose necessary ports
EXPOSE 80 443 3306

WORKDIR ${DEPLOY_DIR}

CMD ["/usr/local/bin/init-starexec.sh"]
