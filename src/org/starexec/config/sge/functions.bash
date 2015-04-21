#!/bin/bash

# /////////////////////////////////////////////
# NAME:        
# StarExec Function Script
#
# AUTHOR:      
# Tyler Jensen
#
# MODIFIED:    
# 2/26/2012
#
# DESCRIPTION:
# This is a script containing common functions used between
# the prolog, epilog and jobscript
#
# /////////////////////////////////////////////

# Include the predefined status codes and functions
. /home/starexec/sge_scripts/status_codes.bash

#################################################################################
# base64 decode some names which could otherwise have nasty characters in them
#################################################################################

# will do a base 64 decode on all solver_names, all solver_paths, and the bench path
function decodePathArrays {
	log "decoding all base 64 encoded strings"
	# create a temporary file in /tmp using the template starexec_base64.XXXXXXXX
	
	TMP=`mktemp --tmpdir=/tmp starexec_base64.XXXXXXXX`
	
	TEMP_ARRAY_INDEX=0
	
	#decode every solver name, solver path, and benchmark suffix in the arrays
	while [ $TEMP_ARRAY_INDEX -lt $NUM_STAGES ]
	do
		echo ${SOLVER_NAMES[$TEMP_ARRAY_INDEX]} > $TMP
		SOLVER_NAMES[$TEMP_ARRAY_INDEX]=`base64 -d $TMP`
		
		echo ${SOLVER_PATHS[$TEMP_ARRAY_INDEX]} > $TMP
		SOLVER_PATHS[$TEMP_ARRAY_INDEX]=`base64 -d $TMP`
		
		echo ${BENCH_SUFFIXES[$TEMP_ARRAY_INDEX]} > $TMP
		BENCH_SUFFIXES[$TEMP_ARRAY_INDEX]=`base64 -d $TMP`
		
		TEMP_ARRAY_INDEX=$(($TEMP_ARRAY_INDEX+1))
	done
	
	TEMP_ARRAY_INDEX=0
	while [ $TEMP_ARRAY_INDEX -lt $NUM_BENCH_INPUTS ]
	do
	
		echo ${BENCH_INPUT_PATHS[$TEMP_ARRAY_INDEX]} > $TMP
		BENCH_INPUT_PATHS[$TEMP_ARRAY_INDEX]=`base64 -d $TMP`
		log "decoded the benchmark input ${BENCH_INPUT_PATHS[$TEMP_ARRAY_INDEX]}"
		TEMP_ARRAY_INDEX=$(($TEMP_ARRAY_INDEX+1))
	done
	
	
	rm $TMP
}


function decodeBenchmarkName {
	
	TMP=`mktemp --tmpdir=/tmp starexec_base64.XXXXXXXX`
	

	echo $BENCH_PATH > $TMP
	BENCH_PATH=`base64 -d $TMP`
	rm $TMP
}
#need to make sure benchmark name is decoded in every file

decodeBenchmarkName 

#################################################################################

# DB username and password for status reporting
DB_USER=star_report
DB_PASS=5t4rr3p0rt2012

#lock files that indicate a particular sandbox is in use
SANDBOX_LOCK_DIR='/export/starexec/sandboxlock.lock'
SANDBOX2_LOCK_DIR='/export/starexec/sandbox2lock.lock'

#files that indicate that lock files are currently being modified
SANDBOX_LOCK_USED='/export/starexec/sandboxlock.active'
SANDBOX2_LOCK_USED='/export/starexec/sandbox2lock.active'

# Path to local workspace for each node in cluster.


# Path to Olivier Roussel's runSolver
RUNSOLVER_PATH="/home/starexec/Solvers/runsolver"



# Path to the job input directory
JOB_IN_DIR="$SHARED_DIR/jobin"

# Path to the job output directory
JOB_OUT_DIR="$SHARED_DIR/joboutput"




######################################################################



