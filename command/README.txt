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
be included when specifying parameters. Parameters are NOT case sensitive.

"addr"--Specifies the base URL at which to log into Starexec. The URL should be absolute and should end with a '/'.

"allPerm"--Indicated that all permissions should be enabled for an upload. All permissions are disabled 
by default. allPerm has no value.

{"addSolver", "addUser", "addSpace", "addJob", "addBench", "removeSolver" ,"removeUser", 
"removeSpace", "removeJob", "removeBench"} - These are ten separate parameters specify that the 
corresponding permission should be set to true. None requires a value.

"bt" -Specifies the ID of the benchmark type that should be used for a benchmark upload.

"cpu"--Specifies the cpu timeout that should be used for a new job, in seconds.

"d"--Specifies a description. When optional, the empty string is used as a default.

"dep"--Specifies a dependency for a benchmark upload.

"df" -Specifies a path to a local file that will contain a description.

"downloadable" -Specifies that a benchmark upload should be downloadable. 

"f" -Specifies a path to a  local file.

"from" -When copying a primitive, specifies the ID of the source space.

"hier" -On a benchmark upload, means that the archive structure should be converted to a space 
structure. By default, all benchmarks are placed in the space being uploaded to.

"id" -Specifies the ID of a primitive, whether that be a space, job, benchmark, etc.

"limit" -When printing primitives to the command line, specifies the maximum number to retrieve and print.

"link" -When uploading benchmarks with dependencies, indicates that the first directory in the path 
corresponds to a dependent bench space.

"lock" -Sets a newly created subspace as locked.

"n" -Specifies a name. When optional, the default is the date.

"out" -Specifies a filepath to which a file should be downloaded.

"ow" -Indicates that the file at the path given by "out", if it exists, should be overwritten with the new file.

"pid" -Specifies a processor id (used only in job creation).

"qid" -Specifies a queue id (used only in job creation).

"since" -When downloading job info, specifies the earliest completed pair after which to get info.

"t" -When using 'sleep,' specifies the amount of time in seconds. Decimal values are permitted.

"to" -When copying a primitive, specifies the ID of the destination space, or the root of the 
	  destination hierarchy.

"trav" -When creating a job, specifies the type of traversal that shold be used. The value "d" is
		used to specify depth-first traversal, whereas "r" is for a round-robin traversal.
		

"url" -Specifies the URL of a remote file.

"val" -Specifies a new value for a user setting.

"verbose" -Tells StarExec that status should be printed to the standard output in batch mode.

"w" -Specifies the wallclock timeout that should be used for a new job, in seconds.

Commands
--------

The following notes describe each command recognized by StarExec. Different commands
expect different parameters, and many paramters are optional. Required parameters
are listed for every command, as are optional parameters.

General Commands
----------------

These commands have a range of functions.

--Exit -Logs the user out of StarExec and exits StarexecCommand.
REQUIRED: None
OPTIONAL: None

--login -Logs the user into StarExec.
REQUIRED: "u", "p". If "u" equals "guest" then "p" is not required.
OPTIONAL: "addr"
EXAMPLES:

'login u=fake@email.com p=password'

'login u=guest'

By default, StarExecCommand attempts to log into www.starexec.org, but an alternative instance can also be specified.
An example for connecting to an alternate instance of starexec would be

'login u=fake@email.com p=password addr=http://stardev.cs.uiowa.edu/starexec/'

In this case, 'addr' must be in this exact format-- it should point to the home page, not to any
deeper page, and it should end with a '/'

--logout -Logs the user out of StarExec.
REQUIRED: None
OPTIONAL: None

--runfile -Given a file containing a sequence of commands, runs all the commands in succession.
REQUIRED: "f"
OPTIONAL: "verbose"

--sleep -Given "t" sleep for the specified number of seconds
REQUIRED: "t"
OPTIONAL: None

Download Commands
-----------------

Every download command retrieves an archive from the server and saves it in a specified location. 
Downloaded archives will be in the format specified in the user's default settings on StarExec.
Most of them expect two parameters. "id", and "out", a If a file already exists at the path specified 
by "out," the parameter "ow" should be included. "ow" requires no value. An example download command is...

'getspacexml id=1 out=xmlfile.zip'

Another example, which will cause the output file to be overwritten, is...

'getspacexml id=1 out=xmlfile.zip ow='

The following commands can be used for downloading

--getbench -Downloads the benchmark associated with the given benchmark id. 
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getbenchproc -Downloads all benchmark processors associated with the given community id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getjobinfo -Get the CSV pertaining to the job associated with the given id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getjobout -Gets the output for all the job pairs associated with the job with the given id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getjobpair -Downloads the output from the job pair associated with the given id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getnewjobinfo -Get the CSV pertaining to the job associated with the given id containing only
info related to job pairs that have been completed since the last time the command was used. 
in the current session. When used with "since," info for every pair completed after "since" will be downloaded.
REQUIRED: "id" "out"
OPTIONAL: "ow" "since"

