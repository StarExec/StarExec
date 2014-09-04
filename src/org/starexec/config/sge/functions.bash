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
TMP=`mktemp`

echo $SOLVER_NAME > $TMP
SOLVER_NAME=`base64 -d $TMP`

echo $SOLVER_PATH > $TMP
SOLVER_PATH=`base64 -d $TMP`

echo $BENCH_PATH > $TMP
BENCH_PATH=`base64 -d $TMP`

rm $TMP
#################################################################################

# DB username and password for status reporting
DB_USER=star_report
DB_PASS=5t4rr3p0rt2012

SANDBOX_LOCK_DIR='/export/starexec/sandboxlock.lock'
SANDBOX2_LOCK_DIR='/export/starexec/sandbox2lock.lock'

# Path to local workspace for each node in cluster.



#path to where cached solvers are stored
SOLVER_CACHE_PATH="/export/starexec/solvercache/$SOLVER_TIMESTAMP/$SOLVER_ID"

#whether the solver was found in the cache
SOLVER_CACHED=0

# Path to Olivier Roussel's runSolver
RUNSOLVER_PATH="/home/starexec/Solvers/runsolver"



# Path to the job input directory
JOB_IN_DIR="$SHARED_DIR/jobin"

# Path to the job output directory
JOB_OUT_DIR="$SHARED_DIR/joboutput"

# The path to the benchmark on the execution host
PROCESSED_BENCH_PATH="$STAREXEC_OUT_DIR/procBenchmark"



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
	
	# The benchmark's name
	BENCH_NAME="${BENCH_PATH##*/}"
	
	BENCH_FILE_EXTENSION="${BENCH_PATH##*.}"
	
	# The path to the benchmark on the execution host 
	LOCAL_BENCH_PATH="$LOCAL_BENCH_DIR/theBenchmark.$BENCH_FILE_EXTENSION"

	# The path to the config run script on the execution host
	LOCAL_CONFIG_PATH="$LOCAL_SOLVER_DIR/bin/starexec_run_$CONFIG_NAME"
	
	# Path to where the solver will be copied
	LOCAL_SOLVER_DIR="$WORKING_DIR/solver"
	
	# Path to where the pre-processor will be copied
	LOCAL_PREPROCESSOR_DIR="$WORKING_DIR/preprocessor"
	
	
	
	# The path to the bin directory of the solver on the execution host
	LOCAL_RUNSOLVER_PATH="$LOCAL_SOLVER_DIR/bin/runsolver"
}

# checks to see whether the pair with the given sge ID is actually running using qstat
function isPairRunning {
	log "isPairRunning called on pair id = $1" 
	if (grep "Following jobs do not exist:" `ssh stardev "export SGE_ROOT=/cluster/sge-6.2u5; /cluster/sge-6.2u5/bin/lx24-amd64/qstat -j $1"` ) then
		return 1
	fi
	return 0
}

#first argument is the sandbox (1 or 2) and second argument is the SGE ID
function trySandbox {
	if [ $1 -eq 1 ] ; then
		LOCK_DIR="$SANDBOX_LOCK_DIR"
	else
		LOCK_DIR="$SANDBOX2_LOCK_DIR"
	fi
	#check to see if we can make the lock directory-- if so, we can run in sandbox 
	if mkdir "$LOCK_DIR" ; then
		# make a file that is named with the given ID so we know which pair should be running here
		touch "$LOCK_DIR/$2"
	
		# if we successfully made the directory
		SANDBOX=$1
		log "putting this job into sandbox $1 $2"
		initWorkspaceVariables
		return 0
	fi
	#if we couldn't get the sandbox directory, there are 2 possibilites. Either it is occupied,
	#or a previous job did not clean up the lock correctly. To check, we see if the pair given
	#in the directory is still running
		
	sgeID=`ls "$LOCK_DIR"`
	log "found the sgeID = $sgeID"
	
	
	if ! isPairRunning $sgeID ; then
		#this means sandbox1 is NOT actually in use, and that the old pair just did not clean up
		log "found that the pair is not running in sandbox1!"
		safeRmLock "$LOCK_DIR"
		
		#try again to get the sandbox1 directory-- we still may fail if another pair is doing this at the same time
		if mkdir "$LOCK_DIR" ; then
			#we got the lock, so take sandbox 1
			touch "$LOCK_DIR/$2"
			# if we successfully made the directory
			SANDBOX=$1
			log "putting this job into sandbox $1 $2"
			initWorkspaceVariables
			return 0
		fi
	else
		log "found that pair $sgeID is running in sandbox1"
	fi
	#could not get the sandbox
	return 1
}