#initializes all workspace variables based on the value of the SANDBOX variable, which should already be set
# either by calling initSandbox or findSandbox
function initWorkspaceVariables {
	if [ $SANDBOX -eq 1 ]
	then
	WORKING_DIR='/export/starexec/sandbox'
	else
	WORKING_DIR='/export/starexec/sandbox2'
	fi
	
	# Path to where the solver will be copied
	LOCAL_SOLVER_DIR="$WORKING_DIR/solver"
	
	# Path to where the benchmark will be copied
	LOCAL_BENCH_DIR="$WORKING_DIR/benchmark"
	
	BENCH_INPUT_DIR="$WORKING_DIR/benchinputs"
	
	# The benchmark's name
	BENCH_NAME="${BENCH_PATH##*/}"
	
	BENCH_FILE_EXTENSION="${BENCH_PATH##*.}"
	
	# The path to the benchmark on the execution host 
	LOCAL_BENCH_PATH="$LOCAL_BENCH_DIR/theBenchmark.$BENCH_FILE_EXTENSION"
	
	# Path to where the solver will be copied
	LOCAL_SOLVER_DIR="$WORKING_DIR/solver"
	
	# Path to where the pre-processor will be copied
	LOCAL_PREPROCESSOR_DIR="$WORKING_DIR/preprocessor"
	
	# The path to the bin directory of the solver on the execution host
	LOCAL_RUNSOLVER_PATH="$LOCAL_SOLVER_DIR/bin/runsolver"
	
	OUT_DIR="$WORKING_DIR/output"
	
	# The path to the benchmark on the execution host
	PROCESSED_BENCH_PATH="$OUT_DIR/procBenchmark"
	
	SAVED_OUTPUT_DIR="$WORKING_DIR/savedoutput"
	
	
	
}
#checks to see whether the first argument is a valid integer
function isInteger {
	log "isInteger called on $1"
	re='^[0-9]+$'
	if ! [[ $1 =~ $re ]] ; then
   		return 1
	fi
	return 0
}
# checks to see whether the pair with the given pair SGE ID is actually running using qstat
function isPairRunning {
	log "isPairRunning called on pair id = $1" 
	
	
	if ! isInteger $1 ; then
		log "$1 is not a valid integer, so no pair is running"
	
		return 1
	fi
	HOST=${HOSTNAME:0:4}
	
	output=`ls /cluster/sge-6.2u5/default/spool/$HOST/active_jobs/`
	log "$output"
	
	#be conservative and say that the pair is running if we fail to check properly
	if [[ $output == *cannot* ]]
	then
		log "could not carry out ls command-- assuming pair is still running"
		return 0
	fi
	if [[ $output == *$1* ]]
	then
		#the active jobs directory still contains the job, so it is still running
		return 0
  	fi
  	#otherwise, the job is not still running
	return 1
}

#first argument is the sandbox (1 or 2) and second argument is the pair ID
function trySandbox {
	
	if [ $1 -eq 1 ] ; then
			LOCK_DIR="$SANDBOX_LOCK_DIR"
			LOCK_USED="$SANDBOX_LOCK_USED"
		else
			LOCK_DIR="$SANDBOX2_LOCK_DIR"
			LOCK_USED="$SANDBOX2_LOCK_USED"
		fi
	#force script to wait until it can get the outer lock file to do the block in parens
	#timeout is 4 seconds-- we give up if we aren't able to get the lock in that amount of time
	
	if (
	flock -x -w 4 200 || return 1
		#we have exclusive rights to work on the lock for this sandbox within this block
		
		log "got the right to use the lock for sandbox $1"
		#check to see if we can make the lock directory-- if so, we can run in sandbox 
		if mkdir "$LOCK_DIR" ; then
			log "able to get sandbox $1!"
			# make a file that is named with the given ID so we know which pair should be running here
			touch "$LOCK_DIR/$2"
			
			log "putting this job into sandbox $1 $2"
			return 0
		fi
		#if we couldn't get the sandbox directory, there are 2 possibilites. Either it is occupied,
		#or a previous job did not clean up the lock correctly. To check, we see if the pair given
		#in the directory is still running
			
		pairID=`ls "$LOCK_DIR"`
		log "found the pairID = $pairID"
		
		
		if ! isPairRunning $pairID ; then
			#this means the sandbox is NOT actually in use, and that the old pair just did not clean up
			log "found that the pair is not running in sandbox $1"
			safeRmLock "$LOCK_DIR"
			
			#try again to get the sandbox1 directory-- we still may fail if another pair is doing this at the same time
			if mkdir "$LOCK_DIR" ; then
				#we got the lock, so take this sandbox
				touch "$LOCK_DIR/$2"
				# if we successfully made the directory
				
				log "putting this job into sandbox $1 $2"
				
				return 0
			fi
		else
			log "found that pair $pairID is running in sandbox $1"
		fi
		#could not get the sandbox
		return 1
	
	
	#End of Flock command. We return from trySandbox whatever flock returns
	)200>"$LOCK_USED" ; then
		return 0
	else
		return 1
	fi
	
}


