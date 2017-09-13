#!/bin/bash

# STAREXEC JOB STATUS CODES
# These variables are defined for use in the jobscript
# They MUST correspond with the status codes defined in the starexec schema

readonly STATUS_UNKNOWN=0
readonly STATUS_PENDING_SUBMIT=1
readonly STATUS_ENQUEUED=2
readonly STATUS_RUNNING=4
readonly STATUS_COMPLETE=7
readonly ERROR_SGE_REJECT=8
readonly ERROR_SUBMIT_FAIL=9
readonly ERROR_RESULTS=10
readonly ERROR_RUNSCRIPT=11
readonly ERROR_BENCHMARK=12
readonly ERROR_DISK_QUOTA_EXCEEDED=13
readonly EXCEED_RUNTIME=14
readonly EXCEED_CPU=15
readonly EXCEED_FILE_WRITE=16
readonly EXCEED_MEM=17
readonly ERROR_GENERAL=18
readonly STATUS_PROCESSING_RESULTS=19
readonly STATUS_PAUSED=20
readonly STATUS_KILLED=21
readonly STATUS_NOT_REACHED=23
readonly ERROR_BENCH_DEPENDENCY_MISSING=24
