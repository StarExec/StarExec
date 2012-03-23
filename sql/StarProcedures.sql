-- Description: This file contains all stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement






/*************************************************************************
********************* BENCHMARK STORED PROCEDURES ************************
*************************************************************************/
	
-- Adds a benchmark into the system and associates it with a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddBenchmark;
CREATE PROCEDURE AddBenchmark(IN _name VARCHAR(128), IN _path TEXT, IN _downloadable TINYINT(1), IN _userId INT, IN _typeId INT, IN _spaceId INT, IN _diskSize BIGINT, OUT _benchId INT)
	BEGIN	
		INSERT INTO benchmarks (user_id, name, bench_type, uploaded, path, downloadable, disk_size)
		VALUES (_userId, _name, _typeId, SYSDATE(), _path, _downloadable, _diskSize);
		
		SELECT LAST_INSERT_ID() INTO _benchId;		
		INSERT INTO bench_assoc VALUES (_spaceId, _benchId);
	END //	
		
-- Adds a new attribute to a benchmark 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddBenchAttr;
CREATE PROCEDURE AddBenchAttr(IN _benchmarkId INT, IN _key VARCHAR(128), IN _val VARCHAR(128))
	BEGIN
		INSERT INTO bench_attributes VALUES (_benchmarkId, _key, _val);
	END //
	
-- Retrieves all attributes for a benchmark 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetBenchAttrs;
CREATE PROCEDURE GetBenchAttrs(IN _benchmarkId INT)
	BEGIN
		SELECT *
		FROM bench_attributes 
		WHERE bench_id=_benchmarkId
		ORDER BY attr_key ASC;
	END //
	
-- Associates the given benchmark with the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateBench;
CREATE PROCEDURE AssociateBench(IN _benchId INT, IN _spaceId INT)
	BEGIN		
		INSERT IGNORE INTO bench_assoc VALUES (_spaceId, _benchId);
	END //

