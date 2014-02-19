#!/bin/tcsh

chmod +x ../src/org/starexec/command/build/StarexecCommand.jar 

./../src/org/starexec/command/build/StarexecCommand.jar < testDataCommands.txt
