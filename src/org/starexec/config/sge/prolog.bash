
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

# Array of secondary benchmarks starexec paths
declare -a BENCH_DEPENDS_ARRAY

# Array of secondary benchmarks execution host paths
declare -a LOCAL_DEPENDS_ARRAY

# /////////////////////////////////////////////
# Functions
# /////////////////////////////////////////////

#fills arrays from file
function fillDependArrays {
#separator
sep=',,,'

INDEX=0

log "has depends = $HAS_DEPENDS"

if [ $HAS_DEPENDS -eq 1 ]
then
while read line
do
  BENCH_DEPENDS_ARRAY[INDEX]=${line//$sep*};
  LOCAL_DEPENDS_ARRAY[INDEX]=${line//*$sep};
  INDEX=$((INDEX + 1))
done < "$JOB_IN_DIR/depend_$PAIR_ID.txt"
fi

return $?
}

#will see if a solver is cached and change the SOLVER_PATH to the cache if so
function checkCache {
	if [ -d "$SOLVER_CACHE_PATH" ]; then
		if [ -d "$SOLVER_CACHE_PATH/finished.lock" ]; then
			log "solver exists in cache at $SOLVER_CACHE_PATH"
  			SOLVER_PATH=$SOLVER_CACHE_PATH	
  			SOLVER_CACHED=1
		fi
	fi	
}

function copyDependencies {
	log "copying solver:  cp -r $SOLVER_PATH/* $LOCAL_SOLVER_DIR"
	cp -r "$SOLVER_PATH"/* "$LOCAL_SOLVER_DIR"	
	log "solver copy complete"
	if [ $SOLVER_CACHED -eq 0 ]; then
		mkdir -p "$SOLVER_CACHE_PATH"
		if mkdir "$SOLVER_CACHE_PATH/lock.lock" ; then
			if [ ! -d "$SOLVER_CACHE_PATH/finished.lock" ]; then
				#store solver in a cache
				log "storing solver in cache at $SOLVER_CACHE_PATH"
				#if the copy was successful
				if cp -r "$LOCAL_SOLVER_DIR"/* "$SOLVER_CACHE_PATH" ; then
					log "the solver was successfully copied into the cache"
					mkdir "$SOLVER_CACHE_PATH/finished.lock"	
					rm -r "$SOLVER_CACHE_PATH/lock.lock"
				else
					#if we failed to copy the solver, remove the cache entry for the solver
					log "the solver could not be copied into the cache successfully"
					rm -r "$SOLVER_CACHE_PATH"	
				fi
			fi
		fi		
	fi
        log "chmod gu+rwx on the solver directory on the execution host ($LOCAL_SOLVER_DIR)"
        chmod -R gu+rwx $LOCAL_SOLVER_DIR

	log "copying runSolver to execution host..."
	cp "$RUNSOLVER_PATH" "$LOCAL_RUNSOLVER_PATH"
	log "runsolver copy complete"
	ls -l "$LOCAL_RUNSOLVER_PATH"

	log "copying benchmark $BENCH_PATH to $LOCAL_BENCH_PATH on execution host..."
	cp "$BENCH_PATH" "$LOCAL_BENCH_PATH"
	log "benchmark copy complete"
	
	#doing benchmark preprocessing here if the pre_processor actually exists
	if [ "$PRE_PROCESSOR_PATH" != "null" ]; then
		mkdir $STAREXEC_OUT_DIR/preProcessor
		cp -r "$PRE_PROCESSOR_PATH"/* $STAREXEC_OUT_DIR/preProcessor
		chmod -R gu+rwx $STAREXEC_OUT_DIR/preProcessor
		cd "$STAREXEC_OUT_DIR"/preProcessor
		log "executing pre processor"
		log "random seed = "$RAND_SEED
		
		./process "$LOCAL_BENCH_PATH" $RAND_SEED > "$PROCESSED_BENCH_PATH"
		#use the processed benchmark in subsequent steps
		rm "$LOCAL_BENCH_PATH"
		mv "$PROCESSED_BENCH_PATH" "$LOCAL_BENCH_PATH"		
	fi
	
	
	# TODO: Does pre-processing need to be done on dependencies as well?
	log "copying benchmark dependencies to execution host..."
	for (( i = 0 ; i < ${#BENCH_DEPENDS_ARRAY[@]} ; i++ ))
	do
		#log "Axiom location = '${BENCH_DEPENDS_ARRAY[$i]}'"
		NEW_D=$(dirname "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}")
		mkdir -p $NEW_D
		if [ "$PRE_PROCESSOR_PATH" != "null" ]; then
			"./process" "${BENCH_DEPENDS_ARRAY[$i]}" $RAND_SEED > "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}"
		else
			cp "${BENCH_DEPENDS_ARRAY[$i]}" "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}"
		fi
		
		
	done
	log "benchmark dependencies copy complete"
	return $?	
}

#benchmark dependencies not currently verified.
function verifyWorkspace { 
	# Make sure the configuration exists before we execute it
	if ! [ -x "$LOCAL_CONFIG_PATH" ]; then
		log "job error: could not locate the configuration script '$CONFIG_NAME' on the execution host"
		#get rid of the cache, as if we're here then something is probably wrong with it
		rm -r "$SOLVER_CACHE_PATH"
		sendStatus $ERROR_RUNSCRIPT
	else
		log "execution host solver configuration verified"	
	fi	

	# Make sure the benchmark exists before the job runs
	if ! [ -r "$LOCAL_BENCH_PATH" ]; then
                echo "job error: could not locate the readable benchmark '$BENCH_NAME' on the execution host."
                sendStatus $ERROR_BENCHMARK
        else
		log "execution host benchmark verified"
	fi		

	return $?
}
#TODO: syntax check this
function sandboxWorkspace {

	if [[ $WORKING_DIR == *sandbox2* ]] 
	
	then
	log "sandboxing workspace with sandbox2 user"
	sudo chown -R sandbox2 $WORKING_DIR 
	else
		log "sandboxing workspace with sandbox user"
		sudo chown -R sandbox $WORKING_DIR
	fi
	ls -lR "$WORKING_DIR"
	return 0
}

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
echo "solver: $SOLVER_NAME"
echo "solverid: $SOLVER_ID"
echo "timestamp: $SOLVER_TIMESTAMP"
echo "configuration: $CONFIG_NAME"
echo "benchmark: ${BENCH_PATH##*/}"
echo "execution host: $HOSTNAME"
echo ""


initSandbox "$JOB_ID"
#todo: how exactly do we return an error correctly?
if [ $SANDBOX -eq -1 ] 
then
	sendStatus $ERROR_RUNSCRIPT
	log "unable to secure any sandbox for this job!"
	exit 1
fi
sendStatus $STATUS_PREPARING
sendNode "$HOSTNAME"
cleanWorkspace
fillDependArrays
checkCache
copyDependencies
verifyWorkspace
sandboxWorkspace

# Determine if there were errors in the job setup
JOB_ERROR=`grep 'job error:' "$SGE_STDOUT_PATH"`

# If there was no error...
if [ "$JOB_ERROR" = "" ]; then
	sendStatus $STATUS_RUNNING
fi

NODE_MEM=$(vmstat -s | head -1 | sed 's/total memory//')

#then, convert kb to mb
NODE_MEM=$(($NODE_MEM/1024))

log "node memory in megabytes = $NODE_MEM"

#then, set to half the memory
NODE_MEM=$(($NODE_MEM/2))

if [ $MAX_MEM -gt $NODE_MEM ]
then

echo "truncating max memory from requested $MAX_MEM to $NODE_MEM (half max memory on the node)"

export MAX_MEM=$NODE_MEM

fi

log "we will run the job pair with max memory in megabytes = $MAX_MEM"

log "runsolver is $LOCAL_RUNSOLVER_PATH"
log "solver is $LOCAL_CONFIG_PATH"
log "benchmark is $LOCAL_BENCH_PATH"
log "prolog ends"

exit 0
