#!/bin/bash


# Prints an error and exits if the given directory does not exist
# $1 The directory
function failIfNotExists {
if [ ! -d "$1" ]; then
  echo "Error: $1 does not exist. Failed to create deploy directory"
  exit 1 
fi

}



#Directory into which starexec has been checked out
STAREXEC_CHECKOUT_DIR="/home/starexec/starexec_eaburns"
STAREXEC_VERSION_NUMBER="1"



failIfNotExists $STAREXEC_CHECKOUT_DIR

cd $STAREXEC_CHECKOUT_DIR

DEPLOY_DIR="$STAREXEC_CHECKOUT_DIR/starexec-deploy-version-$STAREXEC_VERSION_NUMBER"
ZIP_DIR="$STAREXEC_CHECKOUT_DIR/starexec-deploy-version-$STAREXEC_VERSION_NUMBER"
mkdir $DEPLOY_DIR

cp -R src $DEPLOY_DIR/src
mv $DEPLOY_DIR/src/org/starexec/config/starexec-config-deploy.xml $DEPLOY_DIR/src/org/starexec/config/starexec-config.xml


cp -R distribution $DEPLOY_DIR/distribution
rm "$DEPLOY_DIR/distribution/Developer Distribution Notes"
cp -R schemas $DEPLOY_DIR/schemas
cp -R scripts-common $DEPLOY_DIR/scripts-common
rm $DEPLOY_DIR/scripts-common/stardeploy.sh
rm $DEPLOY_DIR/scripts-common/starjobs.sh
rm $DEPLOY_DIR/scripts-common/updateSQL
cp -R sql $DEPLOY_DIR/sql
mv $DEPLOY_DIR/sql/new-install/MinimalDataDistribute.sql $DEPLOY_DIR/sql/new-install/MinimalData.sql
cp -R starexec_res $DEPLOY_DIR/starexec_res
cp -R "upload-test" "$DEPLOY_DIR/upload-test"
cp -R WebContent $DEPLOY_DIR/WebContent
cp build.xml $DEPLOY_DIR/build.xml
cp context.template $DEPLOY_DIR/context.template
cp local.properties $DEPLOY_DIR/local.properties
cp newInstallWithTestData.sh $DEPLOY_DIR/newInstallWithTestData.sh


zip -r -q $DEPLOY_DIR $ZIP_DIR
rm -R $DEPLOY_DIR

