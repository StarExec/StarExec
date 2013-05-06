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

# DB username and password for status reporting
DB_USER=star_report
DB_PASS=5t4rr3p0rt2012

#if [ -z $1 ]; then
#  log "No argument passed"
#  exit 1
#fi

function log {
	echo "`date +'%D %r %Z'`: $1"
	return $?
}

function sendStatus {
    mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairStatus($PAIR_ID, $1)"
	log "sent job status $1 to $REPORT_HOST"
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
	log "processing attribute $a"
	mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL AddJobAttr($PAIR_ID,$JOB_STAR_ID,'$key','$value')"
else
        log "bad post processing - cannot process attribute $a"
fi
done < $1
}


# updates stats for the pair - parameter is watcher.out from runsolver
# Ben McCune
function updateStats {

if [ -z $1 ]; then
  log "No argument passed"
  exit 1
fi

WALLCLOCK_TIME=`awk '/Real time \(s\):/ { print $4 }' $1`
CPU_USAGE=`awk '/CPU usage/ { print $4 }' $1`
CPU_USER_TIME=`awk '/CPU user time \(s\):/ { print $5 }' $1`
SYSTEM_TIME=`awk '/CPU system time \(s\):/ { print $5 }' $1`
MAX_VIRTUAL_MEMORY=`awk '/Max. virtual memory/ { print $9 }' $1`
MAX_RESIDENT_SET_SIZE=`awk '/maximum resident set size/ { print $5 }' $1`
PAGE_RECLAIMS=`awk '/page reclaims/ { print $3 }' $1`
PAGE_FAULTS=`awk '/page faults/ { print $3 }' $1`
BLOCK_INPUT=`awk '/block input/ { print $4 }' $1`
BLOCK_OUTPUT=`awk '/block output/ { print $4 }' $1`
VOL_CONTEXT_SWITCHES=`awk '/^voluntary context switches/ { print $4 }' $1`
INVOL_CONTEXT_SWITCHES=`awk '/involuntary context switches/ { print $4 }' $1`

EXEC_HOST=`hostname`

#log "mysql -u\"$DB_USER\" -p\"$DB_PASS\" -h $REPORT_HOST $DB_NAME -e \"CALL UpdatePairRunSolverStats($PAIR_ID, \'$EXEC_HOST\', 
#$CPU_USAGE, $CPU_USER_TIME, $SYSTEM_TIME, 314159, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $PAGE_RECLAIMS, $PAGE_FAULTS, 
#$BLOCK_INPUT, $BLOCK_OUTPUT, $VOL_CONTEXT_SWITCHES, $INVOL_CONTEXT_SWITCHES)\""

mysql -u"$DB_USER" -p"$DB_PASS" -h $REPORT_HOST $DB_NAME -e "CALL UpdatePairRunSolverStats($PAIR_ID, '$EXEC_HOST', $WALLCLOCK_TIME, $CPU_USAGE, $CPU_USER_TIME, $SYSTEM_TIME, $MAX_VIRTUAL_MEMORY, $MAX_RESIDENT_SET_SIZE, $PAGE_RECLAIMS, $PAGE_FAULTS, $BLOCK_INPUT, $BLOCK_OUTPUT, $VOL_CONTEXT_SWITCHES, $INVOL_CONTEXT_SWITCHES)"

#log "job is $JOB_ID"

log "sent job stats to $REPORT_HOST"

#echo host name = $EXEC_HOST;
#echo cpu usage = $CPU_USAGE;
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
