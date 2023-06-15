#!/bin/bash
#==========================================================================
# Standard SGE job script setup.
#==========================================================================

# The queue to submit to
#$ -q $$QUEUE$$
#OAR -q $$QUEUE$$
#OAR -p queue='$$QUEUE$$'

# Enable resource limit signals
#$ -notify

# Submit under sandbox user
#$ -u $$SANDBOX_USER_ONE$$

# Resource limits
#$ -l s_fsize=$$MAX_WRITE$$G

# Default shell is bash
#$ -S /bin/bash

# Merge stdout and stderr streams
#$ -j y

export readonly BENCHEXEC='BENCHEXEC'
export readonly RUNSOLVER='RUNSOLVER'

export BENCH_NAME_LENGTH_LIMIT='$$BENCH_NAME_LENGTH_MAX$$'
export PRIMARY_PREPROCESSOR_PATH='$$PRIMARY_PREPROCESSOR_PATH$$'
export NUM_SLOTS='$$NUM_SLOTS$$'
export RAND_SEED='$$RANDSEED$$'
export MAX_MEM='$$MAX_MEM$$'
export BENCH_SAVE_DIR='$$BENCH_SAVE_PATH$$'
export USER_ID='$$USERID$$'
export HAS_DEPENDS='$$HAS_DEPENDS$$'
export BENCH_PATH='$$BENCH$$'
export PAIR_ID='$$PAIRID$$'
export STAREXEC_MAX_WRITE='$$MAX_WRITE$$'
export STAREXEC_CPU_LIMIT='$$MAX_CPUTIME$$'
export STAREXEC_WALLCLOCK_LIMIT='$$MAX_RUNTIME$$'
export SOFT_TIME_LIMIT='$$SOFT_TIME_LIMIT$$'
export KILL_DELAY='$$KILL_DELAY$$'
export REPORT_HOST='$$REPORT_HOST$$'
export SHARED_DIR='$$STAREXEC_DATA_DIR$$'
export SPACE_PATH='$$SPACE_PATH$$'
export SCRIPT_PATH='$$SCRIPT_PATH$$'
export PAIR_OUTPUT_DIRECTORY='$$PAIR_OUTPUT_DIRECTORY$$'
export DB_NAME='$$DB_NAME$$'
export BUILD_JOB='$$BUILD_JOB$$'
export WORKING_DIR_BASE='$$WORKING_DIR_BASE$$'
export BENCH_ID='$$BENCH_ID$$'
export SANDBOX_USER_ONE='$$SANDBOX_USER_ONE$$'
export SANDBOX_USER_TWO='$$SANDBOX_USER_TWO$$'
export DISK_QUOTA='$$DISK_QUOTA$$'
export DISK_QUOTA_EXCEEDED=0
# DB username and password for status reporting
export DB_USER="$$DB_USER$$"
export DB_PASS="$$DB_PASS$$"
export SCRIPT_DIR="$$SCRIPT_DIR$$"

export BENCHMARKING_FRAMEWORK="$$BENCHMARKING_FRAMEWORK$$"

# Path to Olivier Roussel's runSolver
export RUNSOLVER_PATH='$$RUNSOLVER_PATH$$'
# Include common functions and status codes
. $SCRIPT_DIR/functions.bash


# Array of secondary benchmarks starexec paths
declare -a BENCH_DEPENDS_ARRAY

# Array of secondary benchmarks execution host paths
declare -a LOCAL_DEPENDS_ARRAY

#==========================================================================
# Arrays of stage information written from Java
#
#
#==========================================================================

$$STAGE_NUMBER_ARRAY$$

$$CPU_TIMEOUT_ARRAY$$

$$CLOCK_TIMEOUT_ARRAY$$

$$MEM_LIMIT_ARRAY$$

$$SOLVER_ID_ARRAY$$

$$SOLVER_TIMESTAMP_ARRAY$$

$$CONFIG_NAME_ARRAY$$

$$PRE_PROCESSOR_PATH_ARRAY$$
$$PRE_PROCESSOR_TIME_LIMIT_ARRAY$$

$$POST_PROCESSOR_PATH_ARRAY$$
$$POST_PROCESSOR_TIME_LIMIT_ARRAY$$

$$SPACE_ID_ARRAY$$

