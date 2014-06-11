-- Description: This file contains all job-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

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
		SELECT COUNT(*) AS spaceCount FROM job_assoc
			INNER JOIN spaces ON spaces.id=job_assoc.space_id
		WHERE job_id=_jobId AND spaces.public_access=1;
	END //
	
-- Adds a new attribute to a job pair 
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobAttr;
CREATE PROCEDURE AddJobAttr(IN _pairId INT, IN _key VARCHAR(128), IN _val VARCHAR(128))
	BEGIN
		REPLACE INTO job_attributes VALUES (_pairId, _key, _val, (select job_id from job_pairs where id=_pairId));
	END //

-- Returns the number of jobs in a given space
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetJobCountBySpace;
CREATE PROCEDURE GetJobCountBySpace(IN _spaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM job_assoc
		WHERE _spaceId=space_id;
	END //
-- Returns the number of jobs in a given space that match a given query
-- Author: Eric burns
DROP PROCEDURE IF EXISTS GetJobCountBySpaceWithQuery;
CREATE PROCEDURE GetJobCountBySpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM job_assoc
			JOIN jobs AS jobs ON jobs.id=job_assoc.job_id
		WHERE _spaceId=job_assoc.space_id
		AND (jobs.name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(jobs.id)	LIKE	CONCAT('%', _query, '%'));
	END //

	
-- Returns the number of jobs pairs for a given job in a given space with a given configuration
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountByConfigInJobSpace;
CREATE PROCEDURE GetJobPairCountByConfigInJobSpace(IN _spaceId INT, IN _configId INT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_space_id=_spaceId AND config_id=_configId;
	END //
		
-- Returns the number of jobs pairs for a given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountByJobInJobSpace;
CREATE PROCEDURE GetJobPairCountByJobInJobSpace(IN _jobSpaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_space_id=_jobSpaceId;
	END //
	
-- Returns the number of jobs pairs for a given job that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountByJobInJobSpaceWithQuery;
CREATE PROCEDURE GetJobPairCountByJobInJobSpaceWithQuery(IN _jobSpaceId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS jobPairCount
		FROM job_pairs
		WHERE job_space_id=_jobSpaceId
		AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code		LIKE 	CONCAT('%', _query, '%')
				OR		wallclock				LIKE	CONCAT('%', _query, '%'));
	END //		

	
-- Gets attributes for every pair in a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobAttrs;
CREATE PROCEDURE GetJobAttrs(IN _jobId INT)
	BEGIN
		SELECT pair.id, attr.attr_key, attr.attr_value
		FROM job_pairs AS pair 
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			WHERE pair.job_id=_jobId;
	END //
	
-- Gets the attributes for every job pair of a job completed after the given completion id
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewJobAttrs;
CREATE PROCEDURE GetNewJobAttrs(IN _jobId INT, IN _completionId INT)
	BEGIN
		SELECT pair.id, attr.attr_key, attr.attr_value
		FROM job_pairs AS pair
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			INNER JOIN job_pair_completion AS complete ON pair.id=complete.pair_id
			WHERE pair.job_id=_jobId AND complete.completion_id>_completionId;

	END //

-- Gets attributes for every job pair in a job that resides in the given job space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobAttrsInJobSpace;
CREATE PROCEDURE GetJobAttrsInJobSpace(IN _jobSpaceId INT)
	BEGIN
		SELECT pair.id, attr.attr_key, attr.attr_value
		FROM job_pairs AS pair 
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			WHERE pair.job_space_id=_jobSpaceId;
	END //

-- Adds a new job stats record to the database
-- Author : Eric Burns
DROP PROCEDURE IF EXISTS AddJobStats;
CREATE PROCEDURE AddJobStats(IN _jobSpaceId INT, IN _configId INT, IN _complete INT, IN _correct INT, IN _incorrect INT, IN _failed INT, IN _wallclock DOUBLE)
	BEGIN
		INSERT INTO job_stats (job_space_id, config_id, complete, correct, incorrect, failed, wallclock)
		VALUES (_jobSpaceId, _configId, _complete, _correct, _incorrect, _failed, _wallclock);
	END //	

-- Gets the cached job results for the hierarchy rooted at the given job space
-- Author: Eric Burns	

DROP PROCEDURE IF EXISTS GetJobStatsInJobSpace;
CREATE PROCEDURE GetJobStatsInJobSpace(IN _jobSpaceId INT) 
	BEGIN
		SELECT *
		FROM job_stats
			JOIN configurations AS config ON config.id=job_stats.config_id
			JOIN solvers AS solver ON solver.id=config.solver_id
		WHERE job_stats.job_space_id = _jobSpaceId;
	END //

-- Removes the cached job results for the hierarchy rooted at the given job space
-- Author: Eric Burns	

DROP PROCEDURE IF EXISTS RemoveJobStatsInJobSpace;
CREATE PROCEDURE RemoveJobStatsInJobSpace(IN _jobSpaceId INT) 
	BEGIN
		DELETE FROM job_stats
		WHERE job_stats.job_space_id = _jobSpaceId;
	END //

-- Counts the number of pending pairs in a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS CountPendingPairs;
CREATE PROCEDURE CountPendingPairs(IN _jobId INT) 
	BEGIN
		SELECT count(*) AS pending FROM job_pairs
		WHERE status_code BETWEEN 1 AND 6 AND job_id=_jobId;
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
			(SELECT COUNT(*) AS pendingPairs FROM job_pairs WHERE job_id=_jobId AND (status_code BETWEEN 1 AND 6 OR status_code=22)) AS c, -- Gets number of pairs with non complete and non error status codes
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



-- Retrieves basic info about job pairs for the given job id (simple version)
-- Author: Julio Cervantes
DROP PROCEDURE IF EXISTS GetJobPairsByJobSimple;
CREATE PROCEDURE GetJobPairsByJobCesar(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE job_pairs.job_id=_id;
	END //

-- Retrieves basic info about job pairs for the given job id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairsByJob;
CREATE PROCEDURE GetJobPairsByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs 				
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id
									LEFT JOIN	job_spaces 		AS  jobSpace ON jobSpace.id=job_pairs.job_space_id
									
		WHERE job_pairs.job_id=_id
		ORDER BY job_pairs.end_time DESC;
	END //

	
-- Retrieves info about job pairs for a given job in a given space with a given configuration
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsShallowWithBenchmarksByConfigInJobSpace;
CREATE PROCEDURE GetJobPairsShallowWithBenchmarksByConfigInJobSpace(IN _jobSpaceId INT, IN _configId INT)
	BEGIN
		SELECT cpu,wallclock,job_pairs.id, status_code, solver_id, solver_name, config_id, config_name,bench_id,bench_name
		FROM job_pairs 
		WHERE job_pairs.job_space_id=_jobSpaceId AND job_pairs.config_id=_configId;
	END //
	
	
-- Retrieves info about job pairs for a given job in a given space with a given configuration
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsShallowByConfigInJobSpace;
CREATE PROCEDURE GetJobPairsShallowByConfigInJobSpace(IN _jobSpaceId INT, IN _configId INT)
	BEGIN
		SELECT cpu,wallclock,job_pairs.id, status_code, solver_id, solver_name, config_id, config_name
		FROM job_pairs 
		WHERE job_pairs.job_space_id=_jobSpaceId AND job_pairs.config_id=_configId;
	END //	
	
-- Retrieves info about job pairs for a given job in a given space with a given configuration,
-- getting back only the data required to populate a client side datatable
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsForTableByConfigInJobSpace;
CREATE PROCEDURE GetJobPairsForTableByConfigInJobSpace(IN _jobSpaceId INT, IN _configId INT)
	BEGIN
		SELECT job_pairs.id, 
				job_pairs.status_code,
				bench_id,
				bench_name,
				GetJobPairResult(job_pairs.id) AS result,
				wallclock
		FROM job_pairs 
		WHERE job_pairs.job_space_id=_jobSpaceId AND job_pairs.config_id=_configId;
	END //

-- Gets all the attribute values for benchmarks in the given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetAttrsOfNameForJob;
CREATE PROCEDURE GetAttrsOfNameForJob(IN _jobId INT, IN _attrName VARCHAR(128))
	BEGIN
		SELECT job_pairs.bench_id, attr_value 
		FROM job_pairs JOIN bench_attributes ON job_pairs.bench_id = bench_attributes.bench_id
		
		WHERE attr_key=_attrName AND job_id=_jobId;
	END  //


-- Gets all the job pairs for a given job in a particular space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsByJobInJobSpace;
CREATE PROCEDURE GetJobPairsByJobInJobSpace(IN _jobSpaceId INT)
	BEGIN
		SELECT solver_id,solver_name,config_id,config_name,status_code,cpu,wallclock,job_pairs.id,bench_id
		FROM job_pairs 				
		WHERE job_space_id =_jobSpaceId;
	END //
	
-- Retrieves basic info about job pairs for the given job id for pairs completed after _completionId
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewCompletedJobPairsByJob;
CREATE PROCEDURE GetNewCompletedJobPairsByJob(IN _id INT, IN _completionId INT)
	BEGIN
		SELECT *
		FROM job_pairs 
						JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
						JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
						JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
						JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id

					   INNER JOIN job_pair_completion AS complete ON job_pairs.id=complete.pair_id
					   LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.job_id=_id AND complete.completion_id>_completionId
		ORDER BY job_pairs.end_time DESC;
	END //
	
-- Retrieves basic info about job pairs with a given status
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsByStatus;
CREATE PROCEDURE GetJobPairsByStatus(IN _jobId INT, IN _cap INT, IN _statusCode INT)
	BEGIN 
		SELECT * FROM job_pairs
		WHERE job_id=_id AND status_code=_statusCode
		LIMIT _cap;
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
	
-- Retrieves basic info about running job pairs for the given job id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRunningJobPairsByJob;
CREATE PROCEDURE GetRunningJobPairsByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE (job_id = _id AND status_code = 4)
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



-- Returns the paused and deleted columns for a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsJobPausedOrKilled;
CREATE PROCEDURE IsJobPausedOrKilled(IN _jobId INT)
	BEGIN
		SELECT paused,killed
		FROM jobs
		WHERE id=_jobId;
	END //
	
-- Get all the job pairs that are not complete
-- Unknown, pending, enqueued, preparing, running, finishing, or awaiting results
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetIncompleteJobPairs;
CREATE PROCEDURE GetIncompleteJobPairs(IN _jobId INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE (job_id = _jobId AND status_code < 7)
		ORDER BY sge_id ASC;
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
	
-- Sets the global paused flag to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS PauseAll;
CREATE PROCEDURE PauseAll()
	BEGIN
		INSERT INTO system_flags (integrity_keeper, paused)
		VALUES ('', true)
		ON DUPLICATE KEY UPDATE paused = true;
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
	
-- sets the global paused flag to false
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ResumeAll;
CREATE PROCEDURE ResumeAll()
	BEGIN
		INSERT INTO system_flags (integrity_keeper, paused)
		VALUES ('', false)
		ON DUPLICATE KEY UPDATE paused = false;
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
	
-- Changes the queueid in the jobs datatable
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ChangeQueue;
CREATE PROCEDURE ChangeQueue(IN _jobId INT, IN _queueId INT)
	BEGIN
		UPDATE jobs
		SET queue_id = _queueId
		WHERE id = _jobId;
	END //

-- Adds a new job pair record to the database
-- Author: Tyler Jensen + Eric Burns
DROP PROCEDURE IF EXISTS AddJobPair;
CREATE PROCEDURE AddJobPair(IN _jobId INT, IN _benchId INT, IN _configId INT, IN _status TINYINT, IN _cpuTimeout INT, IN _clockTimeout INT, IN _path VARCHAR(2048),IN _jobSpaceId INT,IN _configName VARCHAR(256), IN _solverName VARCHAR(256), IN _benchName VARCHAR(256), IN _solverId INT, IN _mem BIGINT, OUT _id INT)
	BEGIN
		INSERT INTO job_pairs (job_id, bench_id, config_id, status_code, cpuTimeout, clockTimeout, path,job_space_id,solver_name,bench_name,config_name,solver_id, maximum_memory)
		VALUES (_jobId, _benchId, _configId, _status, _cpuTimeout, _clockTimeout, _path, _jobSpaceId, _solverName,  _benchName, _configName, _solverId, _mem);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Adds a new job record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJob;
CREATE PROCEDURE AddJob(IN _userId INT, IN _name VARCHAR(64), IN _desc TEXT, IN _queueId INT, IN _preProcessor INT, IN _postProcessor INT, IN _spaceId INT, IN _seed BIGINT, OUT _id INT)
	BEGIN
		INSERT INTO jobs (user_id, name, description, queue_id, pre_processor, post_processor, primary_space,seed)
		VALUES (_userId, _name, _desc, _queueId, _preProcessor, _postProcessor, _spaceId,_seed);
		SELECT LAST_INSERT_ID() INTO _id;
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
	
-- Returns the number of jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetJobCount;
CREATE PROCEDURE GetJobCount()
	BEGIN
		SELECT COUNT(*) as jobCount
		FROM jobs;
	END //
	
-- Returns the number of running jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRunningJobCount;
CREATE PROCEDURE GetRunningJobCount()
	BEGIN
		SELECT COUNT(distinct jobs.id) as jobCount
		FROM jobs
		JOIN    job_pairs ON jobs.id = job_pairs.job_id
		WHERE 	job_pairs.status_code < 7;
	END //
	
-- Returns the number of paused jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetPausedJobCount;
CREATE PROCEDURE GetPausedJobCount()
	BEGIN
		SELECT COUNT(distinct jobs.id) as jobCount
		FROM jobs
		JOIN    job_pairs ON jobs.id = job_pairs.job_id
		WHERE 	job_pairs.status_code = 20;
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
	
-- Returns the number of jobs in a given space that match a given query
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS GetJobCountByUserWithQuery;
CREATE PROCEDURE GetJobCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE user_id=_userId AND deleted=false AND
				(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'));
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
-- Populates the solver_name, config_name, and bench_name columns of all pairs in the 
-- job_pair table. Should only need to be run once on Starexec and Stardev to get the table
-- up to date
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetNewColumns;
CREATE PROCEDURE SetNewColumns() 
	BEGIN
		UPDATE job_pairs
			JOIN benchmarks AS bench ON bench.id=bench_id
			JOIN configurations AS config ON config.id=config_id
			JOIN solvers AS solve ON solve.id=config.solver_id
			SET bench_name=bench.name, solver_name=solve.name, config_name=config.name, job_pairs.solver_id=solve.id;
	END //
	
-- Gets back only the fields of a job pair that are necessary to determine where it is stored on disk
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS GetNewJobPairFilePathInfoByJob;
CREATE PROCEDURE GetNewJobPairFilePathInfoByJob(IN _jobID INT, IN _completionID INT)
	BEGIN
		SELECT path,solver_name,config_name,bench_name, complete.completion_id FROM job_pairs
			JOIN job_pair_completion AS complete ON job_pairs.id=complete.pair_id
		WHERE job_pairs.job_id=_jobID AND complete.completion_id>_completionId;
	END //

-- Sets all the pairs of a given job to the given status
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS SetPairsToStatus;
CREATE PROCEDURE SetPairsToStatus(IN _jobId INT, In _statusCode INT)
	BEGIN
		UPDATE job_pairs
		SET status_code = _statusCode
		WHERE job_id = _jobId;
	END //
	
-- Removes all jobs in the database that are deleted and also orphaned. Runs periodically.
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveDeletedOrphanedJobs;
CREATE PROCEDURE RemoveDeletedOrphanedJobs()
	BEGIN
		DELETE jobs FROM jobs
			LEFT JOIN job_assoc ON job_assoc.job_id=jobs.id
		WHERE deleted=true AND job_assoc.space_id IS NULL;
	END //
	
-- Gives back the number of pairs that are awaiting post_processing for a given job
DROP PROCEDURE IF EXISTS CountProcessingPairsByJob;
CREATE PROCEDURE CountProcessingPairsByJob(IN _jobId INT, IN _processingStatus INT)
	BEGIN
		SELECT COUNT(*) AS processing
		FROM job_pairs 
		WHERE job_pairs.job_id=_jobId and _processingStatus=status_code;
	END //
	
-- For a given job, sets every pair at the complete status to the processing status, and also changes the post_processor
-- of the job to the given one
DROP PROCEDURE IF EXISTS PrepareJobForPostProcessing;
CREATE PROCEDURE PrepareJobForPostProcessing(IN _jobId INT, IN _procId INT, IN _completeStatus INT, IN _processingStatus INT)
	BEGIN
		UPDATE job_pairs SET status_code=_processingStatus WHERE job_id=_jobId AND status_code=_completeStatus;
		UPDATE jobs SET post_processor = _procId WHERE id=_jobId;
	END //
	
	
-- Gets the wallclock timeout for the given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetWallclockTimeout;
CREATE PROCEDURE GetWallclockTimeout(IN _jobId INT)
	BEGIN
		SELECT clockTimeout 
		FROM job_pairs 
		WHERE job_id=_jobId LIMIT 1;
	END //
	
-- Gets the cpu timeout for the given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetCpuTimeout;
CREATE PROCEDURE GetCpuTimeout(IN _jobId INT)
	BEGIN
		SELECT cpuTimeout
		FROM job_pairs 
		WHERE job_id=_jobId LIMIT 1;
	END //

-- Gets the maximum memory for the given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetMaxMemory;
CREATE PROCEDURE GetMaxMemory(IN _jobId INT) 
	BEGIN
		SELECT maximum_memory
		FROM job_pairs
		WHERE job_id=_jobId LIMIT 1;
	END //
	
DROP PROCEDURE IF EXISTS GetAllJobs;
CREATE PROCEDURE GetAllJobs()
	BEGIN
		SELECT 	id,
				user_id,
				name,
				queue_id,
				created,
				description,
				deleted,
				paused,
				killed,
				primary_space,
				GetJobStatus(id) 		AS status,
				GetTotalPairs(id) 		AS totalPairs,
				GetCompletePairs(id)	AS completePairs,
				GetPendingPairs(id)		AS pendingPairs,
				GetErrorPairs(id)		AS errorPairs
				
		FROM jobs;
	END //
	
DROP PROCEDURE IF EXISTS IsSystemPaused;
CREATE PROCEDURE IsSystemPaused()
	BEGIN
		SELECT paused
		FROM system_flags;
	END //
	
DROP PROCEDURE IF EXISTS GetUnRunnableJobs;
CREATE PROCEDURE GetUnRunnableJobs()
	BEGIN
		SELECT DISTINCT *
		FROM jobs
		WHERE queue_id = null
		OR id NOT IN (SELECT jobs.id 
					FROM jobs JOIN queue_assoc ON jobs.queue_id = queue_assoc.queue_id);
	END //
	
DELIMITER ; -- this should always be at the end of the file