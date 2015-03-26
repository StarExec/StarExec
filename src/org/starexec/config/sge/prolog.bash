#!/bin/bash

# /////////////////////////////////////////////
# NAME:        
# StarExec Global Prolog Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 11/08/2013
#
# DESCRIPTION:
# This script runs BEFORE the user's job and
# is used to set up the node's environment by
# copying files locally to the node from the NFS.
# There are many environmental variables used here
# That are defined by SGE and by the jobscript in
# the starexec project.
#
# NOTE: This file is deployed to use when the project
# is built with the production build scripts
#
# /////////////////////////////////////////////

# Include the predefined status codes and functions
. /home/starexec/sge_scripts/functions.bash


# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////

whoami
echo "STAREXEC Job Log"
echo "(C) `date +%Y` The University of Iowa"
echo "====================================="
echo "date: `date`"
echo "user: #$USER_ID"
echo "sge job: #$JOB_ID"
echo "pair id: $PAIR_ID"
echo "execution host: $HOSTNAME"
echo ""

sendStatus $STATUS_PREPARING

initSandbox "$JOB_ID"


if ! isInteger $SANDBOX ; then
	sendStatus $ERROR_RUNSCRIPT
	log "unable to secure any sandbox for this job!"
	exit 0
fi

if [ $SANDBOX -eq -1 ] 
then
	sendStatus $ERROR_RUNSCRIPT
	log "unable to secure any sandbox for this job!"
	exit 0
fi


sendNode "$HOSTNAME" "$SANDBOX"
cleanWorkspace 1
sendStatus $STATUS_RUNNING
setStartTime


log "prolog ends"

exit 0
