StarExecCommand User Guide
--------------------------

StarExecCommand is a program that can be used to communicate with the StarExec server from the 
command line. StarExecCommand can be used on any system with Java installed, and can be run by 
using the following command at a command prompt.

Java -jar StarexecCommand.jar 

After starting the program, a shell accepting commands will start up. Typically, the command "login"
will be the first you will want to run-- the login command will take in credentials and start a connection
to StarExec. See the documentation on commands for more information.

Syntax
------

StarexecCommand runs user commands one by one. All commands have a 
command followed by a set of parameters, using the following syntax

{command} {parameters}

Where {command} specifies a particular action and where parameters are space-separated key/value 
pairs of the form {key}={value}. There should be no spaces between {key} and "="--there may be any 
number of spaces in {value}. Some parameters may not have values, in which case {value} can be the 
empty string. Neither command nor parameter keys are case sensitive--parameter values ARE case 
sensitive.  Parameters can be specified in any order. The parameters accepted by StarExecCommand are 
described below. An example command using this syntax is...

'getjobinfo id=472 out=somefilelocation.zip'

Many other examples are given in the sections explaining specific commands. Note that quote marks are used for clarity only--
they should NOT be included in commands.

Parameters
----------

The following parameters are used in various commands. Quotation marks here are used for emphasis-- they should not 
be included when specifying parameters.

"addr"--Specifies the base URL at which to log into Starexec. The URL should be absolute and should end with a '/'.

"allPerm"--Indicated that all permissions should be enabled for an upload. All permissions are disabled 
by default. allPerm has no value.

{"addSolver", "addUser", "addSpace", "addJob", "addBench", "removeSolver" ,"removeUser", 
"removeSpace", "removeJob", "removeBench"} - These are ten separate parameters specify that the 
corresponding permission should be set to true. None requires a value.

"bt" -Specifies the ID of the benchmark type that should be used for a benchmark upload

"cpu"--Specifies the cpu timeout that should be used for a new job, in seconds.

"d"--Specifies a description. When optional, the empty string is used as a default.

"dep"--Specifies a dependency for a benchmark upload.

"df" -Specifies a path to a local file that will contain a description

"downloadable" -Specifies that a benchmark upload should be downloadable. 

"f" -Specifies a path to a  local file.

"hier" -On a benchmark upload, means that the archive structure should be converted to a space 
structure. By default, all benchmarks are placed in the space being uploaded to.

"id" -Specifies the ID of a primitive, whether that be a space, job, benchmark, etc.

"limit" -When printing primitives to the command line, specifies the maximum number to retrieve and print

"link" -When uploading benchmarks with dependencies, indicates that the first directory in the path 
corresponds to a dependent bench space

"lock" -Sets a newly created subspace as locked.

"n" -Specifies a name. When optional, the default is the date.

"out" -Specifies a filepath to which a file should be downloaded.

"ow" -Indicates that the file at the path given by "out" should be overwritten with the new file

"pid" -Specifies a processor id (used only in job creation)

"qid" -Specifies a queue id (used only in job creation)

"since" -When downloading job info, specifies the earliest completed pair after which to get info

"t" -When using 'sleep,' specifies the amount of time in seconds. Decimal values are permitted.

"url" -Specifies the URL of a remote file

"val" -Specifies a new value for a user setting

"verbose" -Tells StarExec that status should be printed to the standard output in batch mode

"w" -Specifies the wallclock timeout that should be used for a new job, in seconds

Commands
--------

The following notes describe each command recognized by StarExec. Different commands expect 
different parameters, which are described below. Many commands include both required and optional 
parameters.  Including parameters that a command does not expect will not cause an error--such 
parameters are simply ignored.

General Commands
----------------

These commands have a range of functions.

--Exit -Logs the user out of StarExec and exits StarexecCommand. Requires no parameters.

--login -This command takes the parameters "u" and "p" and attempts to log into StarExec with that 
username and password. If "u" is equal to "guest," then a password is not needed and a guest login
will be performed. For example...

