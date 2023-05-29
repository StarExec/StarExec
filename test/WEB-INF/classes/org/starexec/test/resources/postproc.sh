#!/bin/bash
# Aaron Stump
# A simple post processor for SMT.

if ( grep unsat $1 > /dev/null) then 
        echo "starexec-result=unsat";
elif ( grep sat $1 > /dev/null ) then
        echo "starexec-result=sat";
elif ( grep unknown $1 > /dev/null ) then
        echo "starexec-result=unknown";
else 
        echo "starexec-result=none";
fi