#figures out which sandbox the given job pair should run in. First argument is a job pair ID
function initSandbox {
	#try to get sandbox1 first
	if trySandbox 1 $1; then
		SANDBOX=1
		initWorkspaceVariables
		return 0
	fi
	
	#couldn't get sandbox 1, so try sandbox2 next
	if trySandbox 2 $1 ; then
		SANDBOX=2
		initWorkspaceVariables
		return 0
	fi
	#failed to get either sandbox
	SANDBOX=-1
	return 1
	
	
}

#determines whether we should be running in sandbox 1 or sandbox 2, based on the existence of this pairs' lock file
function findSandbox {
	log "trying to find sandbox for pair ID = $1"
	log "sandbox 1 contents:"
	ls "$SANDBOX_LOCK_DIR"
	
	log "sandbox 2 contents:"
	ls "$SANDBOX2_LOCK_DIR"
	
	if [ -e "$SANDBOX_LOCK_DIR/$1" ]
	then
		log "found that the sandbox is 1 for job $1"
		SANDBOX=1
		initWorkspaceVariables
		return 0
	fi
	if [ -e "$SANDBOX2_LOCK_DIR/$1" ] 
	then
		log "found that the sandbox is 2 for job $1"
		SANDBOX=2
		initWorkspaceVariables
		return 0
	fi
	
	log "couldn't find a sandbox for pair ID = $1"
	SANDBOX=-1
	
}

function log {
	echo "`date +'%D %r %Z'`: $1"
	return $?
}

function safeRmLock {
	if [ "$1" == "" ] ; then
		log "Unsafe rm all detected for lock"
	else
		log "doing rm all on  $1"
		rm -rf "$1"
	fi
}

# call "safeRm description dirname" to do an "rm -r dirname/*" except if
# dirname is the empty string, in which case an error message is printed.
function safeRm {
  if [ "$2" == "" ] ; then 
    log "Unsafe rm all detected for $1"
  else
    log "Doing rm all on $2 ($1)"
    rm -rf "$2"
    mkdir "$2"
    chmod gu+rwx "$2"
  fi
}

#cleans up files to prepare for the next stage of the job
function cleanForNextStage {
		# Clear the output directory	
	safeRm output-directory "$OUT_DIR"

	# Clear the local solver directory	
	safeRm local-solver-directory "$LOCAL_SOLVER_DIR"

	# Clear the local benchmark, as it will be replaced by the output of this stage
	
	rm "$LOCAL_BENCH_PATH"

}

#takes in 1 argument-- 0 if we are done with the job and 1 otherwise. Used to decide whether to clean up scripts and locks
function cleanWorkspace {
	log "cleaning execution host workspace..."
	mkdir -p $WORKING_DIR
	
        chmod 770 $WORKING_DIR
        chmod g+s $WORKING_DIR
	# change ownership and permissions to make sure we can clean everything up
	log "WORKING_DIR is $WORKING_DIR"
	
	sudo chown -R `whoami` $WORKING_DIR 
	chmod -R gu+rxw $WORKING_DIR

    

	# Clear the output directory	
	safeRm output-directory "$OUT_DIR"
	
	# Clear the local solver directory	
	safeRm local-solver-directory "$LOCAL_SOLVER_DIR"

	safeRm bench-inputs "$BENCH_INPUT_DIR"

	# Clear the local benchmark directory	
	safeRm local-benchmark-directory "$LOCAL_BENCH_DIR"
	
	safeRm saved-output-dir "$SAVED_OUTPUT_DIR"
	
	
	
	
	#only delete the job script / lock files if we are in the epilog
	log "about to check whether to delete lock files given $1"
	if [ $1 -eq 0 ] ; then
		log "cleaning up scripts and lock files"
		rm -f "$SCRIPT_PATH"
		rm -f "$JOB_IN_DIR/depend_$PAIR_ID.txt"
		if [ $SANDBOX -eq 1 ] 
		then
			safeRmLock "$SANDBOX_LOCK_DIR"
		fi
		if [ $SANDBOX -eq 2 ] 
		then
			safeRmLock "$SANDBOX2_LOCK_DIR"
		fi
	fi
	 
	
	
	log "execution host $HOSTNAME cleaned"
	return $?
}