#figures out which sandbox the given job pair should run in. First argument is a job pair SGE ID
function initSandbox {
	#try to get sandbox1 first
	if trySandbox 1 $1; then
		return 0
	fi
	
	#couldn't get sandbox 1, so try sandbox2 next
	if trySandbox 2 ; then
		return 0
	fi
	#failed to get either sandbox
	SANDBOX=-1
	return 1
	
	
}

#determines whether we should be running in sandbox 1 or sandbox 2, based on the value of the 
function findSandbox {
	if [ -e "$SANDBOX_LOCK_DIR/$1" ]
	then
		log "found that the sandbox is 1 for job $1"
		SANDBOX=1
		initWorkspaceVariables
	fi
	if [ -e "$SANDBOX2_LOCK_DIR/$1 ] 
	then
		log "found that the sandbox is 2 for job $1"
		SANDBOX=2
		initWorkspaceVariables
	fi
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

function cleanWorkspace {
	log "cleaning execution host workspace..."

	# change ownership and permissions to make sure we can clean everything up
	log "WORKING_DIR is $WORKING_DIR"
	sudo chown -R `whoami` $WORKING_DIR 
	chmod -R gu+rxw $WORKING_DIR

        ls -l $WORKING_DIR

	# Clear the output directory	
	safeRm output-directory "$STAREXEC_OUT_DIR"

	# Clear the local solver directory	
	safeRm local-solver-directory "$LOCAL_SOLVER_DIR"

	# Clear the local benchmark directory	
	safeRm local-benchmark-directory "$LOCAL_BENCH_DIR"
	
	rm -f "$SCRIPT_PATH"
	
	if [ $SANDBOX -eq 1 ] 
	then
		safeRmLock "$SANDBOX_LOCK_DIR"
	fi
	if [ $SANDBOX -eq 2 ] 
	then
		safeRmLock "$SANDBOX2_LOCK_DIR"
	fi
	
	log "execution host $HOSTNAME cleaned"
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

function sendNode {
    mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdateNodeId($PAIR_ID, '$1' )"
	log "sent Node Id $1 to $REPORT_HOST"
	return $?
}

function limitExceeded {
	log "job error: $1 limit exceeded, job terminated"
	sendStatus $2	
	exit 1
}

# processes the attributes for a pair
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
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL AddJobAttr($PAIR_ID,'$key','$value')"
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
log "mysql -u... -p... -h $REPORT_HOST $DB_NAME -e \"CALL UpdatePairRunSolverStats($PAIR_ID, '$EXEC_HOST', $WALLCLOCK_TIME, $CPU_TIME, $CPU_USER_TIME, $SYSTEM_TIME, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $PAGE_RECLAIMS, $PAGE_FAULTS, $BLOCK_INPUT, $BLOCK_OUTPUT, $VOL_CONTEXT_SWITCHES, $INVOL_CONTEXT_SWITCHES)\""

if ! mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairRunSolverStats($PAIR_ID, '$EXEC_HOST', $WALLCLOCK_TIME, $CPU_TIME, $CPU_USER_TIME, $SYSTEM_TIME, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $PAGE_RECLAIMS, $PAGE_FAULTS, $BLOCK_INPUT, $BLOCK_OUTPUT, $VOL_CONTEXT_SWITCHES, $INVOL_CONTEXT_SWITCHES)" ; then
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
#echo cpu usage = $CPU_TIME;
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
