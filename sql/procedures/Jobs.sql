-- Description: This file contains all job-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


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

-- Returns the number of jobs in a given space
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetJobCountInSpace;
CREATE PROCEDURE GetJobCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE id IN (SELECT job_id
					FROM job_assoc
					WHERE space_id = _spaceId);
	END //
	
	
-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfJobs;
CREATE PROCEDURE GetNextPageOfJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Jobs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				
				-- Exclude Jobs that aren't in the specified space
				WHERE 	id 	IN (SELECT job_id 
									FROM job_assoc
									WHERE space_id = _spaceId)
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of Jobs
				-- LIMIT _startingRecord, _recordsPerPage;
				LIMIT 0, 10;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				WHERE 	id 	IN (SELECT job_id 
									FROM job_assoc
									WHERE space_id = _spaceId)
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				-- LIMIT _startingRecord, _recordsPerPage;
				LIMIT 0, 10;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				
				-- Exclude Jobs whose name and description don't contain the query string
				WHERE 	id		IN (SELECT	id
									FROM 	jobs
									WHERE 	name		LIKE	CONCAT('%', _query, '%'))
										
				-- Exclude Jobs that aren't in the specified space
				AND 	id 		IN (SELECT job_id 
									FROM job_assoc
									WHERE space_id = _spaceId)
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of Jobs
				-- LIMIT _startingRecord, _recordsPerPage;
				LIMIT 0, 10;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				WHERE 	id		IN (SELECT	id
									FROM 	jobs
									WHERE 	name		LIKE	CONCAT('%', _query, '%'))
				AND 	id 		IN (SELECT job_id 
									FROM job_assoc
									WHERE space_id = _spaceId)
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				-- LIMIT _startingRecord, _recordsPerPage;
				LIMIT 0, 10;
			END IF;
		END IF;
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

	
DELIMITER ; -- This should always be at the end of this file