$$SOLVER_NAME_ARRAY$$

$$SOLVER_PATH_ARRAY$$

$$BENCH_INPUT_ARRAY$$

$$BENCH_SUFFIX_ARRAY$$

$$RESULTS_INTERVAL_ARRAY$$

$$STDOUT_SAVE_OPTION_ARRAY$$

$$EXTRA_SAVE_OPTION_ARRAY$$

NUM_STAGES=${#STAGE_NUMBERS[@]}

((NUM_BENCH_INPUTS = ${#BENCH_INPUT_PATHS[@]} - 1)) # we terminate the array with an empty string, so the number is the size-1

#==========================================================================
# Signal Traps
#
# These are just for the limits we are asking SGE to enforce.
# For the ones runsolver is enforcing, we will look in the
# watcher.out file.
#==========================================================================

trap "limitExceeded 'file write' $EXCEED_FILE_WRITE" SIGXFSZ
trap 'echo "Jobscript received USR2."' USR2
trap exitJobscript EXIT


#==========================================================================
# Execute (prolog will take care of data staging)
#==========================================================================

echo "STAREXEC Job Log"
echo "(C) $(date +%Y) The University of Iowa"
echo "====================================="
echo "date: $(date)"
echo "user: $USER_ID"
echo "pair id: $PAIR_ID"
echo "solver ids: ${SOLVER_IDS[*]}"
echo "benchmark id: $BENCH_ID"
echo "execution host: $HOSTNAME"
echo "benchmarking framework: $BENCHMARKING_FRAMEWORK"
echo "whoami: $(whoami)"

if [ "$BENCHMARKING_FRAMEWORK" == "$BENCHEXEC" ]; then
	log "Using benchexec."
else
	log "Using runsolver."
fi

if [ $BUILD_JOB == "true" ]; then
	echo "This is a solver build job"
fi

echo ""

initSandbox
initWorkspaceVariables
cleanWorkspace 1
setStartTime

createLocalTmpDirectory

log "there are this many benchmark inputs $NUM_BENCH_INPUTS"

decodePathArrays

$$STAGE_DEPENDENCY_ARRAY$$
fillDependArrays

checkIfBenchmarkDependenciesExists
dependenciesExist=$?
log "dependenciesExist was $dependenciesExist"
if ((dependenciesExist == 0)); then
	sendStatus "$ERROR_BENCH_DEPENDENCY_MISSING"
	exit 0
fi
#todo: alter the way we copy runsolver over so it only happens once
copyBenchmarkDependencies

#==============================================================================
# This is the start of the loop that runs for each stage
#
#
#==============================================================================


# directory containing all the output files from a solver. This won't exist until the second stage
# and onwards
OTHER_INPUT_PATH=""

# if we are using pipelines, the OTHER_INPUT_PATH should always exist.
# so, we start with an empty directory here
if ((NUM_STAGES > 1)); then
	mkdir "$OUT_DIR"/empty_output_dir
	OTHER_INPUT_PATH="$OUT_DIR"/empty_output_dir
fi

log "the number of stages is $NUM_STAGES"

for (( STAGE_INDEX = 0; STAGE_INDEX < NUM_STAGES; ++STAGE_INDEX )); do
	CURRENT_STAGE_MEMORY=$MAX_MEM   #start with the default max mem
	CURRENT_STAGE_CPU=$STAREXEC_CPU_LIMIT
	CURRENT_STAGE_WALLCLOCK=$STAREXEC_WALLCLOCK_LIMIT

	if ((CURRENT_STAGE_MEMORY > "${STAGE_MEM_LIMITS[STAGE_INDEX]}")); then
		CURRENT_STAGE_MEMORY=${STAGE_MEM_LIMITS[STAGE_INDEX]}
	fi

	if ((CURRENT_STAGE_CPU > "${STAGE_CPU_TIMEOUTS[STAGE_INDEX]}")); then
		CURRENT_STAGE_CPU=${STAGE_CPU_TIMEOUTS[STAGE_INDEX]}
	fi

	if ((CURRENT_STAGE_WALLCLOCK > "${STAGE_CLOCK_TIMEOUTS[STAGE_INDEX]}")); then
		CURRENT_STAGE_WALLCLOCK=${STAGE_CLOCK_TIMEOUTS[STAGE_INDEX]}
	fi

	POST_PROCESSOR_PATH=${POST_PROCESSOR_PATHS[STAGE_INDEX]}
	POST_PROCESSOR_TIME_LIMIT=${POST_PROCESSOR_TIME_LIMITS[STAGE_INDEX]}

	PRE_PROCESSOR_PATH=${PRE_PROCESSOR_PATHS[STAGE_INDEX]}
	PRE_PROCESSOR_TIME_LIMIT=${PRE_PROCESSOR_TIME_LIMITS[STAGE_INDEX]}

	CONFIG_NAME=${CONFIG_NAMES[STAGE_INDEX]}
	SOLVER_NAME=${SOLVER_NAMES[STAGE_INDEX]}
	SOLVER_PATH=${SOLVER_PATHS[STAGE_INDEX]}
	SOLVER_ID=${SOLVER_IDS[STAGE_INDEX]}
	SOLVER_TIMESTAMP=${SOLVER_TIMESTAMPS[STAGE_INDEX]}
	SPACE_ID=${SPACE_IDS[STAGE_INDEX]}
	ARGUMENT_STRING=${STAGE_DEPENDENCIES[STAGE_INDEX]}
	CURRENT_BENCH_SUFFIX=${BENCH_SUFFIXES[STAGE_INDEX]}
	SUPPRESS_TIMESTAMP=$$SUPPRESS_TIMESTAMP_OPTION$$
	RESULTS_INTERVAL=${RESULTS_INTERVALS[STAGE_INDEX]}
	STDOUT_SAVE_OPTION=${STDOUT_SAVE_OPTIONS[STAGE_INDEX]}
	EXTRA_SAVE_OPTION=${EXTRA_SAVE_OPTIONS[STAGE_INDEX]}

	#path to where cached solvers are stored
	SOLVER_CACHE_PATH="$WORKING_DIR_BASE/solvercache/$SOLVER_TIMESTAMP/$SOLVER_ID"

	#whether the solver was found in the cache
	SOLVER_CACHED=0

	if [ $BUILD_JOB == "true" ]; then
		LOCAL_CONFIG_PATH="$LOCAL_SOLVER_DIR/starexec_build"
	else
		# The path to the config run script on the execution host
		LOCAL_CONFIG_PATH="$LOCAL_SOLVER_DIR/bin/starexec_run_$CONFIG_NAME"
	fi

	if [ $SOFT_TIME_LIMIT -ne 0 ]; then
		SOFT_TIME_LIMIT_FLAG="--softtimelimit $SOFT_TIME_LIMIT"
	fi

	if [ $KILL_DELAY -ne 0 ]; then
		KILL_DELAY_FLAG="-d $KILL_DELAY"
	fi

	createDir "$OUT_DIR/output_files"
	chmod g+w "$OUT_DIR/output_files"

	checkCache
	copyDependencies
	verifyWorkspace
	sandboxWorkspace

	log "listing contents of sandbox..."
	ls -lR "$WORKING_DIR"

	CURRENT_STAGE_NUMBER=${STAGE_NUMBERS[STAGE_INDEX]}

	#Give job pairs 2 extra minutes before we kill them.
	JOB_PAIR_EXTRA_TIME=120

	log "doing stage index $STAGE_INDEX"
	log "stage number : $CURRENT_STAGE_NUMBER"
	log "cpu timeout : $CURRENT_STAGE_CPU"
	log "clock timeout : $CURRENT_STAGE_WALLCLOCK"
	log "global cpu timeout : $STAREXEC_CPU_LIMIT"
	log "global wallclock timeout : $STAREXEC_WALLCLOCK_LIMIT"
	log "extra time for job pair after wallclock timeout : $JOB_PAIR_EXTRA_TIME"
	log "mem limit : $CURRENT_STAGE_MEMORY"
	log "solver id : $SOLVER_ID"
	log "solver name : $SOLVER_NAME"
	log "solver path : $SOLVER_PATH"
	log "solver timestamp : $SOLVER_TIMESTAMP"
	log "config name : $CONFIG_NAME"
	log "pre processor path : $PRE_PROCESSOR_PATH"
	log "post processor path : $POST_PROCESSOR_PATH"
	log "space id : $SPACE_ID"
	log "bench path : $BENCH_PATH"
	log "benchmark suffix : $CURRENT_BENCH_SUFFIX"
	log "argument string : $ARGUMENT_STRING"
	log "results interval : $RESULTS_INTERVAL"
	log "stdout save option : $STDOUT_SAVE_OPTION"
	log "additional output save option : $EXTRA_SAVE_OPTION"
	log "user disk quota : $DISK_QUOTA"

	log "whoami: $(whoami)"

	# this will be set to true in killDeadlockedJobPair if the job pair gets deadlocked
	echo "Calling killDeadlockedJobPair"

	killDeadlockedJobPair "$CURRENT_STAGE_WALLCLOCK" $JOB_PAIR_EXTRA_TIME $SANDBOX_PARAM &
	# Get the process of our defensive killDeadlockedJobPair function.
	KILL_DEADLOCKED_JOB_PAIR_PID=$!
	echo "KILL_DEADLOCKED_JOB_PAIR_PID = $KILL_DEADLOCKED_JOB_PAIR_PID"

	if ((RESULTS_INTERVAL > 0)); then
		copyOutputIncrementally "$RESULTS_INTERVAL" "$STAREXEC_WALLCLOCK_LIMIT" "$CURRENT_STAGE_NUMBER" "$STDOUT_SAVE_OPTION" "$EXTRA_SAVE_OPTION" &
		COPY_OUTPUT_INCREMENTALLY_PID=$!
	fi

	if [ $BUILD_JOB == "true" ]; then
		cd $WORKING_DIR_BASE/$SANDBOX_PARAM/solver
		rm "$LOCAL_BENCH_PATH"
		LOCAL_BENCH_PATH=""
	else
		cd $WORKING_DIR_BASE/$SANDBOX_PARAM/solver/bin
	fi

	pwd

	log "listing contents of the output directory"
	ls -l "$OUT_DIR"

	if [ "$BENCHMARKING_FRAMEWORK" == "$BENCHEXEC" ]; then
		((MEM_IN_BYTES = CURRENT_STAGE_MEMORY * 1000000))

		log "Setting up cpuset cgroup..."
		echo $$ > /sys/fs/cgroup/cpuset/system.slice/benchexec-cgroup.service/tasks
		log "Setting up cpuacct cgroup..."
		echo $$ > /sys/fs/cgroup/cpuacct/system.slice/benchexec-cgroup.service/tasks
		log "Setting up memory cgroup..."
		echo $$ > /sys/fs/cgroup/memory/system.slice/benchexec-cgroup.service/tasks
		log "Setting up freezer cgroup..."
		echo $$ > /sys/fs/cgroup/freezer/system.slice/benchexec-cgroup.service/tasks

		log "Running runexec."
		echo "runexec --no-container --memlimit $MEM_IN_BYTES --timelimit $CURRENT_STAGE_CPU --walltimelimit $CURRENT_STAGE_WALLCLOCK $SOFT_TIME_LIMIT_FLAG --cores $CORES --output $STDOUT_FILE $LOCAL_CONFIG_PATH $LOCAL_BENCH_PATH $OUT_DIR/output_files $OTHER_INPUT_PATH $ARGUMENT_STRING"

		# Run runexec, runexec will output the stdout of the solver to $OUT_DIR/stdout.txt and will output statistics to $OUT_DIR/var.out
		sudo -u $SANDBOX_PARAM TMPDIR="$LOCAL_TMP_DIR" STAREXEC_MAX_MEM="$CURRENT_STAGE_MEMORY" STAREXEC_MAX_WRITE=$$MAX_WRITE$$ STAREXEC_CPU_LIMIT="$CURRENT_STAGE_CPU" STAREXEC_WALLCLOCK_LIMIT="$CURRENT_STAGE_WALLCLOCK" $$JOBPAR_EXECUTION_PREFIX$$ runexec --no-container --memlimit $MEM_IN_BYTES --timelimit $CURRENT_STAGE_CPU --walltimelimit $CURRENT_STAGE_WALLCLOCK $SOFT_TIME_LIMIT_FLAG --cores $CORES --output "$STDOUT_FILE" "$LOCAL_CONFIG_PATH" "$LOCAL_BENCH_PATH" "$OUT_DIR/output_files" $OTHER_INPUT_PATH $ARGUMENT_STRING > "$VARFILE"

		# make a blank watchfile
		touch "$WATCHFILE"
	else
		# run runsolver
		log "Running runsolver."
		echo "  $LOCAL_RUNSOLVER_PATH $TIMESTAMP_PARAM $ADDEOF_PARAM --cores $CORES -C $CURRENT_STAGE_CPU -W $CURRENT_STAGE_WALLCLOCK $KILL_DELAY_FLAG -M $$MAX_MEM$$  -w $WATCHFILE -v $VARFILE -o $STDOUT_FILE $LOCAL_CONFIG_PATH $LOCAL_BENCH_PATH"

		if [ "$SUPPRESS_TIMESTAMP" = true ]; then
			log "Suppressing timestamp."
			TIMESTAMP_PARAM=""
			ADDEOF_PARAM=""
		else
			log "Not suppressing timestamp."
			TIMESTAMP_PARAM="--timestamp"
			ADDEOF_PARAM="--add-eof"
		fi

		sudo -u "$SANDBOX_PARAM" TMPDIR="$LOCAL_TMP_DIR" STAREXEC_MAX_MEM="$CURRENT_STAGE_MEMORY" STAREXEC_MAX_WRITE=$$MAX_WRITE$$ STAREXEC_CPU_LIMIT="$CURRENT_STAGE_CPU" STAREXEC_WALLCLOCK_LIMIT="$CURRENT_STAGE_WALLCLOCK" $$JOBPAR_EXECUTION_PREFIX$$ "$LOCAL_RUNSOLVER_PATH" $TIMESTAMP_PARAM $ADDEOF_PARAM --cores $CORES -C $CURRENT_STAGE_CPU -W $CURRENT_STAGE_WALLCLOCK $KILL_DELAY_FLAG -M $MAX_MEM  -w "$WATCHFILE" -v "$VARFILE" -o "$STDOUT_FILE" "$LOCAL_CONFIG_PATH" "$LOCAL_BENCH_PATH" "$OUT_DIR/output_files" "$OTHER_INPUT_PATH" $ARGUMENT_STRING
	fi


	# move the working directory back to the top level sandbox
	cd $WORKING_DIR_BASE/$SANDBOX_PARAM
	if ((RESULTS_INTERVAL > 0)); then
		kill $COPY_OUTPUT_INCREMENTALLY_PID
	fi

	# If runsolver finishes then stop the killDeadlockedJobPair function.
	log "Runsolver finished. Killing the defensive killall function."
	kill $KILL_DEADLOCKED_JOB_PAIR_PID
	KILL_EXIT_STATUS=$?
	JOB_PAIR_DEADLOCKED=false
	if ((KILL_EXIT_STATUS == 1)); then
		#killDeadlockedJobPair was dead so the job pair
		log "Job pair was deadlocked."
		JOB_PAIR_DEADLOCKED=true
	fi

	log "listing contents of the output directory recursively"
	ls -lhR "$OUT_DIR"

	log "recording header of job pair output"
	head -5 "$STDOUT_FILE"

	if [ ! -s "$VARFILE" ]; then
		echo "Varfile could not be found. Terminating job pair."
		copyOutputNoStats "$CURRENT_STAGE_NUMBER" "$STDOUT_SAVE_OPTION" "$EXTRA_SAVE_OPTION" "$BENCHMARKING_FRAMEWORK"
		markRunscriptError "${STAGE_NUMBERS[STAGE_INDEX]}"
		exit 0
	fi

	if [[ "$BENCHMARKING_FRAMEWORK" == "$RUNSOLVER" ]] && [[ ! -s "$WATCHFILE" ]]; then
		log "Runsolver watchfile could not be found. Terminating job pair."
		copyOutput "$CURRENT_STAGE_NUMBER" "$STDOUT_SAVE_OPTION" "$EXTRA_SAVE_OPTION" "$BENCHMARKING_FRAMEWORK"
		markRunscriptError "${STAGE_NUMBERS[STAGE_INDEX]}"
		exit 0
	fi

	if ! isOutputValid "$STDOUT_FILE" "$CURRENT_STAGE_NUMBER" "$SUPPRESS_TIMESTAMP" "$BENCHMARKING_FRAMEWORK" ; then
		log "runsolver output was not valid. Terminating job pair."
		markRunscriptError "${STAGE_NUMBERS[STAGE_INDEX]}"
		exit 0
	fi

	# check for which cores we are on
	log "epilog checking for information on cores, from runsolver's watch file:"
	grep 'cores:' "$WATCHFILE"

	# this also does post processing
	copyOutput "$CURRENT_STAGE_NUMBER" "$STDOUT_SAVE_OPTION" "$EXTRA_SAVE_OPTION" "$BENCHMARKING_FRAMEWORK"

	if ((STDOUT_SAVE_OPTION == 3)); then
		saveStdoutAsBenchmark
	fi

	if ((EXTRA_SAVE_OPTION == 3)); then
		saveExtraOutputAsBenchmarks
	fi

	ERROR_DETECTED=false

	if [ "$BENCHMARKING_FRAMEWORK" == "$BENCHEXEC" ]; then
		log "Checking for errors for benchexec."
		if (grep 'terminationreason=cputime' "$VARFILE") then
			sendCpuExceededStatus
			ERROR_DETECTED=true
		elif (grep 'terminationreason=walltime' "$VARFILE") || [ "$JOB_PAIR_DEADLOCKED" = true ]; then
			sendWallclockExceededStatus
			ERROR_DETECTED=true
		elif (grep 'terminationreason=memory' "$VARFILE") then
			sendExceedMemStatus
			ERROR_DETECTED=true
		elif (grep 'terminationreason=' "$VARFILE") then
			# generic case for failure
			ERROR_DETECTED=true
		fi
	elif [ "$BENCHMARKING_FRAMEWORK" == "$RUNSOLVER" ]; then
		log "Checking for errors for runsolver."
		if (grep 'job error:' "$SGE_STDOUT_PATH" > /dev/null) then
			ERROR_DETECTED=true
		elif (grep 'wall clock time exceeded' "$WATCHFILE" ) || [ "$JOB_PAIR_DEADLOCKED" = true ]; then
			sendWallclockExceededStatus
			ERROR_DETECTED=true
		elif (grep 'CPU time exceeded' "$WATCHFILE" ) then
			sendCpuExceededStatus
			ERROR_DETECTED=true
		elif (grep 'VSize exceeded' "$WATCHFILE" ) then
			sendExceedMemStatus
			ERROR_DETECTED=true
		fi
	fi

	if [ "$ERROR_DETECTED" = false ] && ((DISK_QUOTA_EXCEEDED == 1)); then
		log "Error: disk quota exceeded"
		sendStatus "$ERROR_DISK_QUOTA_EXCEEDED"
		sendStageStatus "$ERROR_DISK_QUOTA_EXCEEDED" "${STAGE_NUMBERS[STAGE_INDEX]}"
		ERROR_DETECTED=true
	fi

	# This appears to be the idiomatic Bash way to test if a value is false.
	if [ "$ERROR_DETECTED" = false ]; then
		log "execution on $HOSTNAME complete"
		sendStageStatus "$STATUS_COMPLETE" "${STAGE_NUMBERS[STAGE_INDEX]}"

		if ((STAGE_INDEX == NUM_STAGES - 1)); then
			log "job finished successfully"
			sendStatus "$STATUS_COMPLETE"
		fi
		log "STAREXEC job $PAIR_ID completed successfully"
	else
		log "STAREXEC job $PAIR_ID exited with errors"
		sendStatusToLaterStages "$STATUS_NOT_REACHED" "${STAGE_NUMBERS[STAGE_INDEX]}"
		setRunStatsToZeroForLaterStages "${STAGE_NUMBERS[STAGE_INDEX]}"
		cleanUpAfterKilledBuildJob
	fi

	if [ $BUILD_JOB == "true" ]; then
		copySolverBack
	fi

	cleanForNextStage

	# update the path to the input to the output of the last stage
	BENCH_PATH="$SAVED_OUTPUT_DIR/$CURRENT_STAGE_NUMBER"
	OTHER_INPUT_PATH=$SAVED_OUTPUT_DIR"/"$CURRENT_STAGE_NUMBER"_output"
done

setEndTime

cleanWorkspace 0 $SANDBOX_PARAM
