#!/bin/bash

function prependTimestamp() {
	START_TIME_MILLI="$(date +%s%N | cut -b1-13)"
	while IFS='' read -r line || [[ -n "$line" ]]; do
		CUR_TIME_MILLI="$(date +%s%N | cut -b1-13)"
		SECONDS_PASSED=$(echo "($CUR_TIME_MILLI - $START_TIME_MILLI) / 1000.0" | bc -l)
		printf "%0.2f\t\t" $SECONDS_PASSED
		echo $line
	done < "/dev/stdin"
}

$1 "${@:2}" | prependTimestamp
