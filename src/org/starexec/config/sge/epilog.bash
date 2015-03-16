#!/bin/bash
whoami
# /////////////////////////////////////////////
# NAME:        
# StarExec Epilog Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 09/06/2013
#
# DESCRIPTION:
# This is the script that gets executed when
# a job is complete. It's job is to clean up
# the environment and to do any after-job related
# tasks
#
# NOTE: This file is deployed to use when the project
# is built with the production build scripts
#
# /////////////////////////////////////////////

# Include the predefined status codes and functions
. /home/starexec/sge_scripts/functions.bash

# Path to local workspace for each node in cluster.
findSandbox $JOB_ID

if [ $SANDBOX -eq -1 ] 
then
	log "epilog was not completed because no sandbox could be found"
	sendStatus $ERROR_RUNSCRIPT
	setEndTime
	exit 0
fi

# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////

setEndTime

cleanWorkspace 0

if [ "$JOB_ERROR" = "" ]; then
	sendStatus $STATUS_COMPLETE
	sendStageStatus $STATUS_COMPLETE ${STAGE_NUMBERS[$STAGE_INDEX]}
	
	log "STAREXEC job #$JOB_ID completed successfully"	
else
	log "STAREXEC job #$JOB_ID exited with errors"
fi

exit 0
