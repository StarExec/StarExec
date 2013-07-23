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
SVN_PW='bBcs5.imTuWuLbo'

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
		    if [ "$1" == "-deployed" ]; then
                       if [ "$2" == "" ] ; then
                         echo "Please supply the name of the deployed release with -deployed.";
                         exit;
                       fi
		       SVN_URL=$SVN_URL/deployed/$2/starexec
		    else
			echo "please specify a branch to use [-trunk, -fb1, -fb2, -deployed DATE]"
			exit 0
		    fi
		fi
	fi
fi

echo Clearing directory: $STAR_DEPLOY
rm -rf $STAR_DEPLOY

# Pull the trunk into the temp folder
echo Pulling starexec codebase from svn repo.  Please use your DIVMS credentials.
cd $STAR_HOME
# jt: changed to use the generic username/pw for repo
svn --username starexec_deploy --password $SVN_PW co $SVN_URL deployed -q
# svn co $SVN_URL deployed -q

cd $STAR_DEPLOY/
echo "Entering $STAR_DEPLOY"

if [ `hostname` == "stardev.cs.uiowa.edu" ] ; then
  echo "Detected that we are on StarDev";
  echo "Copying production.properties to local.properties"
  cp stardev.properties local.properties
else
  echo "Detected that we are on Production";
  echo "Copying production.properties to local.properties"
  cp production.properties local.properties
fi

echo Starting ANT build process...
ant -buildfile build.xml -q
echo "Starexec compilation and configuration SUCCESSFUL in $STAR_DEPLOY"

echo "This does not include SQL updates or deployment to tomcat."
