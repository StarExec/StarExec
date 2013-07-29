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

-- 	Returns the number of public spaces the job is in
--  Author: Benton McCune
DROP PROCEDURE IF EXISTS JobInPublicSpace;
CREATE PROCEDURE JobInPublicSpace(IN _jobId INT)
	BEGIN 
		SELECT COUNT(*) AS spaceCount
		FROM spaces WHERE (public_access = 1)
		AND id IN (SELECT space_id FROM job_assoc WHERE job_id = _jobId);
	END //
	
-- Adds a new attribute to a job pair 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobAttr;
CREATE PROCEDURE AddJobAttr(IN _pairId INT, IN _jobId INT, IN _key VARCHAR(128), IN _val VARCHAR(128))
	BEGIN
		REPLACE INTO job_attributes VALUES (_pairId, _key, _val, _jobId);
	END //

-- Returns the number of jobs in a given space
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetJobCountBySpace;
CREATE PROCEDURE GetJobCountBySpace(IN _spaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE id IN (SELECT job_id
					FROM job_assoc
					WHERE space_id = _spaceId);
	END //

-- Returns the number of jobs pairs for a given job
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetJobPairCountByJob;
CREATE PROCEDURE GetJobPairCountByJob(IN _jobId INT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_id = _jobId;
	END //

	
-- Returns the number of jobs pairs for a given job in a given space with a given configuration
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountByConfigInJobSpace;
CREATE PROCEDURE GetJobPairCountByConfigInJobSpace(IN _jobId INT, IN _spaceId INT, IN _configId INT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_id = _jobId AND job_space_id=_spaceId AND config_id=_configId;
	END //
	
-- Returns the number of jobs pairs for a given job
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetJobPairCountByJobInJobSpace;
CREATE PROCEDURE GetJobPairCountByJobInJobSpace(IN _jobId INT, IN _jobSpaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_id = _jobId AND job_space_id=_jobSpaceId;
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
						deleted,
						primary_space,
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
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
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
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
										
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
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
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
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //


	
	
	-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets jobs across all spaces for one user.  
-- Author: Ben and Ruoyu
DROP PROCEDURE IF EXISTS GetNextPageOfUserJobs;
CREATE PROCEDURE GetNextPageOfUserJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Jobs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false
				
				
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
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false
				
				-- Exclude Jobs whose name and status don't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
										
										
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
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs 
				WHERE user_id = _userId
				
				AND deleted=false
				
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairs;
CREATE PROCEDURE GetNextPageOfJobPairs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _jobId INT, IN _query TEXT, IN _spaceId INT, IN _configId INT)
	BEGIN
		-- If _query is empty, get next page of JobPairs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						job_pairs.bench_id,
						job_pairs.config_id,
						job_pairs.cpu,
						job_pairs.job_space_id,
						config.id,
						config.name,
						config.description,
						status.code,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						jobSpace.name,
						GetJobPairResult(job_pairs.id) AS result,
						cpu
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN 	job_spaces		AS jobSpace ON job_pairs.job_space_id=jobSpace.id	
				
				WHERE 	job_id = _jobId  AND ((_spaceId IS NULL) OR job_space_id=_spaceId) AND ((_configId IS NULL) OR config_id=_configId)
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN bench.name
					 	WHEN 1 THEN solver.name
					 	WHEN 2 THEN config.name
					 	WHEN 3 THEN status.status
					 	WHEN 4 THEN cpu
					 	WHEN 5 THEN result
					 	WHEN 6 THEN jobSpace.name
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						job_pairs.bench_id,
						job_pairs.config_id,
						job_pairs.job_space_id,
						config.id,
						config.name,
						config.description,
						status.code,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						jobSpace.name,
						GetJobPairResult(job_pairs.id) AS result,
						cpu
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN 	job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
									
				WHERE 	job_id = _jobId AND ((_spaceId IS NULL) OR job_space_id=_spaceId) AND ((_configId IS NULL) OR config_id=_configId)
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN bench.name
					 	WHEN 1 THEN solver.name
					 	WHEN 2 THEN config.name
					 	WHEN 3 THEN status.status
					 	WHEN 4 THEN cpu
					 	WHEN 5 THEN result
					 	WHEN 6 THEN jobSpace.name
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						job_pairs.bench_id,
						job_pairs.config_id,
						job_pairs.job_space_id,
						config.id,
						config.name,
						config.description,
						status.code,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						jobSpace.name,
						GetJobPairResult(job_pairs.id) AS result,
						cpu
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN	job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id

				WHERE 	job_id = _jobId  AND ((_spaceId IS NULL) OR job_space_id=_spaceId) AND ((_configId IS NULL) OR config_id=_configId)
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench.name 		LIKE 	CONCAT('%', _query, '%')
				OR		config.name		LIKE	CONCAT('%', _query, '%')
				OR		solver.name		LIKE	CONCAT('%', _query, '%')
				OR 		jobSpace.name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN bench.name
					 	WHEN 1 THEN solver.name
					 	WHEN 2 THEN config.name
					 	WHEN 3 THEN status.status
					 	WHEN 4 THEN cpu
					 	WHEN 5 THEN result
					 	WHEN 6 THEN jobSpace.name
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						job_pairs.bench_id,
						job_pairs.config_id,
						job_pairs.job_space_id,
						config.id,
						config.name,
						config.description,
						status.code,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						jobSpace.name,
						GetJobPairResult(job_pairs.id) AS result,
						cpu
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN 	job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id

				WHERE 	job_id = _jobId AND ((_spaceId IS NULL) OR job_space_id=_spaceId) AND ((_configId IS NULL) OR config_id=_configId)
				AND		(bench.name 		LIKE 	CONCAT('%', _query, '%')
				OR		config.name		LIKE	CONCAT('%', _query, '%')
				OR		solver.name		LIKE	CONCAT('%', _query, '%')
				OR		jobSpace.name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN bench.name
					 	WHEN 1 THEN solver.name
					 	WHEN 2 THEN config.name
					 	WHEN 3 THEN status.status
					 	WHEN 4 THEN cpu
					 	WHEN 5 THEN result
					 	WHEN 6 THEN jobSpace.name 
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
DROP PROCEDURE IF EXISTS GetJobAttrs;
CREATE PROCEDURE GetJobAttrs(IN _jobId INT)
	BEGIN
		SELECT *
		FROM job_pairs AS pair 
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			WHERE pair.job_id=_jobId;
	END //
	
DROP PROCEDURE IF EXISTS GetJobAttrsInJobSpace;
CREATE PROCEDURE GetJobAttrsInJobSpace(IN _jobId INT, IN _jobSpaceId INT)
	BEGIN
		SELECT *
		FROM job_pairs AS pair 
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			WHERE pair.job_id=_jobId AND pair.job_space_id=_jobSpaceId;
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
		WHERE id = _id and deleted=false;
	END //
	
-- Retrieves basic info about a job from the jobs table
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobByIdIncludeDeleted;
CREATE PROCEDURE GetJobByIdIncludeDeleted(IN _id INT)
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
		FROM job_pairs 				JOIN    status_codes    AS  status  ON job_pairs.status_code=status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id
									LEFT JOIN	job_spaces 		AS  jobSpace ON jobSpace.id=job_pairs.job_space_id
									
		WHERE job_pairs.job_id=_id
		ORDER BY job_pairs.end_time DESC;
	END //

-- Gets job pair information necessary for stats (benchmark information excluded)
-- Author: Eric Burns
	
DROP PROCEDURE IF EXISTS GetJobPairsByJobForStats;
CREATE PROCEDURE GetJobPairsByJobForStats(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
					   JOIN configurations AS config ON job_pairs.config_id=config.id
					   JOIN solvers AS solver ON config.solver_id=solver.id
		WHERE job_pairs.job_id=_id
		ORDER BY job_pairs.end_time DESC;
	END //	
	
	
-- Retrieves info about job pairs for a given job in a given space with a given configuration
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsByConfigInJobSpace;
CREATE PROCEDURE GetJobPairsByConfigInJobSpace(IN _id INT, IN _jobSpaceId INT, IN _configId INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
						JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
						JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
						JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
						JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id

					   JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.job_id=_id AND job_pairs.job_space_id=_jobSpaceId AND job_pairs.config_id=_configId;
	END //




-- Gets all the job pairs for a given job in a particular space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsByJobInJobSpace;
CREATE PROCEDURE GetJobPairsByJobInJobSpace(In _jobId INT, IN _jobSpaceId INT)
	BEGIN
		SELECT *
		FROM job_pairs 				JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_id=_jobId AND job_space_id =_jobSpaceId;
	END //
	
-- Retrieves basic info about job pairs for the given job id for pairs completed after _completionId
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewCompletedJobPairsByJob;
CREATE PROCEDURE GetNewCompletedJobPairsByJob(IN _id INT, IN _completionId INT)
	BEGIN
		SELECT *
		FROM job_pairs JOIN status_codes AS status ON job_pairs.status_code=status.code
						JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
						JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
						JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
						JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id

					   INNER JOIN job_pair_completion AS complete ON job_pairs.id=complete.pair_id
					   LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.job_id=_id AND complete.completion_id>_completionId
		ORDER BY job_pairs.end_time DESC;
	END //
	
-- Retrieves basic info about pending/rejected job pairs for the given job id
-- Author:Benton McCune
DROP PROCEDURE IF EXISTS GetPendingJobPairsByJob;
CREATE PROCEDURE GetPendingJobPairsByJob(IN _id INT, IN _cap INT)
	BEGIN
		SELECT *
		FROM job_pairs 
		WHERE job_id=_id AND (status_code = 1)
		ORDER BY id ASC
		LIMIT _cap;
	END //	
	
-- Retrieves basic info about enqueued job pairs for the given job id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEnqueuedJobPairsByJob;
CREATE PROCEDURE GetEnqueuedJobPairsByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE (job_id = _id AND status_code = 2)
		ORDER BY sge_id ASC;
	END //
	
-- Returns true if the job in question has the deleted flag set as true
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsJobDeleted;
CREATE PROCEDURE IsJobDeleted(IN _jobId INT)
	BEGIN
		SELECT count(*) AS jobDeleted
		FROM jobs
		WHERE deleted=true AND id=_jobId;
	END //

-- Returns true if the job in question has the paused flag set to true	
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS IsJobPaused;
CREATE PROCEDURE IsJobPaused(IN _jobId INT)
	BEGIN
		SELECT count(*) AS jobPaused
		FROM jobs
		WHERE paused=true AND id=_jobId;
	END //
	
-- Returns true if the job in question has the killed flag set to true	
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS IsJobKilled;
CREATE PROCEDURE IsJobKilled(IN _jobId INT)
	BEGIN
		SELECT count(*) AS jobKilled
		FROM jobs
		WHERE killed=true AND id=_jobId;
	END //

-- Sets the "deleted" property of a job to true and deletes all its job pairs from the database
-- If the job has no more space associations, it is deleted from the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteJob;
CREATE PROCEDURE DeleteJob(IN _jobId INT)
	BEGIN
		UPDATE jobs
		SET deleted=true
		WHERE id = _jobId;
		DELETE FROM job_pairs
		WHERE job_id=_jobId;
		-- if the benchmark is associated with no spaces, we can delete it from the database
		IF ((SELECT COUNT(*) FROM job_assoc WHERE job_id=_jobId)=0) THEN
			DELETE FROM jobs
			WHERE id=_jobId;
		END IF;
	END //
	
-- Sets the "paused" property of a job to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS PauseJob;
CREATE PROCEDURE PauseJob(IN _jobId INT)
	BEGIN
		UPDATE jobs
		SET paused=true
		WHERE id = _jobId;
		
		UPDATE job_pairs
		SET status_code = 20
		WHERE job_id = _jobId AND status_code = 1;
	END //
	
-- Sets the "paused" property of a job to false
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ResumeJob;
CREATE PROCEDURE ResumeJob(IN _jobId INT)
	BEGIN
		UPDATE jobs
		SET paused=false
		WHERE id = _jobId;
		
		UPDATE job_pairs
		SET status_code = 1
		WHERE job_id = _jobId AND status_code = 20;
	END //
	
-- Sets the "killed" property of a job to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS KillJob;
CREATE PROCEDURE KillJob(IN _jobId INT)
	BEGIN
		UPDATE jobs
		SET killed=true
		WHERE id = _jobId;
		
		UPDATE jobs
		SET paused=false
		WHERE id = _jobId;
		
		UPDATE job_pairs
		SET status_code = 21
		WHERE job_id = _jobId AND (status_code = 1 OR status_code = 20);
		
	END //

-- Adds a new job pair record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobPair;
CREATE PROCEDURE AddJobPair(IN _jobId INT, IN _benchId INT, IN _configId INT, IN _status TINYINT, IN _cpuTimeout INT, IN _clockTimeout INT, IN _path VARCHAR(2048),IN _jobSpaceId INT, OUT _id INT)
	BEGIN
		INSERT INTO job_pairs (job_id, bench_id, config_id, status_code, cpuTimeout, clockTimeout, path,job_space_id)
		VALUES (_jobId, _benchId, _configId, _status, _cpuTimeout, _clockTimeout, _path, _jobSpaceId);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds a new job record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJob;
CREATE PROCEDURE AddJob(IN _userId INT, IN _name VARCHAR(64), IN _desc TEXT, IN _queueId INT, IN _preProcessor INT, IN _postProcessor INT, IN _spaceId INT, OUT _id INT)
	BEGIN
		INSERT INTO jobs (user_id, name, description, queue_id, pre_processor, post_processor, primary_space)
		VALUES (_userId, _name, _desc, _queueId, _preProcessor, _postProcessor, _spaceId);
		SELECT LAST_INSERT_ID() INTO _id;
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

-- Retrieves all jobs belonging to a user (but not their job pairs)
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS GetUserJobsById;
CREATE PROCEDURE GetUserJobsById(IN _userId INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE user_id=_userId and deleted=false
		ORDER BY created DESC;
	END //

-- Get the total count of the jobs belong to a specific user
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS GetJobCountByUser;
CREATE PROCEDURE GetJobCountByUser(IN _userId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE user_id = _userId and deleted=false;
	END //
	
DROP PROCEDURE IF EXISTS GetNameofJobById;
CREATE PROCEDURE GetNameofJobById(IN _jobId INT)
	BEGIN
		SELECT name
		FROM jobs
		where id = _jobId and deleted=false;
	END //
	
-- Sets the "paused" property of a job to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS UpdatePrimarySpace;
CREATE PROCEDURE UpdatePrimarySpace(IN _jobId INT, IN _jobSpaceId INT)
	BEGIN
		UPDATE jobs
		SET primary_space=_jobSpaceId
		WHERE id = _jobId;
	END //
	
DELIMITER ; -- this should always be at the end of the file