function sendStageStatus {
	log "sending status for stage number $2"
    if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStageStatus($PAIR_ID,$2, $1)" ; then
       sleep 20
       mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStageStatus($PAIR_ID,$2, $1)"
    fi
	return $?
}

function sendStatusToLaterStages {

	log "sending status for stage numbers greater than $2"
    if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdateLaterStageStatuses($PAIR_ID,$2, $1)" ; then
       sleep 20
       mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdateLaterStageStatuses($PAIR_ID,$2, $1)"
    fi
	return $?

}

function setRunStatsToZeroForLaterStages {

	log "setting all stats to 0 for stages greater than $1"
    if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL SetRunStatsForLaterStagesToZero($PAIR_ID,$1)" ; then
       sleep 20
       mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL SetRunStatsForLaterStagesToZero($PAIR_ID,$1)"
    fi
	return $?

}

function sendStatus {
    if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStatus($PAIR_ID, $1)" ; then
       sleep 20
       mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStatus($PAIR_ID, $1)"
    fi
	log "sent job status $1 to $REPORT_HOST"
	return $?
}

function setStartTime {
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL SetPairStartTime($PAIR_ID)"
	log "set start time for pair id = $PAIR_ID"
	return $?
}

function setEndTime {
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL SetPairEndTime($PAIR_ID)"
	log "set start time for pair id = $PAIR_ID"
	return $?
}

function recordJobPairRun {
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL AddToEventOccurrencesNotRelatedToQueue('job pairs run', 1); CALL AddToEventOccurrencesForJobPairsQueue('job pairs run', 1, $PAIR_ID)"
	return $?
}


function sendNode {
    mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdateNodeId($PAIR_ID, '$1', '$2' )"
	log "sent Node Id $1 to $REPORT_HOST in sandbox $2"
	return $?
}

function limitExceeded {
	log "job error: $1 limit exceeded, job terminated"
	sendStatus $2	
	exit 1
}

