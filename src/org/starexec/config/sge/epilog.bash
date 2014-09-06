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
findSandbox $PAIR_ID

if [ $SANDBOX -eq -1 ] 
then
	log "epilog was not completed because no sandbox could be found"
	exit 1
fi

# runsolver dumps a lot of information to the WATCHFILE, and summary of times and such to VARFILE
WATCHFILE="$STAREXEC_OUT_DIR"/watcher.out
VARFILE="$STAREXEC_OUT_DIR"/var.out

# /////////////////////////////////////////////
# Functions
# /////////////////////////////////////////////


# TODO: We should just be passing in the output directory from javaS
function copyOutput {
	log "creating storage directory on master host"

	createDir "$PAIR_OUTPUT_DIRECTORY"

	cp "$STAREXEC_OUT_DIR"/stdout.txt "$PAIR_OUTPUT_PATH"
	log "job output copy complete - now sending stats"
	updateStats $VARFILE $WATCHFILE
	if [ "$POST_PROCESSOR_PATH" != "null" ]; then
		log "getting postprocessor"
		mkdir $STAREXEC_OUT_DIR/postProcessor
		cp -r "$POST_PROCESSOR_PATH"/* $STAREXEC_OUT_DIR/postProcessor
		chmod -R gu+rwx $STAREXEC_OUT_DIR/postProcessor
		cd "$STAREXEC_OUT_DIR"/postProcessor
		log "executing post processor"
		./process $STAREXEC_OUT_DIR/stdout.txt $LOCAL_BENCH_PATH > "$STAREXEC_OUT_DIR"/attributes.txt
		log "processing attributes"
		#cat $STAREXEC_OUT_DIR/attributes.txt
		processAttributes $STAREXEC_OUT_DIR/attributes.txt
	fi

	return $?	
}

function createDir {
	mkdir -p "$1"

	# Check the directory actually does exist
	if [ ! -d "$1" ]; then
		mkdir "$1"
		log "job error: cannot create directory '$1' this jobs output cannot be saved"
	fi

	return $?
}

# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////

# check for which cores we are on
log "epilog checking for information on cores, from runsolver's watch file:"
grep 'cores:' $WATCHFILE


copyOutput


# cleared below if no error
JOB_ERROR="1";


if ( grep 'job error:' "$SGE_STDOUT_PATH" ) then
  true 
elif ( grep 'wall clock time exceeded' $WATCHFILE ) then
  log "epilog detects wall clock time exceeded"
  sendStatus $EXCEED_RUNTIME
elif ( grep 'CPU time exceeded' $WATCHFILE ) then
  log "epilog detects cpu time exceeded"
  sendStatus $EXCEED_CPU
elif ( grep 'VSize exceeded' $WATCHFILE ) then
  log "epilog detects max virtual memory exceeded"
  sendStatus $EXCEED_MEM
else
  JOB_ERROR="";
  log "execution on $HOSTNAME complete"
  sendStatus $STATUS_FINISHING
fi



cleanWorkspace

if [ "$JOB_ERROR" = "" ]; then
	sendStatus $STATUS_COMPLETE
	log "STAREXEC job #$JOB_ID completed successfully"	
else
	log "STAREXEC job #$JOB_ID exited with errors"
fi

exit 0
