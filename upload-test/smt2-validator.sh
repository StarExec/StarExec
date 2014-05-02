#!/bin/bash
# AUTHOR: Tyler Jensen
# DESCRIPTION: An example benchmark processor for .smt2 benchmarks
# arg1 = the absolute path to the benchmark file

# First extract the logic family of the benchmark
LOGIC=`grep set-logic "$1" | sed -e "s/(set-logic //" -e 's/[(|)]//g'`

# Here we perform basic validation: this benchmark must belong to a logic family
if [ "$LOGIC" == "" ]; then
        # The special starexec validation property
        echo "starexec-valid=false"
        exit 0
else
        # The special starexec validation property
        echo "starexec-valid=true"
fi

# Here we output the rest of the properties we wish to capture

# We want to capture the logic
echo "set-logic=$LOGIC"

# Here we process the rest of the benchmark metadata. The command below
# formats the metadata by removing invalid characters and transforming
# it into a nice key=value mapping
grep set-info "$1" | sed -e "/source/d" -e "s/(set-info ://" -e "s/ /=/" -e "s/)//" -e "s/[^A-Za-z0-9_\.=-]//g"