# processes the attributes for a pair. Takes a file produced by a post processor as the first argument
# and a stage number as the second argument
# Ben McCune
function processAttributes {

if [ -z $1 ]; then
  log "No argument passed to processAttributes"
  exit 1
fi

a=0
while read line
do a=$(($a+1));
key=${line%=*};
value=${line#*=};
keySize=${#key}
valueSize=${#value}
product=$[keySize*valueSize]
#testing to see if key or value is empty
if (( $product ))
   then
	log "processing attribute $a (pair=$PAIR_ID, job=$JOB_STAR_ID, key='$key', value='$value')"
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL AddJobAttr($PAIR_ID,'$key','$value',$2)"
else
        log "bad post processing - cannot process attribute $a"
fi
done < $1
}


# updates stats for the pair - parameters are var.out ($1) and watcher.out ($2) from runsolver
# Ben McCune
function updateStats {


WALLCLOCK_TIME=`sed -n 's/^WCTIME=\([0-9\.]*\)$/\1/p' $1`
CPU_TIME=`sed -n 's/^CPUTIME=\([0-9\.]*\)$/\1/p' $1`
CPU_USER_TIME=`sed -n 's/^USERTIME=\([0-9\.]*\)$/\1/p' $1`
SYSTEM_TIME=`sed -n 's/^SYSTEMTIME=\([0-9\.]*\)$/\1/p' $1`
MAX_VIRTUAL_MEMORY=`sed -n 's/^MAXVM=\([0-9\.]*\)$/\1/p' $1`

log "the max virtual memory was $MAX_VIRTUAL_MEMORY"
log "temp line: the var file was"
cat $1
log "end varfile"

ROUNDED_WALLCLOCK_TIME=$( printf "%.0f" $WALLCLOCK_TIME )
ROUNDED_CPU_TIME=$( printf "%.0f" $CPU_TIME )

STAREXEC_WALLCLOCK_LIMIT=$(($STAREXEC_WALLCLOCK_LIMIT-$ROUNDED_WALLCLOCK_TIME))
STAREXEC_CPU_LIMIT=$(($STAREXEC_CPU_LIMIT-$ROUNDED_CPU_TIME))


SOLVER_STATUS_CODE=`awk '/Child status/ { print $3 }' $2`

log "the solver exit code was $SOLVER_STATUS_CODE"

MAX_RESIDENT_SET_SIZE=`awk '/maximum resident set size/ { print $5 }' $2`
PAGE_RECLAIMS=`awk '/page reclaims/ { print $3 }' $2`
PAGE_FAULTS=`awk '/page faults/ { print $3 }' $2`
BLOCK_INPUT=`awk '/block input/ { print $4 }' $2`
BLOCK_OUTPUT=`awk '/block output/ { print $4 }' $2`
VOL_CONTEXT_SWITCHES=`awk '/^voluntary context switches/ { print $4 }' $2`
INVOL_CONTEXT_SWITCHES=`awk '/involuntary context switches/ { print $4 }' $2`

# just sanitize these latter to avoid db errors, since fishing things out of the watchfile is more error-prone apparently.
if [[ ! ( "$MAX_RESIDENT_SET_SIZE" =~ ^[0-9\.]+$ ) ]] ; then MAX_RESIDENT_SET_SIZE=0 ; fi
if [[ ! ( "$PAGE_RECLAIMS" =~ ^[0-9\.]+$ ) ]] ; then PAGE_RECLAIMS=0 ; fi
if [[ ! ( "$PAGE_FAULTS" =~ ^[0-9\.]+$ ) ]] ; then PAGE_FAULTS=0 ; fi
if [[ ! ( "$BLOCK_INPUT" =~ ^[0-9\.]+$ ) ]] ; then BLOCK_INPUT=0 ; fi
if [[ ! ( "$BLOCK_OUTPUT" =~ ^[0-9\.]+$ ) ]] ; then BLOCK_OUTPUT=0 ; fi
if [[ ! ( "$VOL_CONTEXT_SWITCHES" =~ ^[0-9\.]+$ ) ]] ; then VOL_CONTEXT_SWITCHES=0 ; fi
if [[ ! ( "$INVOL_CONTEXT_SWITCHES" =~ ^[0-9\.]+$ ) ]] ; then INVOL_CONTEXT_SWITCHES=0 ; fi

EXEC_HOST=`hostname`
log "mysql -u... -p... -h $REPORT_HOST $DB_NAME -e \"CALL UpdatePairRunSolverStats($PAIR_ID, '$EXEC_HOST', $WALLCLOCK_TIME, $CPU_TIME, $CPU_USER_TIME, $SYSTEM_TIME, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $CURRENT_STAGE_NUMBER)\""

if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairRunSolverStats($PAIR_ID, '$EXEC_HOST', $WALLCLOCK_TIME, $CPU_TIME, $CPU_USER_TIME, $SYSTEM_TIME, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $CURRENT_STAGE_NUMBER)" ; then
log "Error copying stats from watchfile into database. Copying varfile to log {"
cat $1
log "} End varfile."
log "Copying watchfile to log {"
cat $2
log "} End watchfile."
fi

#log "job is $JOB_ID"

log "sent job stats to $REPORT_HOST"

#echo host name = $EXEC_HOST;
log "cpu usage = $CPU_TIME"
log "wallclock time = $WALLCLOCK_TIME"
#echo user time = $CPU_USER_TIME;
#echo system_time = $SYSTEM_TIME;
#echo max virt mem = $MAX_VIRTUAL_MEMORY;
#echo max res set size = $MAX_RESIDENT_SET_SIZE;
#echo page reclaims = $PAGE_RECLAIMS;
#echo page faults = $PAGE_FAULTS;
#echo block input = $BLOCK_INPUT;
#echo block output = $BLOCK_OUTPUT;
#echo VOL Context switches = $VOL_CONTEXT_SWITCHES;
#echo invol context switches = $INVOL_CONTEXT_SWITCHES;

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

# takes in a stage number as an argument so we know where to put the output
function copyOutput {
	log "creating storage directory on master host"

	createDir "$PAIR_OUTPUT_DIRECTORY"
	createDir "$SAVED_OUTPUT_DIR"
	
	if [ $NUM_STAGES -eq 1 ] 
	then
		PAIR_OUTPUT_PATH="$PAIR_OUTPUT_DIRECTORY/$PAIR_ID.txt"
		
	else
		PAIR_OUTPUT_PATH="$PAIR_OUTPUT_DIRECTORY/$1.txt"
		
	
	fi
	
	SAVED_PAIR_OUTPUT_PATH="$SAVED_OUTPUT_DIR/$1"
	
	log "the output path is $PAIR_OUTPUT_PATH"
	cp "$OUT_DIR"/stdout.txt "$PAIR_OUTPUT_PATH"
	cp "$OUT_DIR"/stdout.txt "$SAVED_PAIR_OUTPUT_PATH"
	
	log "job output copy complete - now sending stats"
	updateStats $VARFILE $WATCHFILE
	if [ "$POST_PROCESSOR_PATH" != "" ]; then
		log "getting postprocessor"
		mkdir $OUT_DIR/postProcessor
		cp -r "$POST_PROCESSOR_PATH"/* $OUT_DIR/postProcessor
		chmod -R gu+rwx $OUT_DIR/postProcessor
		cd "$OUT_DIR"/postProcessor
		log "executing post processor"
		./process $OUT_DIR/stdout.txt $LOCAL_BENCH_PATH > "$OUT_DIR"/attributes.txt
		log "processing attributes"
		#cat $OUT_DIR/attributes.txt
		processAttributes $OUT_DIR/attributes.txt $1
	fi

	return $?	
}

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




#will see if a solver is cached and change the current SOLVER_PATH to the cache if so
function checkCache {
	if [ -d "$SOLVER_CACHE_PATH" ]; then
		if [ -d "$SOLVER_CACHE_PATH/finished.lock" ]; then
			log "solver exists in cache at $SOLVER_CACHE_PATH"
  			SOLVER_PATH=$SOLVER_CACHE_PATH	
  			SOLVER_CACHED=1
		fi
	fi	
}

function copyBenchmarkDependencies {

	
	
	if [ "$PRIMARY_PREPROCESSOR_PATH" != "" ]; then
		mkdir $OUT_DIR/preProcessor
		cp -r "$PRIMARY_PREPROCESSOR_PATH"/* $OUT_DIR/preProcessor
		chmod -R gu+rwx $OUT_DIR/preProcessor
		cd "$OUT_DIR"/preProcessor	
	fi
	
	
	log "copying benchmark dependencies to execution host..."
	for (( i = 0 ; i < ${#BENCH_DEPENDS_ARRAY[@]} ; i++ ))
	do
		log "Axiom location = '${BENCH_DEPENDS_ARRAY[$i]}'"
		NEW_D=$(dirname "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}")
		mkdir -p $NEW_D
		if [ "$PRIMARY_PREPROCESSOR_PATH" != "" ]; then
			log "copying benchmark ${BENCH_DEPENDS_ARRAY[$i]} to $LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]} on execution host..."

			"./process" "${BENCH_DEPENDS_ARRAY[$i]}" $RAND_SEED > "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}"
		else
			log "copying benchmark ${BENCH_DEPENDS_ARRAY[$i]} to $LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]} on execution host..."
			
			cp "${BENCH_DEPENDS_ARRAY[$i]}" "$LOCAL_BENCH_DIR/${LOCAL_DEPENDS_ARRAY[$i]}"
		fi
		
		
	done
	
	BENCH_INPUT_INDEX=0
	mkdir -p $BENCH_INPUT_DIR
	
	while [ $BENCH_INPUT_INDEX -lt $(($NUM_BENCH_INPUTS)) ]
	do
		CURRENT_BENCH_INPUT_PATH=${BENCH_INPUT_PATHS[$BENCH_INPUT_INDEX]}
		cp "$CURRENT_BENCH_INPUT_PATH" "$BENCH_INPUT_DIR/$(($BENCH_INPUT_INDEX+1))"
		BENCH_INPUT_INDEX=$(($BENCH_INPUT_INDEX+1))
	done

	log "benchmark dependencies copy complete"

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

	log "copying benchmark $BENCH_PATH to $LOCAL_BENCH_PATH on execution host..."
	cp "$BENCH_PATH" "$LOCAL_BENCH_PATH"
	log "benchmark copy complete"
	
	#doing benchmark preprocessing here if the pre_processor actually exists
	if [ "$PRE_PROCESSOR_PATH" != "" ]; then
		mkdir $OUT_DIR/preProcessor
		cp -r "$PRE_PROCESSOR_PATH"/* $OUT_DIR/preProcessor
		chmod -R gu+rwx $OUT_DIR/preProcessor
		cd "$OUT_DIR"/preProcessor
		log "executing pre processor"
		log "random seed = "$RAND_SEED
		
		./process "$LOCAL_BENCH_PATH" $RAND_SEED > "$PROCESSED_BENCH_PATH"
		#use the processed benchmark in subsequent steps
		rm "$LOCAL_BENCH_PATH"
		mv "$PROCESSED_BENCH_PATH" "$LOCAL_BENCH_PATH"		
	fi
	
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
	if [ "$PRE_PROCESSOR_PATH" != "" ]; then
		mkdir $OUT_DIR/preProcessor
		cp -r "$PRE_PROCESSOR_PATH"/* $OUT_DIR/preProcessor
		chmod -R gu+rwx $OUT_DIR/preProcessor
		cd "$OUT_DIR"/preProcessor
		log "executing pre processor"
		log "random seed = "$RAND_SEED
		
		./process "$LOCAL_BENCH_PATH" $RAND_SEED > "$PROCESSED_BENCH_PATH"
		#use the processed benchmark in subsequent steps
		rm "$LOCAL_BENCH_PATH"
		mv "$PROCESSED_BENCH_PATH" "$LOCAL_BENCH_PATH"		
	fi
	
	
	
	return $?	
}

# Saves the current output 
function saveOutputAsBenchmark {
	log "saving output as benchmark for stage $CURRENT_STAGE_NUMBER" 
	CURRENT_OUTPUT_FILE=$SAVED_OUTPUT_DIR/$CURRENT_STAGE_NUMBER
	BENCH_NAME_ADDON="stage-"
	
	# if no suffix is give, we just use the suffix of the benchmark
	if [ "$CURRENT_BENCH_SUFFIX" == "" ] ; then
		CURRENT_BENCH_SUFFIX=$([[ "$CURRENT_BENCH_SUFFIX" = *.* ]] && echo ".${CURRENT_BENCH_SUFFIX##*.}" || echo '')
	fi
	
	CURRENT_BENCH_NAME=${BENCH_NAME%%.*}$BENCH_NAME_ADDON$CURRENT_STAGE_NUMBER
	MAX_BENCH_NAME_LENGTH=$(($BENCH_NAME_LENGTH_LIMIT-${#CURRENT_BENCH_SUFFIX}))
	CURRENT_BENCH_NAME=${CURRENT_BENCH_NAME:0:$MAX_BENCH_NAME_LENGTH}
	CURRENT_BENCH_NAME="$CURRENT_BENCH_NAME$CURRENT_BENCH_SUFFIX"
	
	
	CURRENT_BENCH_PATH=$BENCH_SAVE_DIR/$SPACE_PATH
	
	FILE_SIZE_IN_BYTES=`wc -c < $CURRENT_OUTPUT_FILE`
	
	
	
	createDir $CURRENT_BENCH_PATH
	
	CURRENT_BENCH_PATH=$CURRENT_BENCH_PATH/$CURRENT_BENCH_NAME

	if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL AddAndAssociateBenchmark('$CURRENT_BENCH_NAME','$CURRENT_BENCH_PATH',false,$USER_ID,1,$FILE_SIZE_IN_BYTES,$SPACE_ID,@id)" ; then
		log "error saving output as benchmark-- benchmark was not created"
	else
		cp $CURRENT_OUTPUT_FILE "$CURRENT_BENCH_PATH"
		
	fi
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


