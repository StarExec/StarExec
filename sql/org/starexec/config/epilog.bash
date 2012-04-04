#!/bin/bash

# /////////////////////////////////////////////
# NAME:        
# StarExec Epilog Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 2/15/2012
#
# DESCRIPTION:
# This is the script that gets executed when
# a job is complete. It's job is to clean up
# the environment and to do any after-job related
# tasks
#
# NOTE: This file is included in the STAREXEC project
# for version control only. If changes are made, please
# copy the file to /home/starexec/sge_scripts
#
# /////////////////////////////////////////////

# Include the predefined status codes
. /home/starexec/sge_scripts/status_codes.bash

# Path to local workspace for each node in cluster.
WORKING_DIR='/export/starexec/workspace'

# Path to where the solver will be copied
LOCAL_SOLVER_DIR=$WORKING_DIR/solver

# Path to where the benchmark will be copied
LOCAL_BENCH_DIR=$WORKING_DIR/benchmark

# Path to shared directory for each node in cluster.
SHARED_DIR='/home/starexec'

# Path to the job input directory
JOB_IN_DIR=$SHARED_DIR/jobin

# Path to the job output directory
JOB_OUT_DIR=$SHARED_DIR/jobout

# DB username and password for status reporting
DB_USER=star_report
DB_PASS=5t4rr3p0rt2012

# /////////////////////////////////////////////
# Functions
# /////////////////////////////////////////////

function log {
	echo "`date +'%D %r %Z'`: $1"
	return $?
}

function sendStatus {
        mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStatus($PAIR_ID, $1)"
	log "sent job status $1 to $REPORT_HOST"
	return $?
}

function cleanWorkspace {
	log "cleaning execution host workspace..."

	# Remove all existing files in the workspace
	rm -rf $WORKING_DIR/*

	# Remove original jobscript
	rm -f $JOB_IN_DIR/job_$PAIR_ID.bash

	log "execution host $HOSTNAME cleaned"
	return $?
}

function copyOutput {
	log "creating storage directory on master host"

	# Copy this job's output to /JOBOUT/USERID/STAREXEC JOB ID/PAIR ID	
	UNIQUE_OUT_DIR="$JOB_OUT_DIR/$USER_ID/$JOB_STAR_ID/$PAIR_ID"
	createDir "$UNIQUE_OUT_DIR"

	log "copying output to master host"

	# Copy output from local host output to master host output storage
	cp -r $STAREXEC_OUT_DIR/* $UNIQUE_OUT_DIR

	log "job output copy complete"

	return $?	
}

function createDir {
	mkdir -p $1

	# Check the directory actually does exist
	if [ ! -d $1 ]; then
		mkdir $1
		log "cannot create directory '$1' this may cause problems in result reporting"
	fi

	return $?
}

# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////
log "execution on $HOSTNAME complete"
sendStatus $STATUS_FINISHING
copyOutput
cleanWorkspace
sendStatus $STATUS_WAIT_STATS
log "STAREXEC job #$JOB_ID complete. Statistics for this job will be sent to starexec in the near future."