'login u=fake@email.com p=password'

'login u=guest'

By default, StarExecCommand attempts to log into www.starexec.org, but an alternative instance can also be specified.
An example for connecting to an alternate instance of starexec would be

'login u=fake@email.com p=password addr=http://stardev.cs.uiowa.edu/starexec/'

In this case, 'addr' must be in this exact format-- it should point to the home page, not to any
deeper page, and it should end with a '/'

--logout -Logs the user out of StarExec. Requires no parameters.

--polljob -Downloads both job output and job info at regular intervals until the job is complete. Expects
"id" "out" and "t." The files saved will have the path given by "out" plus either "-info" or "-output" and an integer

--runfile -Given "f" containing one line per command, execute all the commands in succession. With "verbose" also print
out status to standard output. The user does not have to be logged in before running this command.

--sleep -Given "t" sleep for the specified number of seconds

Download Commands
-----------------

Every download command retrieves an archive from the server and saves it in a specified location. 
Downloaded archives will be in the format specified in the user's default settings on StarExec.
The following commands are used to download files from the server. All of them expect two 
parameters. "id", and "out", a If a file already exists at the path specified by "out," the parameter "ow" 
should be included. "ow" requires no value. An example download command is...

'getspacexml id=1 out=xmlfile.zip'

Another example, which will cause the output file to be overwritten, is...

'getspacexml id=1 out=xmlfile.zip ow='

The following commands can be used for downloading

getbench -Downloads the benchmark associated with the given benchmark id. 

getbenchproc -Downloads all benchmark processors associated with the given community id. (Note the 
"id," NOT "pid," is the expected parameter).

Getjobinfo -Get the CSV pertaining to the job associated with the given id.

Getjobout -Gets the output for all the job pairs associated with the job with the given id.

getjobpair -Downloads the output from the job pair associated with the given id.

Getnewjobinfo -Get the CSV pertaining to the job associated with the given id containing only
info related to job pairs that have been completed since the last time the command was used. 
in the current session. When used with "since," info for every pair completed after "since" will be downloaded 

getnewjobout -Get the output for the job pairs associated with the given job that have been 
completed since the last time the command was used in the current session. When used with "since,"
retrieve all pairs completed after "since."

getpostproc -Downloads all post processors associated with the given community id. (Note that "id," 
NOT "pid," is the expected parameter).

getsolver -Downloads the solver associated with the given community id.

Getspace -Donwloads the space at the given ID.

getspacehierarchy -Downloads the space hierarchy associated with the given space id.

getspacexml -Downloads an XML file representing

Set Commands
------------

These commands are used to change user settings for the current user. The first four of the following 
commands expect one parameter of the form "val={newval}", where "newval" will be the new value of 
the selected setting. The last two commands expect the parameter "id," where "id" is a space ID, and 
also accept the optional parameter "hier".

Setarchivetype -Given a val of zip, tar, tar.gz, or tgz, sets the default archive type for the user
EX: 'setarchivetype val=zip'

Setfirstname -Sets the user's first name
EX: 'setfirstname val=newname'

Setinstitution -Sets the user's institution.
EX: 'setinstitution val=The University of Iowa'

Setlastname -Sets the user's last name

Setspaceprivate -Sets the space with the given id as private--with "hier," sets entire hierarchy private
EX: 'setspaceprivate id=1 hier=' 

Setspacepublic -Sets the space with the given id as public--with "hier," sets entire hierarchy public.

Push Commands
-------------

These commands allow users to push benchmarks, solvers, and processors to the server. They are more 
individually unique than previous commands, and many accept a large number of optional parameters. 

Pushsolver -This command is used for uploading a solver. The parameter "id" refers to a space ID and is 
required, and either "f" or "url" is also required. The parameters "n," "d," "df," and "downloadable" are 
optional. Only one of "d" and "df" can be set.
EX: 'pushsolver id=153 f=solverdirectory.tar n=fakesolver d=Solver Description downloadable='

