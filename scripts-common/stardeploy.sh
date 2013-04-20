#!/bin/bash
# Author: Tyler Jensen
# =========================
# DESCRIPTION: Pulls down starexec trunk, builds it with ant, packages it into a war and deploys it to the server. 
# =========================
#

WEB_HOME=/project/tomcat-webapps/webapps
BACKUP_HOME=$WEB_HOME/backup
#NEW_DIR=$BACKUP_HOME/`date +%m%d%y%s`  
STAR_HOME=/home/starexec
STAR_DEPLOY=/home/starexec/deployed
WAR=starexec.war
STAR_WAR=$WEB_HOME/$WAR
SVN_URL=''

# Setup the branch the user wants to pull from
SVN_URL='https://svn.divms.uiowa.edu/repos/clc/projects/starexec/dev/'
if [ "$1" == "-trunk" ]; then
	SVN_URL=$SVN_URL/trunk/starexec
else
	if [ "$1" == "-fb1" ]; then
		SVN_URL=$SVN_URL/branches/fb1/starexec
	else
		if [ "$1" == "-fb2" ]; then
			SVN_URL=$SVN_URL/branches/fb2/starexec
		else
			echo "please specify a repository to use [-trunk, -fb1, -fb2]"
			exit 0
		fi
	fi
fi

echo Clearing directory: $STAR_DEPLOY
rm -rf $STAR_DEPLOY

# Pull the trunk into the temp folder
echo Pulling starexec trunk, please use your DIVMS credentials
cd $STAR_HOME
svn co $SVN_URL deployed -q

cd $STAR_DEPLOY/
echo "Entering $STAR_DEPLOY"
echo "Copying $STAR_HOME/production.properties to local.properties"
cp $STAR_HOME/production.properties local.properties

echo Starting ANT build process...
ant -buildfile build.xml -q
echo Starexec compilation and configuration SUCCESSFUL

echo Moving existing war file: $STAR_WAR
rm -f $STAR_WAR
#mkdir $NEW_DIR
#mv $STAR_WAR $NEW_DIR/starBackup.war

echo Waiting for tomcat to move previous starexec version
while [ -e "$STAR_WAR" ]
do
	sleep 1s
	echo -n '.'
done

echo Copying new WAR to $WEB_HOME
cp $STAR_DEPLOY/$WAR $WEB_HOME/$WAR

echo Changing permissions on $STAR_WAR
chmod 775 $STAR_WAR

# Restarting tomcat will explode the starexec.war into a webapp
echo Restarting Tomcat, please use your StarDev credentials...
sudo /sbin/service tomcat7 restart

#echo Cleaning up $STAR_DEPLOY
#rm -rf $STAR_DEPLOY
echo See $STAR_DEPLOY for deployed code.

echo Starexec production deployment SUCCESSFUL
echo Please manually update the database as needed. Schema and stored procedure updates are not included in this deployment
