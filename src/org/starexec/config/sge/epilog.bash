#!/bin/bash

# /////////////////////////////////////////////
# NAME:        
# StarExec Epilog Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 03/07/2012
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
WORKING_DIR='/export/starexec/sandbox'

# Path to where the solver will be copied
LOCAL_SOLVER_DIR="$WORKING_DIR/solver"

# Path to where the benchmark will be copied
LOCAL_BENCH_DIR="$WORKING_DIR/benchmark"

# Path to shared directory for each node in cluster.
SHARED_DIR='/home/starexec'

# Path to the job input directory
JOB_IN_DIR="$SHARED_DIR/jobin"

# Path to the job output directory
JOB_OUT_DIR="$SHARED_DIR/jobout"



# /////////////////////////////////////////////
# Functions
# /////////////////////////////////////////////

function cleanWorkspace {
	log "cleaning execution host workspace..."

	# Remove all existing files in the workspace
	rm -f "$WORKING_DIR"/*.*
	rm -rf "$LOCAL_SOLVER_DIR"/*
	rm -rf "$LOCAL_BENCH_DIR"/*
	rm -rf "$STAREXEC_OUT_DIR"/*

	# Remove original jobscript
	rm -f "$JOB_IN_DIR/job_$PAIR_ID.bash"

	log "execution host $HOSTNAME cleaned"
	return $?
}

function copyOutput {
	log "creating storage directory on master host"

	# Copy this job's output to /JOBOUT/USERID/STAREXEC JOB ID/PAIR ID	
	UNIQUE_OUT_DIR="$JOB_OUT_DIR/$USER_ID/$JOB_STAR_ID/$PAIR_ID"
	createDir "$UNIQUE_OUT_DIR"
	
	log "second output dir"
	RZ_OUT_DIR="$JOB_OUT_DIR/$USER_ID/$JOB_STAR_ID/$SOLVER_NAME"
	log $RZ_OUT_DIR	
        APPEND=_$CONFIG_NAME
        log $APPEND
        RZ_OUT_DIR="$RZ_OUT_DIR$APPEND"
	log $RZ_OUT_DIR
	createDir "$RZ_OUT_DIR"

	log "copying output to master host"
	


	# Copy output from local host output to master host output storage
	cp -r "$STAREXEC_OUT_DIR"/* "$UNIQUE_OUT_DIR"
	
	BENCH_NAME="${BENCH_PATH##*/}"
	log "Bench Name = $BENCH_NAME"
	log $RZ_OUT_DIR/$BENCH_NAME
	cp  "$STAREXEC_OUT_DIR"/stdout.txt "$RZ_OUT_DIR/$BENCH_NAME"

	log "job output copy complete"

	return $?	
}

function createDir {
	mkdir -p $1

	# Check the directory actually does exist
	if [ ! -d $1 ]; then
		mkdir $1
		log "job error: cannot create directory '$1' this jobs output cannot be saved"
	fi

	return $?
}

# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////

# A trap was caught if the string terminated is contained in the log
JOB_ERROR=`grep 'job error:' $SGE_STDOUT_PATH`

# If there was no error...
if [ "$JOB_ERROR" = "" ]; then
	log "execution on $HOSTNAME complete"
	sendStatus $STATUS_FINISHING
	copyOutput
else
	log "execution on $HOSTNAME interrupted"
fi

cleanWorkspace

if [ "$JOB_ERROR" = "" ]; then
	sendStatus $STATUS_WAIT_RESULTS
	log "STAREXEC job #$JOB_ID completed successfully"
else
	log "STAREXEC job #$JOB_ID exited with errors"
fi

exit 0
