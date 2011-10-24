import random, datetime, sys

# Stores index to word pairs for a dictionary of words
dictionary = {}

# The word 'password' hashed in SHA5 (used when inserting users)
sha5Pass = 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86'

# A list of sql command lists, this is what we store output in before it is dumped to a file
toWrite = [[], [], [], [], [], [], [], [], [], [], [], [], []]

# The different statuses for jobs
stati = ['Finished', 'Running', 'Enqueued', 'Error', 'Timeout']

# Used to randomly do or don't do something
bools = [True, False]

# indicies to the toWrite list, also determines ordering the commands are dumped
pre = 0
users = 1
roles = 2
perm = 3
benchs = 4
solvers = 5
spaces = 6 
jobs = 7
aSet = 8
aBench = 9
aSolver = 10
aUser = 11
aJob = 12

# Load a dictionary into our internal dictionary where each word in the file is on a new line
def loadDictionary(filename):
	i = 0
	
	for line in open(filename, 'r'):
		dictionary[i] = line.strip()
		i += 1

# Gets a random word from the dictionary
def randWord():
	return dictionary[random.randint(1, len(dictionary) - 1)]

# Gives you <length> number of random words joined by spaces
def randStr(length):
	s = ""
	for i in range(length):
		s += randWord() + " "
	
	return s.strip()

# Returns a random job status
def randStatus():
	return random.choice(stati)

# set_assoc table insertion
def insertSetAssoc(parent, child):
	cmd = "INSERT INTO set_assoc VALUES (%d, %d, 0, 1);" % (parent, child)
	toWrite[aSet] += [cmd]

# set_assoc table insertion
def insertUserAssoc(user, space):
	cmd = "INSERT INTO user_assoc VALUES (%d, %d, 1);" % (space, user)
	toWrite[aUser] += [cmd]
	
# bench_assoc table insertion
def insertBenchAssoc(bench, space):
	cmd = "INSERT INTO bench_assoc VALUES (%d, %d);" % (space, bench)
	toWrite[aBench] += [cmd]

# solver_assoc table insertion
def insertSolverAssoc(solver, space):
	cmd = "INSERT INTO solver_assoc VALUES (%d, %d);" % (space, solver)
	toWrite[aSolver] += [cmd]

# job_assoc table insertion
def insertJobAssoc(job, space):
	cmd = "INSERT INTO job_assoc VALUES (%d, %d);" % (space, job)
	toWrite[aJob] += [cmd]
	
# users table insertion
def insertUser(name, institution):
	(fname, lname) = name.split()
	email = "%s-%s@test.starexec.org" % (fname.lower(), lname.lower())
	
	cmd = "INSERT INTO users (email, first_name, last_name, institution, created, password, verified) "
	cmd += "VALUES ('%s', '%s', '%s', '%s', SYSDATE(), '%s', 1);" % (email, fname, lname, institution, sha5Pass)
	toWrite[users] += [cmd]
	
	cmd = "INSERT INTO user_roles VALUES('%s', 'user');" % (email)
	toWrite[roles] += [cmd]
	
# spaces table insertion
def insertSpace(name, desc):
	toWrite[spaces] += ["INSERT INTO spaces(name, created, description, locked, default_permission) VALUES	('%s', SYSDATE(), '%s', 0, 1);" % (name, desc)]

# benchmarks table insertion
def insertBenchmark(name, desc):
	path = "/starexec/test/benchmark/%s.bench" % name 
	userId = random.randint(1, len(toWrite[users]))
	cmd = "INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable) "
	cmd += "VALUES (%d, '%s', SYSDATE(), '%s', '%s', 1);"  % (userId, name, path, desc)
	toWrite[benchs] += [cmd]

# solvers table insertion
def insertSolver(name, desc):
	path = "/starexec/test/solver/%s" % name 
	userId = random.randint(1, len(toWrite[users]))
	cmd = "INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable) "
	cmd += "VALUES (%d, '%s', SYSDATE(), '%s', '%s', 1);"  % (userId, name, path, desc)
	toWrite[solvers] += [cmd]

# jobs table insertion
def insertJob(name, desc, status):
	userId = random.randint(1, len(toWrite[users]))
	cmd = "INSERT INTO jobs (user_id, name, status, description) "
	cmd += "VALUES(%d, '%s', '%s', '%s');" % (userId, name, status, desc)
	toWrite[jobs] += [cmd]

