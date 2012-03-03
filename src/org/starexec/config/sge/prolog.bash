#!/bin/bash

# /////////////////////////////////////////////
# NAME:        
# StarExec Global Prolog Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 3/2/2012
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

# Path to local workspace for each node in cluster.
WORKING_DIR='/export/starexec/workspace'

# Path to where the solver will be copied
LOCAL_SOLVER_DIR=$WORKING_DIR/solver

# Path to where the benchmark will be copied
LOCAL_BENCH_DIR=$WORKING_DIR/benchmarks

# Path to shared directory for each node in cluster.
SHARED_DIR='/home/starexec'

# Path to the job input directory
JOB_IN_DIR=$SHARED_DIR/jobin

# Actual qualified name of the config run script
CONFIG_NAME=run_$CONFIG_NAME

# The benchmark's name
BENCH_NAME=${BENCH_PATH##*/}

# The path to the benchmark on the execution host
LOCAL_BENCH_PATH=$LOCAL_BENCH_DIR/$BENCH_NAME

# The path to the config run script on the execution host
CONFIG_PATH=$LOCAL_SOLVER_DIR/bin/$CONFIG_NAME

# /////////////////////////////////////////////
# Functions
# /////////////////////////////////////////////

function createDir {
	mkdir $1

	if [ ! -d $1 ]; then
		log "job error: cannot create directory '$1'"
		sendStatus $ERROR_ENVIRONMENT		
	fi

	return $?
}

function cleanWorkspace {
	log "cleaning execution host workspace..."

	# Remove all existing files in the workspace
	rm -rf $WORKING_DIR/*

	# Create the output directory
	createDir $STAREXEC_OUT_DIR	

	# Create local solver directory
	createDir $LOCAL_SOLVER_DIR

	# Create local benchmark directory
	createDir $LOCAL_BENCH_DIR

	log "execution host $HOSTNAME cleaned"
	return $?
}

function copyDependencies {
	log "copying solver ($SOLVER_NAME) to execution host..."
	cp -r $SOLVER_PATH/* $LOCAL_SOLVER_DIR
	log "solver copy complete"

	log "copying benchmark (${BENCH_PATH##*/}) to execution host..."
	cp $BENCH_PATH $LOCAL_BENCH_DIR
	log "benchmark copy complete"

	return $?	
}

function verifyWorkspace { 
	# Make sure the configuration exists before we execute it
	if ! [ -x $CONFIG_PATH ]; then
		log "job error: could not locate the configuration script '$CONFIG_NAME' on the execution host"
		sendStatus $ERROR_RUNSCRIPT
	else
		log "execution host solver configuration verified"	
	fi	

	# Make sure the benchmark exists before the job runs
	if ! [ -x $LOCAL_BENCH_PATH ]; then
		echo "job error: could not locate the benchmark '$BENCH_NAME' on the execution host"
		sendStatus $ERROR_BENCHMARK		
	else
		log "execution host benchmark verified"
	fi		

	return $?
}

# /////////////////////////////////////////////
# MAIN
# /////////////////////////////////////////////

echo "STAREXEC Job Log"
echo "(C) `date +%Y` The University of Iowa"
echo "====================================="
echo "date: `date`"
echo "user: #$USER_ID"
echo "sge job: #$JOB_ID"
echo "solver: $SOLVER_NAME"
echo "configuration: $CONFIG_NAME"
echo "benchmark: ${BENCH_PATH##*/}"
echo "execution host: $HOSTNAME"
echo ""

sendStatus $STATUS_PREPARING
cleanWorkspace
copyDependencies
verifyWorkspace

# Determine if there were errors in the job setup
JOB_ERROR=`grep 'job error:' $SGE_STDOUT_PATH`

# If there was no error...
if [ "$JOB_ERROR" = "" ]; then
	log "running $SOLVER_NAME ($CONFIG_NAME) on $BENCH_NAME"
	sendStatus $STATUS_RUNNING
fi

exit 0