--getnewjobout -Get the output for the job pairs associated with the given job that have been 
completed since the last time the command was used in the current session. When used with "since,"
retrieve all pairs completed after "since."
REQUIRED: "id" "out"
OPTIONAL: "ow" "since"

--getpostproc -Downloads all post processors associated with the given community id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getsolver -Downloads the solver associated with the given community id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getspace -Donwloads the space at the given ID.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getspacehierarchy -Downloads the space hierarchy associated with the given space id.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--getspacexml -Downloads an XML file representing.
REQUIRED: "id" "out"
OPTIONAL: "ow"

--polljob -Downloads both job output and job info at the specified interval until the job is complete. Saved 
archives will have name given plus "-info" or "-output" and an integer.
REQUIRED: "id", "out", "t" 
OPTIONAL: "ow"

Set Commands
------------

These commands are used to change user settings for the current user. The first four of the following 
commands expect one parameter of the form "val={newval}", where "newval" will be the new value of 
the selected setting. The last two commands expect the parameter "id," where "id" is a space ID, and 
also accept the optional parameter "hier".

--setarchivetype -Given a val of zip, tar, tar.gz, or tgz, sets the default archive type for the user
REQUIRED: "val"
OPTIONAL: None
EXAMPLE: 'setarchivetype val=zip'

--setfirstname -Sets the user's first name
REQUIRED: "val"
OPTIONAL: None
EXAMPLE: 'setfirstname val=newname'

--setinstitution -Sets the user's institution.
REQUIRED: "val"
OPTIONAL: None
EXAMPLE: 'setinstitution val=The University of Iowa'

--setlastname -Sets the user's last name
REQUIRED: "val"
OPTIONAL: None

--setspaceprivate -Sets the space with the given id as private--with "hier," sets entire hierarchy private
REQUIRED: "id"
OPTIONAL: "hier"
EXAMPLE: 'setspaceprivate id=1 hier=' 

--setspacepublic -Sets the space with the given id as public--with "hier," sets entire hierarchy public.
REQUIRED: "id" "hier"
OPTIONAL: None

Push Commands
-------------

These commands allow users to push benchmarks, solvers, and processors to the server. They are more 
individually unique than previous commands, and many accept a large number of optional parameters. 

--pushsolver -This command is used for uploading a solver. The parameter "id" refers to a space ID.
REQUIRED: ("f" OR "url") "id"
OPTIONAL: "n" ("d" OR "df") "downloadable"
EXAMPLE: 'pushsolver id=153 f=solverdirectory.tar n=fakesolver d=Solver Description downloadable='

--pushbenchmarks -Uploads a benchmark archive to an existing space. "id" refers to a space ID.
REQUIRED: ("f" OR "url") "id" "bt"
OPTIONAL: ("d" OR "df") "dep" "downloadable" "hier" "link" "allPerm" (also all other permission parameters)
EXAMPLE: 'pushbenchmarks id=231 url=http://www.benchmarksite.com/benchmarks.zip df=descriptionfile.tar '

--pushbenchproc -This command uploads a benchmark processor to a community. The parameter "id" 
refers to a community id.
REQUIRED: "f" "id"
OPTIONAL: "n" "d"
EXAMPLE: 'pushbenchproc id=142 f=procdirectory.tgz d=Processor Description n=newproc'

--pushconfig -This command is used for uploading a configuration for an existing solver. The parameter 
"id" refers to a solver ID.
REQUIRED: "f" "id"
OPTIONAL: "n" "d"
EXAMPLE: 'pushbenchproc id=4 f=configdirectory.tgz d=New Configuration n=config'

--pushpostproc -This command uploads a post processor to a community. The parameter "id" refers to a 
community id.
REQUIRED: "f" "id"
OPTIONAL: "n" "d"

--pushspacexml -This command is used for uploading  a space XML. The parameter "id" refers to a space 
ID and is required. 
REQUIRED: "f" "id"
OPTIONAL: None

Delete Commands
---------------

These commands are all used to delete primitives from Starexec. All of them accept a single parameter, 
"id." Delete commands all look like the following example...

'deleteconfig id=5'

--deletebench -Deletes the benchmark with the given ID.
REQUIRED: "id"
OPTIONAL: None

--deletebenchproc -Deletes the benchmark processor with the given ID.
REQUIRED: "id"
OPTIONAL: None

--deleteconfig -Deletes the configuration with the given ID.
REQUIRED: "id"
OPTIONAL: None


--deletesolver -Deletes the solver with the given ID.
REQUIRED: "id"
OPTIONAL: None