Pushbenchmarks -Uploads a benchmark archive to an existing space. "id," "type," and either "f" or "url" 
are required. "d," "df," "dep," "downloadable," "hier," and "link," are optional. All of the permission 
parameters are also optional.
EX: 'pushbenchmarks id=231 url=http://www.benchmarksite.com/benchmarks.zip df=descriptionfile.tar '

Pushbenchproc -This command uploads a benchmark processor to a community. The parameter "id" 
refers to a community id and is required, as is "f." The parameters "n" and "d" are optional.
EX: 'pushbenchproc id=142 f=procdirectory.tgz d=Processor Description n=newproc'

Pushconfig -This command is used for uploading a configuration for an existing solver. The parameter 
"id" refers to a solver ID and is required. "f" is also required. "n" and "d" are optional. 
EX: 'pushbenchproc id=4 f=configdirectory.tgz d=New Configuration n=config'

Pushpostproc -This command uploads a post processor to a community. The parameter "id" refers to a 
community id and is required, as is "f." The parameters "n" and "d" are optional.

Pushspacexml -This command is used for uploading  a space XML. The parameter "id" refers to a space 
ID and is required. The parameter "f" is also required.

Delete Commands
---------------

These commands are all used to delete primitives from Starexec. All of them accept a single parameter, 
"id." Delete commands all look like the following example...

'deleteconfig id=5'

Deletebench -Deletes the benchmark with the given ID.

Deletebenchproc -Deletes the benchmark processor with the given ID.

Deleteconfig -Deletes the configuration with the given ID.

Deletesolver -Deletes the solver with the given ID.

Deletejob -Not yet implemented

Deletepostproc -Deletes the post processor with the given ID.

Deletespace -Not yet implemented

Create Commands
---------------

These commands create some new information on StarExec. Like push commands, they also accept a 
varied set of parameters.

createjob--This command is used for creating a new job in a given space. "id," "pid," and "qid," are 
required, and  describe the space ID of the space to run the job, the ID of the post processor to be used, 
and the ID of the queue to be used, respectively. "n," "d," "w," and "cpu" are optional. Currently, only 
jobs that run every benchmark and solver and keep the hierarchy structure can be created from 
StarExecCommand--other options will be added in the future.
EX: 'createjob id=5 pid=2 qid=3 n=commandjob w=200 cpu=100'

createsubspace--Creates a subspace of an existing space. "id" refers to the id of the existing space and 
is required. "n," "d," "lock," and all of the permission parameters are optional.
EX: 'createsubspace id=5'

List Commands
-------------

These commands will print the contents of a given space to the console. IDs and names will be printed.
Every command in this category requires the "id" parameter, specifying a space. The parameter "limit"
is optional, and indicates that at most "limit" primitives should be printed. An example is given below

'lsjobs id=4 limit=100'

This will print at most 100 jobs present in space 4. The list commands are shown below.

lsbenchmarks -Lists the IDs and names of all benchmarks in the space.

lsjobs -Lists the IDs and names of all jobs in the space.

lssolvers -Lists the IDs and names of all solvers in the space.

lssubspaces -Lists the IDs and names of all subspaces in the space the current user is authorized to see

lsusers -Lists the IDs and names of all users in the space.

Shell Mode
----------

In shell mode, commands are issued one at a time to an interactive prompt. StarExecCommand will start 
in shell mode by default during every session.


Batch Mode
----------

In batch mode, which is initiated by the runfile command StarExecCommand will read commands from a text file 
and execute them in succession. Commands have the same syntax as they do in shell mode, and should be placed one per line. By 
default, StarExecCommand will execute silently in batch mode--to see information regarding the 
progress of the execution, add the additional argument "verbose" after the filepath when starting 
StarExecCommand. An example of a shell startup would be...

runfile f=fileofcommands.txt verbose=

This would run StarExecCommand on the file 'fileofcommands.txt,' and it would also
instruct the program to print out status to standard output.