# SQL Commands to be added to the output before randomized data is inserted
# where database is the name of the database to execute all commands for
def doInit(database):
	# Write some warning comments to the output first
	toWrite[pre] += ['-- THIS IS AN AUTO GENERATED FILE.']
	toWrite[pre] += ['-- ANY CHANGES WILL BE REPLACED.']
	toWrite[pre] += ['-- GENERATED ON %s' % (datetime.datetime.now()) ]
	
	# Indicate we're using the give database
	toWrite[pre] += ["\nUSE %s;" % (database)]
	
	# Insert some default users (the starexec team)
	insertUser('Tyler Jensen', 'The University of Iowa')	
	insertUser('Clifton Palmer', 'The University of Iowa')	
	insertUser('Aaron Stump', 'The University of Iowa')	
	insertUser('Cesare Tinelli', 'The University of Iowa')	
	insertUser('Todd Elvers', 'The University of Iowa')	
	insertUser('Skylar Stark', 'The University of Iowa')	
	insertUser('Ruoyu Zhang', 'The University of Iowa')	
	
	# Insert the root space since it will always exist
	insertSpace('root', 'this is the root space, the mother of all spaces')	
	
	# For now, insert a dummy permission that can be referenced by other records
	toWrite[perm] += ["INSERT INTO permissions(add_solver, add_bench, add_user, add_space, remove_solver, remove_bench, remove_user, remove_space, is_leader) VALUES (0, 0, 0, 0, 0, 0, 0, 0, 0);"]
	
# Randomly inserts data into the output. Each type (solver, benchmark, job, user, etc.)
# will have between 1 and <max> random records inserted.
def doRandomizeData(max):

	# For each type, insert records a random number of times
	for i in range(random.randint(1, max)):
		insertUser(randStr(2), randStr(3))
	
	for i in range(random.randint(1, max)):
		insertSolver(randStr(1), randStr(8))	
		
	for i in range(random.randint(1, max)):
		insertBenchmark(randStr(1), randStr(8))	
	
	for i in range(random.randint(1, max)):
		insertSpace(randStr(1), randStr(10))	
	
	for i in range(random.randint(1, max)):
		insertJob(randStr(1), randStr(8), randStatus())	

# Cleans up the randomized associations, guarenteeing each record is unique
# to prevent MySQL from complaining of key violations
def cleanAssoc():
	# For each association command list (keys 8, 9, 10, 11 and 12)
	for i in range(8, 13):
		# Python trick, convert it to a set and back to a list (sets are unique)
		toWrite[i] = list(set(toWrite[i]))
		
# Inserts random associations between each type and a space. Between 1 and <max> random associations
# are inserted per type
def doRandomizeAssoc(solverMax, benchMax, jobMax, userMax, spaceMax):
	# Get the number of spaces for reference (-1 because randint is inclusive)
	numSpaces = len(toWrite[spaces]) - 1
	
	# For the following, we generate random numbers starting at 2, because the 1st space
	# is the root space, and nothing can belong directly to the root space (unless it's another space)
	
	# For each user...
	for i in range(1, len(toWrite[users])):
		for j in range(userMax):
			insertUserAssoc(i, random.randint(2, numSpaces))
	
	# For each solver...
	for i in range(1, len(toWrite[solvers])):
		for j in range(solverMax):
			insertSolverAssoc(i, random.randint(2, numSpaces))
	
	# For each benchmark...
	for i in range(1, len(toWrite[benchs])):
		for j in range(benchMax):
			insertBenchAssoc(i, random.randint(2, numSpaces))
		
	# For each job...
	for i in range(1, len(toWrite[jobs])):
		for j in range(jobMax):
			insertJobAssoc(i, random.randint(2, numSpaces))
		
	# For each space aside from the root...
	for i in range(2, len(toWrite[spaces])):
		# Associate me with a random parent space
		insertSetAssoc(random.randint(1, numSpaces), i)
		
		# If a set can have more than one parent...
		for j in range(spaceMax - 1):						
			# Randomly flip a coin and insert an additional parent
			if(random.choice(bools)):
				insertSetAssoc(random.randint(1, numSpaces), i)
				
	# Now that our associations are generated, remove duplicates
	cleanAssoc()

# Takes all of the commands stored in toWrite list and dumps them to the given file
def dumpFile(filename):
	file = open(filename, 'w')
	
	for i in range(len(toWrite)):
		for line in toWrite[i]:
			file.write(line + '\n')		

# Performs all operations necessary to generate a random schema
# filename is the file it should be dumped to, dbName is the name
# of the database the commands are intended for, maxSize is the
# maximum number of random records of each type to insert
def generateSampleData(filename, dbName, maxSize):
	# Do any setup with the given database name
	doInit(dbName)
	
	# Insert random data
	doRandomizeData(maxSize)
	
	# Insert random associations
	doRandomizeAssoc(1, 1, 1, 1, 3)
	
	# Output to a file
	dumpFile(filename)

if __name__ == "__main__":
	if(len(sys.argv) != 5):
		print '\nWARNING: Invalid number of arguments, default values are being used, please ensure the output file is correct'
		print 'usage: stargen <dictionary filename> <output filename> <database name> <max amount of records per table>\n'
		
		# Load the random word dictionary from the execution directory
		loadDictionary('dictionary.txt')
		
		# Generate sample data with the given filename, database and number of items
		generateSampleData('StarSample.sql', 'starexec', 100)		
	else:
		# Load the random word dictionary from the execution directory
		 loadDictionary(sys.argv[1])
		
		# Generate sample data with the given filename, database and number of items
		 generateSampleData(sys.argv[2], sys.argv[3], int(sys.argv[4]))		