--deletepostproc -Deletes the post processor with the given ID.
REQUIRED: "id"
OPTIONAL: None

Remove Commands
---------------

This set of commands can be used to remove the associations between primitives
and spaces. Solvers and benchmarks that are removed from every space they 
belong to will be deleted.

-- removebench -Removes a benchmark from a given space. "id" is the benchmark ID.
REQUIRED: "id" "from"
OPTIONAL: None

-- removejob -Removes a job from a given space. "id" is the job ID.
REQUIRED: "id" "from"
OPTIONAL: None

-- removesolver -Removes a solver from a given space. "id" is the solver ID.
REQUIRED: "id" "from"
OPTIONAL: None

-- removesubspace -Removes a subspace from a given space. "id" is the subspace ID.
REQUIRED: "id" "from"
OPTIONAL: None

-- removeuser -Removes a user from a given space. "id" is the user ID.
REQUIRED: "id" "from"
OPTIONAL: None

Create Commands
---------------

These commands create some new information on StarExec. Like push commands, they also accept a 
varied set of parameters.

--createjob -This command is used for creating a new job in a given space. "id" refers to the id of
the space that the job should be created in. Currently, only jobs that run every benchmark and solver and keep
the hierarchy structure can be created from StarExecCommand. By default, job-pairs are run in a depth-first manner,
the optional parameter "trav" can be used to alter this behavior. To immediately start polling this job
for results, simply include the polljob parameters "t" and "out" and optionally "ow"
REQUIRED: "id" "pid" "qid" 
OPTIONAL: "n" "d" "w" "cpu" "trav" ("t" AND "out") "ow"
EXAMPLE: 'createjob id=5 pid=2 qid=3 n=commandjob w=200 cpu=100 trav=r'

--createsubspace -Creates a subspace of an existing space. "id" refers to the id of an existing space.
REQUIRED: "id"
OPTIONAL: "n" "d" "lock" "allPerm" (also all the other permissions parameters)
EXAMPLE: 'createsubspace id=5'

Copy Commands
-------------

These commands can be used to either copy or link primitives from one space to another. To 'copy'
a primitive means to create a deep copy-- you will become the owner of a copied solver or benchmark,
and it will count towards your disk usage. To 'link' a primitive means that you are using a primitive
that still belongs to another user. If they choose to delete a solver or benchmark that you have linked,
then you will lose access to it. You may also copy or link primitives that you own.

--copybench -Copies a benchmark from one space to another. "id" is a benchmark id, and "from" and "to" are
space ids.
REQUIRED: "id" "from" "to"
OPTIONAL: None
EXAMPLE: 'copybench id=42 from=643 to=56"

--linkbench -links an existing benchmark and associates it with a space. "id" is a benchmark id, and "from" and "to" are
space ids.
REQUIRED: "id" "from" "to"
OPTIONAL: None

--copysolver -Copies a solver from one space and places it into a new space or hierarchy. "id" is a solver id, and "from"
and "to" are space ids
REQUIRED: "id" "from" "to"
OPTIONAL: "hier"

--linksolver -links an existing solver and associates it with a new space or hierarchy. "id" is a solver id, and "from"
and "to" are space ids
REQUIRED: "id" "from" "to"
OPTIONAL: "hier"

--copyjob -Copies an existing job and associates it with a space. "id" is a job id, and "from" and "to" are
space ids.
REQUIRED: "id" "from" "to"
OPTIONAL: None

--copyspace -Copies an existing space or space hierarchy rooted at "id" from space "from" to space "to"
REQUIRED: "id" "from", "to"
OPTIONAL: "hier"

--linkuser -links an existing user and associates them with a space. "id" is a benchmark id, and "from" and "to" are
space ids.
REQUIRED: "id" "from" "to"
OPTIONAL: None

List Commands
-------------

These commands will print the contents of a given space to the console. IDs and names will be printed.
Every command in this category requires the "id" parameter, specifying a space. The parameter "limit"
is optional, and indicates that at most "limit" primitives should be printed. An example is given below

'lsjobs id=4 limit=100'

This will print at most 100 jobs present in space 4. The list commands are shown below.

--lsbenchmarks -Lists the IDs and names of all benchmarks in the space.
REQUIRED: "id"
OPTIONAL: None

--lsjobs -Lists the IDs and names of all jobs in the space.
REQUIRED: "id"
OPTIONAL: None

--lssolvers -Lists the IDs and names of all solvers in the space.
REQUIRED: "id"
OPTIONAL: None

--lssubspaces -Lists the IDs and names of all subspaces in the space the current user is authorized to see
REQUIRED: "id"
OPTIONAL: None

--lsusers -Lists the IDs and names of all users in the space.
REQUIRED: "id"
OPTIONAL: None

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