-- Deletes a benchmark given that benchmark's id
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteBenchmarkById;
CREATE PROCEDURE DeleteBenchmarkById(IN _benchmarkId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM benchmarks WHERE id = _benchmarkId;
		DELETE FROM benchmarks
		WHERE id = _benchmarkId;
	END //	
	
-- Retrieves the benchmark with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetBenchmarkById;
CREATE PROCEDURE GetBenchmarkById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN processors AS types
			ON bench.bench_type=types.id
		WHERE bench.id = _id;
	END //
	
-- Retrieves all benchmarks owned by a given user id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetBenchmarksByOwner;
CREATE PROCEDURE GetBenchmarksByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM benchmarks
		WHERE user_id = _userId;
	END //
	
	
-- Retrieves all benchmarks belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceBenchmarksById;
CREATE PROCEDURE GetSpaceBenchmarksById(IN _id INT)
	BEGIN
		SELECT *
		FROM benchmarks AS bench
			LEFT OUTER JOIN processors AS types
			ON bench.bench_type=types.id
		WHERE bench.id IN
				(SELECT bench_id
				FROM bench_assoc
				WHERE space_id = _id)
		ORDER BY bench.name;
	END //
	
-- Removes the association between a benchmark and a given space;
-- places the path of the benchmark in _path if it has no other
-- associations in bench_assoc, otherwise places NULL in _path
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveBenchFromSpace;
CREATE PROCEDURE RemoveBenchFromSpace(IN _benchId INT, IN _spaceId INT, OUT _path TEXT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM bench_assoc
			WHERE space_id = _spaceId
			AND bench_id = _benchId;
		END IF;
		
		IF NOT EXISTS (SELECT * FROM bench_assoc WHERE bench_id = _benchId) THEN
			IF NOT EXISTS (SELECT * FROM job_pairs WHERE bench_id = _benchId) THEN
				SELECT path INTO _path FROM benchmarks WHERE id = _benchId;
				DELETE FROM benchmarks
				WHERE id = _benchId;
			ELSE
				SELECT NULL INTO _path;
			END IF;
		ELSE
			SELECT NULL INTO _path;
		END IF;
	END //
	
-- Updates the details associated with a given benchmark
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateBenchmarkDetails;
CREATE PROCEDURE UpdateBenchmarkDetails(IN _benchmarkId INT, IN _name VARCHAR(32), IN _description TEXT, IN _downloadable BOOLEAN, IN _type INT)
	BEGIN
		UPDATE benchmarks
		SET name = _name,
		description = _description,
		downloadable = _downloadable,
		bench_type = _type
		WHERE id = _benchmarkId;
	END //
	
	

	
	
	

/*************************************************************************
********************* PROCESSOR STORED PROCEDURES ***********************
*************************************************************************/

-- Adds a new processor with the given information
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddProcessor;
CREATE PROCEDURE AddProcessor(IN _name VARCHAR(32), IN _desc TEXT, IN _path TEXT, IN _comId INT, IN _type TINYINT, IN _diskSize BIGINT)
	BEGIN		
		INSERT INTO processors (name, description, path, community, processor_type, disk_size)
		VALUES (_name, _desc, _path, _comId, _type, _diskSize);
	END //
	
-- Removes the association between a processor and a given space,
-- and inserts the processor_path into _path, so the physical file(s) can
-- be removed from disk
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeleteProcessor;
CREATE PROCEDURE DeleteProcessor(IN _id INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM processors WHERE id = _id;
		DELETE FROM processors
		WHERE id = _id;
	END //
	
-- Gets all processors of a given type
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetAllProcessors;
CREATE PROCEDURE GetAllProcessors(IN _type TINYINT)
	BEGIN		
		SELECT *
		FROM processors
		WHERE processor_type=_type
		ORDER BY name;
	END //
	
-- Retrieves all processor belonging to a community
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetProcessorsByCommunity;
CREATE PROCEDURE GetProcessorsByCommunity(IN _id INT, IN _type TINYINT)
	BEGIN
		SELECT *
		FROM processors
		WHERE community=_id AND processor_type=_type
		ORDER BY name;
	END //
	
-- Gets the processor with the given ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetProcessorById;
CREATE PROCEDURE GetProcessorById(IN _id INT)
	BEGIN		
		SELECT *
		FROM processors
		WHERE id=_id;
	END //
	
-- Updates a processor's description
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorDescription;
CREATE PROCEDURE UpdateProcessorDescription(IN _id INT, IN _desc TEXT)
	BEGIN		
		UPDATE processors
		SET description=_desc
		WHERE id=_id;
	END //
	
-- Updates a processor's name
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorName;
CREATE PROCEDURE UpdateProcessorName(IN _id INT, IN _name VARCHAR(32))
	BEGIN		
		UPDATE processors
		SET name=_name
		WHERE id=_id;
	END //
	
-- Updates a processor's processor path
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateProcessorPath;
CREATE PROCEDURE UpdateProcessorPath(IN _id INT, IN _path TEXT, IN _diskSize BIGINT)
	BEGIN		
		UPDATE processors
		SET path=_path,
			disk_size=_diskSize
		WHERE id=_id;
	END //
	
	

	
	
	

/*************************************************************************
********************** CLUSTER STORED PROCEDURES *************************
*************************************************************************/

-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateQueue;
CREATE PROCEDURE AssociateQueue(IN _queueName VARCHAR(64), IN _nodeName VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queue_assoc
		VALUES(
			(SELECT id FROM queues WHERE name=_queueName), 
			(SELECT id FROM nodes WHERE name=_nodeName));
	END //
	
-- Adds a worker node to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddNode;
CREATE PROCEDURE AddNode(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO nodes (name)
		VALUES (_name);
	END //
	
-- Adds a SGE queue to the database and ignores duplicates
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddQueue;
CREATE PROCEDURE AddQueue(IN _name VARCHAR(64))
	BEGIN
		INSERT IGNORE INTO queues (name)
		VALUES (_name);
	END //
	
-- Gets the id, name and status of all nodes in the cluster
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetAllNodes;
CREATE PROCEDURE GetAllNodes()
	BEGIN		
		SELECT id, name, status
		FROM nodes
		ORDER BY name;	
	END //
	
-- Gets the id, name and status of all nodes in the cluster that are active
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetNodesForQueue;
CREATE PROCEDURE GetNodesForQueue(IN _id INT)
	BEGIN		
		SELECT id, name, status
		FROM nodes
		WHERE id IN
			(SELECT node_id FROM queue_assoc WHERE queue_id=_id)
		ORDER BY name;	
	END //

-- Gets the id, name and status of all queues in the cluster that are active
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetAllQueues;
CREATE PROCEDURE GetAllQueues()
	BEGIN		
		SELECT id, name, status
		FROM queues
		WHERE status="ACTIVE"
		ORDER BY name;	
	END //
	
-- Gets worker node with the given ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetNodeDetails;
CREATE PROCEDURE GetNodeDetails(IN _id INT)
	BEGIN		
		SELECT *
		FROM nodes
		WHERE id=_id;
	END // 
	
-- Gets the queue with the given ID (excluding SGE attributes)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetQueue;
CREATE PROCEDURE GetQueue(IN _id INT)
	BEGIN		
		SELECT id, name, status, slots_used, slots_reserved, slots_free, slots_total
		FROM queues
		WHERE id=_id;
	END // 
	
-- Gets the queue with the given ID (includes all SGE attributes)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetQueueDetails;
CREATE PROCEDURE GetQueueDetails(IN _id INT)
	BEGIN		
		SELECT *
		FROM queues
		WHERE id=_id;
	END // 
	
-- Updates a node's attribute (assuming the column already exists)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateNodeAttr;
CREATE PROCEDURE UpdateNodeAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE nodes SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's attribute (assuming the column already exists)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueAttr;
CREATE PROCEDURE UpdateQueueAttr(IN _name VARCHAR(64), IN _fieldName VARCHAR(64), IN _fieldVal VARCHAR(64))
	BEGIN	
		SET @updateAttr = CONCAT('UPDATE queues SET ', _fieldName, '="', _fieldVal,'" WHERE name="', _name, '"');
		PREPARE stmt FROM @updateAttr;
		EXECUTE stmt;		
	END // 
	
-- Updates a queues's usage stats
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueUseage;
CREATE PROCEDURE UpdateQueueUseage(IN _name VARCHAR(64), IN _total INTEGER, IN _free INTEGER, IN _used INTEGER, IN _reserved INTEGER)
	BEGIN	
		UPDATE queues
		SET slots_total=_total, slots_free=_free, slots_used=_used, slots_reserved=_reserved
		WHERE name=_name;
	END // 

-- Updates all queues status'
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateAllQueueStatus;
CREATE PROCEDURE UpdateAllQueueStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status;
	END // 
	
-- Updates a specific queues status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateQueueStatus;
CREATE PROCEDURE UpdateQueueStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE queues
		SET status=_status
		WHERE name=_name;
	END // 

-- Updates all nodes status'
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateAllNodeStatus;
CREATE PROCEDURE UpdateAllNodeStatus(IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status;
	END // 
	
-- Updates a specific node's status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateNodeStatus;
CREATE PROCEDURE UpdateNodeStatus(IN _name VARCHAR(64), IN _status VARCHAR(32))
	BEGIN	
		UPDATE nodes
		SET status=_status
		WHERE name=_name;
	END // 

	
	
	
	
	
/*************************************************************************
********************* COMMUNITY STORED PROCEDURES ************************
*************************************************************************/

-- Returns basic space information for the community with the given id
-- This ensures security by preventing malicious users from getting details about ANY space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetCommunityById;
CREATE PROCEDURE GetCommunityById(IN _id INT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
			(SELECT child_id
			 FROM set_assoc
			 WHERE space_id=1
			 AND child_id = _id);
	END //

-- Removes the association a user has with a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS LeaveCommunity;
CREATE PROCEDURE LeaveCommunity(IN _userId INT, IN _commId INT)
	BEGIN
		-- Remove the permission associated with this user/community
		DELETE FROM permissions
			WHERE id=(SELECT permission FROM user_assoc WHERE user_id = _userId	AND space_id = _commId);
		
		-- Delete the association	
		DELETE FROM user_assoc
		WHERE user_id = _userId
		AND space_id = _commId;
	END //
	
	
	
	
	

/*************************************************************************
************************ JOB STORED PROCEDURES ***************************
*************************************************************************/

-- Adds an association between the given job and space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateJob;
CREATE PROCEDURE AssociateJob(IN _jobId INT, IN _spaceId INT)
	BEGIN
		INSERT IGNORE INTO job_assoc VALUES (_spaceId, _jobId);
	END //
	
	
-- Adds a new attribute to a job pair 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobAttr;
CREATE PROCEDURE AddJobAttr(IN _pairId INT, IN _key VARCHAR(128), IN _val VARCHAR(128))
	BEGIN
		INSERT INTO job_attributes VALUES (_pairId, _key, _val);
	END //
	
-- Retrieves all attributes for a job pair 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetPairAttrs;
CREATE PROCEDURE GetPairAttrs(IN _pairId INT)
	BEGIN
		SELECT *
		FROM job_attributes 
		WHERE pair_id=_pairId
		ORDER BY attr_key ASC;
	END //
	
-- Retrieves simple overall statistics for job pairs belonging to a job
-- Including the total number of pairs, how many are complete, pending or errored out
-- as well as how long the pairs ran
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairOverview;
CREATE PROCEDURE GetJobPairOverview(IN _jobId INT)
	BEGIN
		-- This is messy in order to get back pretty column names.
		-- Derived tables must have identifiers which is why a, b, c, d and e exist but aren't used
		SELECT * FROM (
			(SELECT COUNT(*) AS totalPairs FROM job_pairs WHERE job_id=_jobId) AS a, -- Gets the total number of pairs
			(SELECT COUNT(*) AS completePairs FROM job_pairs WHERE job_id=_jobId AND status_code=7) AS b, -- Gets number of pairs with COMPLETE status codes
			(SELECT COUNT(*) AS pendingPairs FROM job_pairs WHERE job_id=_jobId AND (status_code BETWEEN 1 AND 6)) AS c, -- Gets number of pairs with non complete and non error status codes
			(SELECT COUNT(*) AS errorPairs FROM job_pairs WHERE job_id=_jobId AND (status_code BETWEEN 8 AND 17 OR status_code=0)) AS d, -- Gets number of UNKNOWN or ERROR status code pairs
			(SELECT TIMESTAMPDIFF( -- Gets time difference between earliest completed pair's start time and latest completed pair's end time
				MICROSECOND, 
				(SELECT MIN(start_time) FROM job_pairs WHERE job_id=_jobId AND status_code=7),
				(SELECT MAX(end_time) FROM job_pairs WHERE job_id=_jobId AND status_code=7)) AS runtime) AS e);
	END //
	
-- Retrieves basic info about a job from the jobs table
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobById;
CREATE PROCEDURE GetJobById(IN _id INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id = _id;
	END //
	
-- Retrieves basic info about job pairs for the given job id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairsByJob;
CREATE PROCEDURE GetJobPairsByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
		WHERE job_pairs.job_id=_id
		ORDER BY job_pairs.end_time DESC;
	END //

-- Gets the job pair associated with the given sge id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairBySgeId;
CREATE PROCEDURE GetJobPairBySgeId(IN _sgeId INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE sge_id=_sgeId;
	END //
	
-- Gets the job pair with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairById;
CREATE PROCEDURE GetJobPairById(IN _Id INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
		WHERE job_pairs.id=_Id;
	END //
	
-- Gets the job pair with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairBySGE;
CREATE PROCEDURE GetJobPairBySGE(IN _Id INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
		WHERE job_pairs.sge_id=_Id;
	END //
	
-- Removes the association between a job and a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveJobFromSpace;
CREATE PROCEDURE RemoveJobFromSpace(IN _jobId INT, IN _spaceId INT)
BEGIN
	DELETE FROM job_assoc
	WHERE job_id = _jobId
	AND space_id = _spaceId;
END //
	
-- Retrieves all jobs belonging to a space (but not their job pairs)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceJobsById;
CREATE PROCEDURE GetSpaceJobsById(IN _spaceId INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id IN
			(SELECT job_id
			 FROM job_assoc
			 WHERE space_id=_spaceId) 
		ORDER BY created DESC;
	END //

-- Adds a new job pair record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobPair;
CREATE PROCEDURE AddJobPair(IN _jobId INT, IN _benchId INT, IN _configId INT, IN _status TINYINT, IN _cpuTimeout INT, IN _clockTimeout INT, OUT _id INT)
	BEGIN
		INSERT INTO job_pairs (job_id, bench_id, config_id, status_code, cpuTimeout, clockTimeout)
		VALUES (_jobId, _benchId, _configId, _status, _cpuTimeout, _clockTimeout);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds a new job record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJob;
CREATE PROCEDURE AddJob(IN _userId INT, IN _name VARCHAR(32), IN _desc TEXT, IN _queueId INT, IN _preProcessor INT, IN _postProcessor INT, OUT _id INT)
	BEGIN
		INSERT INTO jobs (user_id, name, description, queue_id, pre_processor, post_processor)
		VALUES (_userId, _name, _desc, _queueId, _preProcessor, _postProcessor);
		SELECT LAST_INSERT_ID() INTO _id;
	END //
	
-- Updates a job pair's sge id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS SetSGEJobId;
CREATE PROCEDURE SetSGEJobId(IN _jobPairId INT, IN _sgeId INT)
	BEGIN
		UPDATE job_pairs
		SET sge_id=_sgeId
		WHERE id=_jobPairId;
	END //
	
-- Gets all SGE ids that have a certain status code
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSGEIdsByStatus;
CREATE PROCEDURE GetSGEIdsByStatus(IN _statusCode TINYINT)
	BEGIN
		SELECT sge_id
		FROM job_pairs
		WHERE status_code=_statusCode;
	END //	
	
-- Updates a job pair's status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdatePairStatus;
CREATE PROCEDURE UpdatePairStatus(IN _jobPairId INT, IN _statusCode TINYINT)
	BEGIN
		UPDATE job_pairs
		SET status_code=_statusCode
		WHERE id=_jobPairId;
	END //
	
-- Updates a job pair's status given its sge id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateSGEPairStatus;
CREATE PROCEDURE UpdateSGEPairStatus(IN _sgeId INT, IN _statusCode TINYINT)
	BEGIN
		UPDATE job_pairs
		SET status_code=_statusCode
		WHERE sge_id=_sgeId;
	END //		

-- Updates a job pair's statistics (used by the job epilog script)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdatePairStats;
CREATE PROCEDURE UpdatePairStats(IN _sgeId INT, IN _nodeName VARCHAR(64), IN _queuesubTime TIMESTAMP, IN _startTime TIMESTAMP, IN _endTime TIMESTAMP, IN _exitStatus INT, IN _cpu DOUBLE, IN _userTime DOUBLE, IN _systemTime DOUBLE, IN _ioData DOUBLE, IN _ioWait DOUBLE, IN _memUsage DOUBLE, IN _maxVmem DOUBLE, IN _maxResSet BIGINT, IN _pageReclaims BIGINT, IN _pageFaults BIGINT, IN _blockInput BIGINT, IN _blockOutput BIGINT, IN _volContexSwtch BIGINT, IN _involContexSwtch BIGINT)
	BEGIN
		UPDATE job_pairs
		SET node_id=(SELECT id FROM nodes WHERE name=_nodeName),
			queuesub_time=_queuesubTime,
			start_time=_startTime,
			end_time=_endTime,
			exit_status=_exitStatus,
			wallclock=TIMESTAMPDIFF(MICROSECOND , _startTime, _endTime),
			cpu=_cpu,
			user_time=_userTime,
			system_time=_systemTime,
			io_data=_ioData,
			io_wait=_ioWait,
			mem_usage=_memUsage,
			max_vmem=_maxVmem,
			max_res_set=_maxResSet,
			page_reclaims=_pageReclaims,
			page_faults=_pageFaults,
			block_input=_blockInput,
			block_output=_blockOutput,
			vol_contex_swtch=_volContexSwtch,
			invol_contex_swtch=_involContexSwtch
		WHERE sge_id=_sgeId;
	END //

	
	
	
	
	
	
/*************************************************************************
*********************** MISC STORED PROCEDURES ***************************
*************************************************************************/

-- Adds a new column to the specified table only if it doesn't already exist.
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddColumnUnlessExists;
CREATE PROCEDURE AddColumnUnlessExists(IN dbName tinytext, IN tableName tinytext, IN fieldName tinytext, IN fieldDef text)
	BEGIN
		IF NOT EXISTS 
			(SELECT * FROM information_schema.COLUMNS
			WHERE column_name=fieldName
			AND table_name=tableName
			AND table_schema=dbName)
		THEN
			SET @addColumn = CONCAT('ALTER TABLE ', dbName, '.', tableName,
			' ADD COLUMN ', fieldName, ' ', fieldDef);
			PREPARE stmt FROM @addColumn;
			EXECUTE stmt;
		END IF;
	END //

-- Adds a new historical record to the logins table which tracks all user logins
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS LoginRecord;
CREATE PROCEDURE LoginRecord(IN _userId INT, IN _ipAddress VARCHAR(15), IN _agent TEXT)
	BEGIN		
		INSERT INTO logins (user_id, login_date, ip_address, browser_agent)
		VALUES (_userId, SYSDATE(), _ipAddress, _agent);
	END //







/*************************************************************************
********************* PERMISSION STORED PROCEDURES ***********************
*************************************************************************/

-- Adds a new permissions record with the given permissions
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddPermissions;
CREATE PROCEDURE AddPermissions(IN _addSolver TINYINT(1), IN _addBench TINYINT(1), IN _addUser TINYINT(1), 
IN _addSpace TINYINT(1), IN _addJob TINYINT(1), IN _removeSolver TINYINT(1), IN _removeBench TINYINT(1), IN _removeSpace TINYINT(1), 
IN _removeUser TINYINT(1), IN _removeJob TINYINT(1), IN _isLeader TINYINT(1), OUT id INT)
	BEGIN		
		INSERT INTO permissions 
			(add_solver, add_bench, add_user, add_space, add_job, remove_solver, 
			remove_bench, remove_space, remove_user, remove_job, is_leader)
		VALUES
			(_addSolver, _addBench, _addUser, _addSpace, _addJob, _removeSolver, 
			_removeBench, _removeSpace, _removeUser, _removeJob, _isLeader);	
		SELECT LAST_INSERT_ID() INTO id;
	END //
	
-- Returns 1 if the given user can somehow see the given solver, 0 otherwise
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CanViewSolver;
CREATE PROCEDURE CanViewSolver(IN _solverId INT, IN _userId INT)
	BEGIN		
		SELECT IF((				
			SELECT COUNT(*)
			FROM solvers JOIN solver_assoc ON solvers.id=solver_assoc.solver_id -- Get all solvers and find its association to spaces
			JOIN spaces ON solver_assoc.space_id=spaces.id						-- Join on spaces to get all the spaces the solver belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id					-- Join on user_assoc to get all the users that belong to those spaces
			WHERE solvers.id=_solverId AND user_assoc.user_id=_userId)			-- But only count those for the solver and user we're looking for
		> 0, 1, 0) AS verified; 												-- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

	-- Returns 1 if the given user can somehow see the given benchmark, 0 otherwise
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CanViewBenchmark;
CREATE PROCEDURE CanViewBenchmark(IN _benchId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM benchmarks JOIN bench_assoc ON benchmarks.id=bench_assoc.bench_id  -- Get all benchmarks and find its association to spaces
			JOIN spaces ON bench_assoc.space_id=spaces.id                           -- Join on spaces to get all the spaces the benchmark belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id                        -- Join on user_assoc to get all the users that belong to those spaces
			WHERE benchmarks.id=_benchId AND user_assoc.user_id=_userId)            -- But only count those for the benchmark and user we're looking for
		> 0, 1, 0) AS verified; 												    -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

-- Returns 1 if the given user can somehow see the given job, 0 otherwise
-- Author: Tyler Jensen	
DROP PROCEDURE IF EXISTS CanViewJob;
CREATE PROCEDURE CanViewJob(IN _jobId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM jobs JOIN job_assoc ON jobs.id=job_assoc.job_id      -- Get all jobs and find its association to spaces
			JOIN spaces ON job_assoc.space_id=spaces.id               -- Join on spaces to get all the spaces the job belongs to
			JOIN user_assoc ON user_assoc.space_id=spaces.id          -- Join on user_assoc to get all the users that belong to those spaces
			WHERE jobs.id=_jobId AND user_assoc.user_id=_userId)      -- But only count those for the job and user we're looking for
		> 0, 1, 0) AS verified;                                       -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //

-- Returns 1 if the given user can somehow see the given space, 0 otherwise
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CanViewSpace;
CREATE PROCEDURE CanViewSpace(IN _spaceId INT, IN _userId INT)
	BEGIN		
		SELECT IF((
			SELECT COUNT(*)
			FROM user_assoc 
			WHERE space_id=_spaceId AND user_id=_userId)	-- Directly find how many times the user belongs to a space
		> 0, 1, 0) AS verified;                             -- If there were more than 0 results, return 1, else return 0, and return under the name 'verified'
	END //
	
-- Finds the maximal set of permissions for the given user on the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserPermissions;
CREATE PROCEDURE GetUserPermissions(IN _userId INT, IN _spaceId INT)
	BEGIN		
		SELECT MAX(add_solver) AS add_solver, 
			MAX(add_bench) AS add_bench, 
			MAX(add_user) AS add_user, 
			MAX(add_space) AS add_space, 
			MAX(add_job) AS add_job,
			MAX(remove_solver) AS remove_solver, 
			MAX(remove_bench) AS remove_bench, 
			MAX(remove_space) AS remove_space, 
			MAX(remove_user) AS remove_user,
			MAX(remove_job) AS remove_job,
			MAX(is_leader) AS is_leader
		FROM permissions JOIN user_assoc ON user_assoc.permission=permissions.id
		WHERE user_assoc.user_id=_userId AND user_assoc.space_id=_spaceId;
	END //
	
-- Finds the default user permissions for the given space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpacePermissions;
CREATE PROCEDURE GetSpacePermissions(IN _spaceId INT)
	BEGIN		
		SELECT permissions.*
		FROM permissions JOIN spaces ON spaces.default_permission=permissions.id
		WHERE spaces.id=_spaceId;
	END //	
	
-- Copies one set of permissions into another with a new ID
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS CopyPermissions;
CREATE PROCEDURE CopyPermissions(IN _permId INT, OUT _newId INT)
	BEGIN
		INSERT INTO permissions (add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader)
		(SELECT add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader
		FROM permissions
		WHERE id=_permId);
		SELECT LAST_INSERT_ID() INTO _newId;
	END //	
	

-- Sets a user's permissions for a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS SetUserPermissions;
CREATE PROCEDURE SetUserPermissions(IN _userId INT, IN _spaceId INT,IN _addSolver TINYINT(1), IN _addBench TINYINT(1), IN _addUser TINYINT(1), 
IN _addSpace TINYINT(1), IN _addJob TINYINT(1), IN _removeSolver TINYINT(1), IN _removeBench TINYINT(1), IN _removeSpace TINYINT(1), 
IN _removeUser TINYINT(1), IN _removeJob TINYINT(1), IN _isLeader TINYINT(1))
	BEGIN
		UPDATE permissions JOIN user_assoc ON permissions.id=user_assoc.permission
		SET add_user      = _addUser,
			add_solver    = _addSolver, 
			add_bench     = _addBench,
			add_job       = _addJob,
			add_space     = _addSpace,
			remove_user   = _removeUser,
			remove_solver = _removeSolver,
			remove_bench  = _removeBench,
			remove_job    = _removeJob,
			remove_space  = _removeSpace,
			is_leader     = _isLeader			
		WHERE user_id = _userId
		AND space_id = _spaceId;
	END //
	
-- Updates the permission set with the given id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdatePermissions;
CREATE PROCEDURE UpdatePermissions(IN _id INT, IN _addSolver BOOLEAN, IN _addBench BOOLEAN, IN _addUser BOOLEAN, 
IN _addSpace BOOLEAN, IN _addJob BOOLEAN, IN _removeSolver BOOLEAN, IN _removeBench BOOLEAN, IN _removeSpace BOOLEAN,
IN _removeUser BOOLEAN, IN _removeJob BOOLEAN)
	BEGIN
		UPDATE permissions
		SET add_user      = _addUser,
			add_solver    = _addSolver, 
			add_bench     = _addBench,
			add_job       = _addJob,
			add_space     = _addSpace,
			remove_user   = _removeUser,
			remove_solver = _removeSolver,
			remove_bench  = _removeBench,
			remove_job    = _removeJob,
			remove_space  = _removeSpace
		WHERE id = _id;
	END //
	
/*************************************************************************
********************** REQUEST STORED PROCEDURES *************************
*************************************************************************/

-- Adds an activation code for a specific user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCode;
CREATE PROCEDURE AddCode(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		INSERT INTO verify(user_id, code, created)
		VALUES (_id, _code, SYSDATE());
	END //
	
-- Adds a request to join a community, provided the user isn't already a part of that community
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddCommunityRequest;
CREATE PROCEDURE AddCommunityRequest(IN _id INT, IN _community INT, IN _code VARCHAR(36), IN _message VARCHAR(300))
	BEGIN
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _id AND space_id = _community) THEN
			INSERT INTO community_requests(user_id, community, code, message, created)
			VALUES (_id, _community, _code, _message, SYSDATE());
		END IF;
	END //
	
-- Adds a user to USER_ASSOC, USER_ROLES and deletes their entry in INVITES
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS ApproveCommunityRequest;
CREATE PROCEDURE ApproveCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DECLARE _newPermId INT;
		DECLARE _pid INT;	
		
		IF EXISTS(SELECT * FROM community_requests WHERE user_id = _id AND community = _community) THEN
			DELETE FROM community_requests 
			WHERE user_id = _id and community = _community;			
			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_community INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc(user_id, space_id, proxy, permission)
			VALUES(_id, _community, _community, _newPermId);
		END IF;
		
		IF NOT EXISTS(SELECT email FROM user_roles WHERE email = (SELECT email FROM users WHERE users.id = _id)) THEN
			INSERT INTO user_roles(email, role)
				VALUES((SELECT email FROM users WHERE users.id = _id), 'user');
		END IF;
	END //
	
-- Adds a new entry to pass_reset_request for a given user (also deletes previous
-- entries for the same user)
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddPassResetRequest;
CREATE PROCEDURE AddPassResetRequest(IN _id INT, IN _code VARCHAR(36))
	BEGIN
		IF EXISTS(SELECT * FROM pass_reset_request WHERE user_id = _id) THEN
			DELETE FROM pass_reset_request
			WHERE user_id = _id;
		END IF;
		INSERT INTO pass_reset_request(user_id, code, created)
		VALUES(_id, _code, SYSDATE());
	END //
	
-- Deletes a user's entry in INVITES, and if the user is unregistered
-- (i.e. doesn't have an entry in USER_ROLES) then they are completely
-- deleted from the system
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeclineCommunityRequest;
CREATE PROCEDURE DeclineCommunityRequest(IN _id INT, IN _community INT)
	BEGIN
		DELETE FROM community_requests 
		WHERE user_id = _id and community = _community;
		DELETE FROM users
		WHERE users.id = _id
		AND users.email NOT IN
			(SELECT email
			FROM user_roles);
	END //
	
-- Returns the community request associated with given user id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestById;
CREATE PROCEDURE GetCommunityRequestById(IN _id INT)
	BEGIN
		SELECT *
		FROM community_requests
		WHERE user_id = _id;
	END //
	
-- Returns the community request associated with the given activation code
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetCommunityRequestByCode;
CREATE PROCEDURE GetCommunityRequestByCode(IN _code VARCHAR(36))
	BEGIN
		SELECT *
		FROM community_requests
		WHERE code = _code;
	END //

-- Looks for an activation code, and if successful, removes it from VERIFY,
-- then adds an entry to USER_ROLES
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RedeemActivationCode;
CREATE PROCEDURE RedeemActivationCode(IN _code VARCHAR(36), OUT _id INT)
	BEGIN
		IF EXISTS(SELECT _code FROM verify WHERE code = _code) THEN
			SELECT user_id INTO _id 
			FROM verify
			WHERE code = _code;
			
			DELETE FROM verify
			WHERE code = _code;
		END IF;
	END // 

-- Redeems a given password reset code by deleting the corresponding entry
-- in pass_reset_request and returning the user_id of that deleted entry
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RedeemPassResetRequestByCode;
CREATE PROCEDURE RedeemPassResetRequestByCode(IN _code VARCHAR(36), OUT _id INT)
	BEGIN
		SELECT user_id INTO _id
		FROM pass_reset_request
		WHERE code = _code;
		DELETE FROM pass_reset_request
		WHERE code = _code;
	END //
	
	
	
	
	

/*************************************************************************
********************** SOLVER STORED PROCEDURES **************************
*************************************************************************/

-- Adds a solver and returns the solver ID
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolver;
CREATE PROCEDURE AddSolver(IN _userId INT, IN _name VARCHAR(32), IN _downloadable BOOLEAN, IN _path TEXT, IN _description TEXT, OUT _id INT, IN _diskSize BIGINT)
	BEGIN
		INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size)
		VALUES (_userId, _name, SYSDATE(), _path, _description, _downloadable, _diskSize);
		
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds a Space/Solver association
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolverAssociation;
CREATE PROCEDURE AddSolverAssociation(IN _spaceId INT, IN _solverId INT)
	BEGIN
		INSERT IGNORE INTO solver_assoc VALUES (_spaceId, _solverId);
	END // 
	
-- Adds a run configuration to the specified solver
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddConfiguration;
CREATE PROCEDURE AddConfiguration(IN _solverId INT, IN _name VARCHAR(64), OUT configId INT)
	BEGIN
		INSERT INTO configurations (solver_id, name)
		VALUES (_solverId, _name);
		
		SELECT LAST_INSERT_ID() INTO configId;
	END //
	

-- Deletes a configuration given that configuration's id
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteConfigurationById;
CREATE PROCEDURE DeleteConfigurationById(IN _configId INT)
	BEGIN
		DELETE FROM configurations
		WHERE id = _configId;
	END //	
	
	
-- Deletes a solver given that solver's id
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteSolverById;
CREATE PROCEDURE DeleteSolverById(IN _solverId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM solvers WHERE id = _solverId;
		DELETE FROM solvers
		WHERE id = _solverId;
	END //	
	
-- Retrieves the configurations that belong to a solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfigsForSolver;
CREATE PROCEDURE GetConfigsForSolver(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE solver_id = _id;
	END //
	
-- Retrieves all solvers belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceSolversById;
CREATE PROCEDURE GetSpaceSolversById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id IN
				(SELECT solver_id
				FROM solver_assoc
				WHERE space_id = _id)
		ORDER BY name;
	END //	
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverById;
CREATE PROCEDURE GetSolverById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id;
	END //

-- Retrieves the solvers owned by a given user id
-- Todd Elvers
DROP PROCEDURE IF EXISTS GetSolversByOwner;
CREATE PROCEDURE GetSolversByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE user_id = _userId;
	END //
	
-- Retrieves the configurations that belong to a solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfigsForSolver;
CREATE PROCEDURE GetConfigsForSolver(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE solver_id = _id;
	END //
	
-- Retrieves the configurations with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfiguration;
CREATE PROCEDURE GetConfiguration(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE id = _id;
	END //
	
-- Removes the association between a solver and a given space;
-- places the path of the solver in _path if it has no other
-- associations in solver_assoc and isn't being used for any jobs,
-- otherwise places NULL in _path
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveSolverFromSpace;
CREATE PROCEDURE RemoveSolverFromSpace(IN _solverId INT, IN _spaceId INT, OUT _path TEXT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM solver_assoc
			WHERE solver_id = _solverId
			AND space_id = _spaceId;
		END IF;
		
		-- Ensure the solver isn't being used in any other space
		IF NOT EXISTS(SELECT * FROM solver_assoc WHERE solver_id =_solverId) THEN
			-- Ensure the solver isn't being used for any other jobs
			IF NOT EXISTS(SELECT * FROM job_pairs JOIN configurations ON job_pairs.config_id=configurations.id WHERE solver_id=_solverId) THEN
				SELECT path INTO _path FROM solvers WHERE id = _solverId;
				DELETE FROM solvers
				WHERE id = _solverId;
			ELSE
				SELECT NULL INTO _path;
			END IF;
		ELSE
			SELECT NULL INTO _path;
		END IF;
			
	END // 
	
-- Updates the disk_size attribute of a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDiskSize;
CREATE PROCEDURE UpdateSolverDiskSize(IN _solverId INT, IN _newDiskSize BIGINT)
	BEGIN
		UPDATE solvers
		SET disk_size = _newDiskSize
		WHERE id = _solverId;
	END //
	
	
-- Updates the details associated with a given configuration
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateConfigurationDetails;
CREATE PROCEDURE UpdateConfigurationDetails(IN _configId INT, IN _name VARCHAR(64), IN _description TEXT)
	BEGIN
		UPDATE configurations
		SET name = _name,
		description = _description
		WHERE id = _configId;
	END //
	
	
-- Updates the details associated with a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDetails;
CREATE PROCEDURE UpdateSolverDetails(IN _solverId INT, IN _name VARCHAR(32), IN _description TEXT, IN _downloadable BOOLEAN)
	BEGIN
		UPDATE solvers
		SET name = _name,
		description = _description,
		downloadable = _downloadable
		WHERE id = _solverId;
	END //
	
	
	
	

	
/*************************************************************************
*********************** SPACE STORED PROCEDURES **************************
*************************************************************************/

-- Adds a new space with the given information
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddSpace;
CREATE PROCEDURE AddSpace(IN _name VARCHAR(32), IN _desc TEXT, IN _locked TINYINT(1), IN _permission INT, IN _parent INT, OUT id INT)
	BEGIN		
		INSERT INTO spaces (name, created, description, locked, default_permission)
		VALUES (_name, SYSDATE(), _desc, _locked, _permission);
		SELECT LAST_INSERT_ID() INTO id;
		INSERT INTO closure (ancestor, descendant)	-- Update closure table
			SELECT ancestor, id FROM closure
			WHERE descendant = _parent
			UNION ALL SELECT _parent, id UNION SELECT id, id; 
	END //

-- Adds an association between two spaces
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateSpaces;
CREATE PROCEDURE AssociateSpaces(IN _parentId INT, IN _childId INT)
	BEGIN		
		INSERT IGNORE INTO set_assoc
		VALUES (_parentId, _childId);
	END //
	
-- Gets all the descendants of a space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetDescendantsOfSpace;
CREATE PROCEDURE GetDescendantsOfSpace(IN _spaceId INT)
	BEGIN
		SELECT descendant
		FROM closure
		WHERE ancestor = _spaceId AND NOT descendant=_spaceId;
	END //
	
-- Gets all the leaders of a space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetLeadersBySpaceId;
CREATE PROCEDURE GetLeadersBySpaceId(IN _id INT)
	BEGIN
		SELECT *
		FROM users
		WHERE email IN
			(SELECT DISTINCT users.email
			FROM spaces
			JOIN user_assoc ON spaces.id=user_assoc.space_id
			JOIN users ON user_assoc.user_id=users.id
			JOIN permissions ON user_assoc.permission=permissions.id
			WHERE spaces.id=_id AND permissions.is_leader=1);
	END //
	
-- Returns basic space information for the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceById;
CREATE PROCEDURE GetSpaceById(IN _id INT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id = _id;
	END //
	
-- Returns all spaces belonging to the space with the given id.
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSubSpacesById;
CREATE PROCEDURE GetSubSpacesById(IN _spaceId INT, IN _userId INT)
	BEGIN
		IF _spaceId <= 0 THEN	-- If we get an invalid ID, return the root space (the space with the mininum ID)
			SELECT *
			FROM spaces
			WHERE id = 
				(SELECT MIN(id)
				FROM spaces);
		ELSE					-- Else find all children spaces that are an ancestor of a space the user is apart of
			SELECT *
			FROM spaces
			WHERE id IN
				(SELECT child_id 
				 FROM set_assoc 
					JOIN closure ON set_assoc.child_id=closure.ancestor 
					JOIN user_assoc ON (user_assoc.user_id=_userId AND user_assoc.space_id=closure.descendant) 
					WHERE set_assoc.space_id=_spaceId)
			ORDER BY name;
		END IF;
	END //
	
-- Returns all spaces that are a subspace of the root
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetSubSpacesOfRoot;
CREATE PROCEDURE GetSubSpacesOfRoot()
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
				(SELECT child_id
				 FROM set_assoc
				 WHERE space_id=1)
		ORDER BY name;
	END //
	
-- Removes the association between a space and a subspace and deletes the subspace
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveSubspace;
CREATE PROCEDURE RemoveSubspace(IN _subspaceId INT)
	BEGIN
		-- Remove that space's default permission
		DELETE FROM permissions 
			WHERE id=(SELECT default_permission FROM spaces WHERE id=_subspaceId);
		-- Remove the space
		DELETE FROM spaces
		WHERE id = _subspaceId;
		
	END //
	
-- Updates the name of the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateSpaceName;
CREATE PROCEDURE UpdateSpaceName(IN _id INT, IN _name VARCHAR(32))
	BEGIN
		UPDATE spaces
		SET name = _name
		WHERE id = _id;
	END //

-- Updates the name of the spacewith the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateSpaceDescription;
CREATE PROCEDURE UpdateSpaceDescription(IN _id INT, IN _desc TEXT)
	BEGIN
		UPDATE spaces
		SET description = _desc
		WHERE id = _id;
	END //
	
-- Updates all details of the space with the given id, and returns the permission id to
-- help update default permissions.
-- Author: Skylar Stark	
DROP PROCEDURE IF EXISTS UpdateSpaceDetails;
CREATE PROCEDURE UpdateSpaceDetails(IN _spaceId INT, IN _name VARCHAR(32), IN _desc TEXT, IN _locked BOOLEAN, OUT _perm INT)
	BEGIN
		UPDATE spaces
		SET name = _name,
		description = _desc,
		locked = _locked
		WHERE id = _spaceId;
		
		SELECT default_permission INTO _perm
		FROM spaces
		WHERE id = _spaceId;
	END //
	
	
/*************************************************************************
*********************** USER STORED PROCEDURES ***************************
*************************************************************************/

-- Begins the registration process by adding a user to the USERS table
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS AddUser;
CREATE PROCEDURE AddUser(IN _first_name VARCHAR(32), IN _last_name VARCHAR(32), IN _email VARCHAR(64), IN _institute VARCHAR(64), IN _password VARCHAR(128),  IN _diskQuota BIGINT, IN _archiveType VARCHAR(8), OUT _id INT)
	BEGIN		
		INSERT INTO users(first_name, last_name, email, institution, created, password, disk_quota, pref_archive_type)
		VALUES (_first_name, _last_name, _email, _institute, SYSDATE(), _password, _diskQuota, _archiveType);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds an association between a user and a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddUserToSpace;
CREATE PROCEDURE AddUserToSpace(IN _userId INT, IN _spaceId INT, IN _proxy INT)
	BEGIN		
		DECLARE _newPermId INT;
		DECLARE _pid INT;
		IF NOT EXISTS(SELECT * FROM user_assoc WHERE user_id = _userId AND space_id = _spaceId) THEN			
			-- Copy the default permission for the community 					
			SELECT default_permission FROM spaces WHERE id=_spaceId INTO _pid;
			CALL CopyPermissions(_pid, _newPermId);
			
			INSERT INTO user_assoc (user_id, space_id, proxy, permission)
			VALUES (_userId, _spaceId, _proxy, _newPermId);
		END IF;
	END //
	
-- Returns the (hashed) password of the user with the given user id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetPasswordById;
CREATE PROCEDURE GetPasswordById(IN _id INT)
	BEGIN
		SELECT password
		FROM users
		WHERE users.id = _id;
	END // 

-- Returns the user record with the given email address
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserByEmail;
CREATE PROCEDURE GetUserByEmail(IN _email VARCHAR(64))
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.email = _email;
	END //
	
-- Returns unregistered user corresponding to the given id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetUnregisteredUserById;
CREATE PROCEDURE GetUnregisteredUserById(IN _id INT)
	BEGIN
		SELECT * 
		FROM users 
		WHERE users.id = _id 
		AND users.email NOT IN
			(SELECT email 
			FROM user_roles);
	END //
	
-- Returns the user record with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetUserById;
CREATE PROCEDURE GetUserById(IN _id INT)
	BEGIN
		SELECT * 
		FROM users NATURAL JOIN user_roles
		WHERE users.id = _id;
	END //
	
-- Retrieves all users belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceUsersById;
CREATE PROCEDURE GetSpaceUsersById(IN _id INT)
	BEGIN
		SELECT DISTINCT *
		FROM users
		WHERE id IN
				(SELECT user_id
				FROM user_assoc
				WHERE space_id = _id)
		ORDER BY first_name;
	END //
	
	
-- Updates the preferred archive type of the user with the given user
-- id to the given archive type.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateArchiveType;
CREATE PROCEDURE UpdateArchiveType(IN _id INT, IN _archiveType VARCHAR(8))
	BEGIN
		UPDATE users
		SET pref_archive_type = _archiveType
		WHERE users.id = id;
	END //

	
-- Updates the email address of the user with the given user id to the
-- given email address. The email address should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateEmail;
CREATE PROCEDURE UpdateEmail(IN _id INT, IN _email VARCHAR(64))
	BEGIN
		UPDATE users
		SET email = _email
		WHERE users.id = _id;
	END //
	
-- Updates the first name of the user with the given user id to the
-- given first name. The first name should already be validated.	
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateFirstName;
CREATE PROCEDURE UpdateFirstName(IN _id INT, IN _firstname VARCHAR(32))
	BEGIN
		UPDATE users
		SET first_name = _firstname
		WHERE users.id = _id;
	END //
	
-- Updates the last name of the user with the given user id to the
-- given last name. The last name should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateLastName;
CREATE PROCEDURE UpdateLastName(IN _id INT, IN _lastname VARCHAR(32))
	BEGIN
		UPDATE users
		SET last_name = _lastname
		WHERE users.id = _id;
	END //

-- Updates the institution of the user with the given user id to the
-- given institution. The institution should already be validated
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdateInstitution;
CREATE PROCEDURE UpdateInstitution(IN _id INT, IN _institution VARCHAR(64))
	BEGIN
		UPDATE users
		SET institution = _institution
		WHERE users.id = _id;
	END //

-- Updates the password of the user with the given user id to the
-- given (already hashed and validated) password.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS UpdatePassword;
CREATE PROCEDURE UpdatePassword(IN _id INT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //
	
-- Sets a new password for a given user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS SetPasswordByUserId;
CREATE PROCEDURE SetPasswordByUserId(IN _id INT, IN _password VARCHAR(128))
	BEGIN
		UPDATE users
		SET password = _password
		WHERE users.id = _id;
	END //

-- Increments the disk_quota attribute of the users table by the value of _newBytes
-- for the given user
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateUserDiskQuota;
CREATE PROCEDURE UpdateUserDiskQuota(IN _userId INT, IN _newQuota BIGINT)
	BEGIN
		UPDATE users
		SET disk_quota = _newQuota
		WHERE id = _userId;
	END //
	
-- Returns the number of bytes a given user is consuming on disk
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetUserDiskUsage;
CREATE PROCEDURE GetUserDiskUsage(IN _userID INT)
	BEGIN
		SELECT sum(benchmarks.disk_size + solvers.disk_size) AS disk_usage
		FROM   benchmarks,solvers
		WHERE  benchmarks.user_id = _userId
		AND    solvers.user_id = _userId;
	END //
	
-- Returns one record if a given user is a member of a particular space
-- otherwise it returns an empty set
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS IsMemberOfSpace;
CREATE PROCEDURE IsMemberOfSpace(IN _userId INT, IN _spaceId INT)
	BEGIN
		SELECT *
		FROM  user_assoc
		WHERE user_id  = _userId
		AND   space_id = _spaceId;
	END // 

	
	
	
/*************************************************************************
********************** WEBSITE STORED PROCEDURES *************************
*************************************************************************/
	
-- Adds a website that is associated with a user
-- Author: Skylar Stark	
DROP PROCEDURE IF EXISTS AddUserWebsite;
CREATE PROCEDURE AddUserWebsite(IN _userId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(user_id, url, name)
		VALUES(_userId, _url, _name);
	END //
	
-- Adds a website that is associated with a solver
-- Author: Tyler Jensen	
DROP PROCEDURE IF EXISTS AddSolverWebsite;
CREATE PROCEDURE AddSolverWebsite(IN _solverId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(solver_id, url, name)
		VALUES(_solverId, _url, _name);
	END //
	
-- Adds a website that is associated with a space (community)
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddSpaceWebsite;
CREATE PROCEDURE AddSpaceWebsite(IN _spaceId INT, IN _url TEXT, IN _name VARCHAR(64))
	BEGIN
		INSERT INTO website(space_id, url, name)
		VALUES(_spaceId, _url, _name);
	END //

-- Deletes the solver website with the given website id
-- Author: Skylar Stark	
DROP PROCEDURE IF EXISTS DeleteSolverWebsite;
CREATE PROCEDURE DeleteSolverWebsite(IN _id INT, IN _solverId INT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND solver_id = _solverId;
	END // 
	
-- Deletes the space website with the given website id
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS DeleteSpaceWebsite;
CREATE PROCEDURE DeleteSpaceWebsite(IN _id INT, IN _spaceId INT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND space_id = _spaceId;
	END //
	
-- Deletes the user website with the given website id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS DeleteUserWebsite;
CREATE PROCEDURE DeleteUserWebsite(IN _id INT, IN _userId INT)
	BEGIN
		DELETE FROM website
		WHERE id = _id AND user_id = _userId;
	END //

	
-- Returns all websites associated with the user with the given user id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetWebsitesByUserId;
CREATE PROCEDURE GetWebsitesByUserId(IN _userid INT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.user_id = _userid
		ORDER BY name;
	END //

-- Gets all websites that are associated with the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetWebsitesBySolverId;
CREATE PROCEDURE GetWebsitesBySolverId(IN _id INT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.solver_id = _id
		ORDER BY name;
	END //

-- Gets all websites that are associated with the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetWebsitesBySpaceId;
CREATE PROCEDURE GetWebsitesBySpaceId(IN _id INT)
	BEGIN
		SELECT id, name, url
		FROM website
		WHERE website.space_id = _id
		ORDER BY name;
	END //	

DELIMITER ; -- This should always be at the end of this file