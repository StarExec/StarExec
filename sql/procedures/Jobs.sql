-- Description: This file contains all job-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

-- Adds an association between the given job and space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateJob //
CREATE PROCEDURE AssociateJob(IN _jobId INT, IN _spaceId INT)
	BEGIN
		INSERT IGNORE INTO job_assoc VALUES (_spaceId, _jobId);
	END //

-- 	Returns the number of public spaces the job is in
--  Author: Benton McCune
DROP PROCEDURE IF EXISTS JobInPublicSpace //
CREATE PROCEDURE JobInPublicSpace(IN _jobId INT)
	BEGIN
		SELECT COUNT(*) AS spaceCount FROM job_assoc
			INNER JOIN spaces ON spaces.id=job_assoc.space_id
		WHERE job_id=_jobId AND spaces.public_access=1;
	END //

-- Adds a new attribute to a job pair for the given stage
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJobAttr //
CREATE PROCEDURE AddJobAttr(IN _pairId INT, IN _key VARCHAR(128), IN _val VARCHAR(128), IN _stage INT)
	BEGIN
		REPLACE INTO job_attributes (pair_id,attr_key,attr_value,job_id,stage_number) VALUES (_pairId, _key, _val, (select job_id from job_pairs where id=_pairId),_stage);
	END //

-- Returns the number of jobs in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetJobCountBySpace //
CREATE PROCEDURE GetJobCountBySpace(IN _spaceId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM job_assoc
		WHERE _spaceId=space_id;
	END //
-- Returns the number of jobs in a given space that match a given query
-- Author: Eric burns
DROP PROCEDURE IF EXISTS GetJobCountBySpaceWithQuery //
CREATE PROCEDURE GetJobCountBySpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM job_assoc
			JOIN jobs AS jobs ON jobs.id=job_assoc.job_id
		WHERE _spaceId=job_assoc.space_id
		AND (jobs.name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(jobs.id)	LIKE	CONCAT('%', _query, '%'));
	END //

-- Returns the number of jobs pairs for a given job in the given job space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountInJobSpace //
CREATE PROCEDURE GetJobPairCountInJobSpace(IN _jobSpaceId INT, IN _stageNumber INT)
	BEGIN
		IF _stageNumber > 0 THEN
			SELECT COUNT(*) AS jobPairCount
			FROM job_pairs
			JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
			WHERE jobpair_stage_data.job_space_id=_jobSpaceId AND stage_number=_stageNumber;
		ELSE
			SELECT COUNT(*) AS jobPairCount
			FROM job_pairs
			JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
			WHERE jobpair_stage_data.job_space_id=_jobSpaceId AND stage_number=job_pairs.primary_jobpair_data;
		END IF;

	END //



-- Counts the number of pairs in a job with a completion index <= the given
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS CountOlderPairs //
CREATE PROCEDURE CountOlderPairs(IN _id INT, IN _since INT)
	BEGIN
		SELECT COUNT(*) AS count
		FROM job_pairs JOIN job_pair_completion ON id=pair_id
		WHERE completion_id<=_since and job_id=_id;
	END //

-- Returns the number of jobs pairs for a given job that match a given query for the given stage
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairCountByJobInJobSpaceWithQuery //
CREATE PROCEDURE GetJobPairCountByJobInJobSpaceWithQuery(IN _jobSpaceId INT, IN _query TEXT, IN _stageNumber INT)
	BEGIN
		IF _stageNumber>0 THEN
			SELECT COUNT(*) AS jobPairCount
			FROM job_pairs
			JOIN jobpair_stage_data ON (jobpair_stage_data.jobpair_id = job_pairs.id)
			WHERE jobpair_stage_data.job_space_id=_jobSpaceId AND stage_number = _stageNumber
			AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%'));
		ELSE
			SELECT COUNT(*) AS jobPairCount
			FROM job_pairs
			JOIN jobpair_stage_data ON (jobpair_stage_data.jobpair_id = job_pairs.id)
			WHERE jobpair_stage_data.job_space_id=_jobSpaceId AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data
			AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%'));

		END IF;

	END //


-- Gets attributes for every pair in a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobAttrs //
CREATE PROCEDURE GetJobAttrs(IN _jobId INT)
	BEGIN
		SELECT pair.id, attr.attr_key, attr.attr_value, attr.stage_number
		FROM job_pairs AS pair
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			WHERE pair.job_id=_jobId;
	END //

-- Gets the attributes for every job pair of a job completed after the given completion id
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewJobAttrs //
CREATE PROCEDURE GetNewJobAttrs(IN _jobId INT, IN _completionId INT)
	BEGIN
		SELECT pair.id, attr.attr_key, attr.attr_value, attr.stage_number
		FROM job_pairs AS pair
			LEFT JOIN job_attributes AS attr ON attr.pair_id=pair.id
			INNER JOIN job_pair_completion AS complete ON pair.id=complete.pair_id
			WHERE pair.job_id=_jobId AND complete.completion_id>_completionId;

	END //

-- Adds a new job stats record to the database
-- Author : Eric Burns
DROP PROCEDURE IF EXISTS AddJobStats //
CREATE PROCEDURE AddJobStats(IN _jobSpaceId INT, IN _configId INT, IN _complete INT, IN _correct INT, IN _incorrect INT, IN _failed INT, IN _conflicts INT, IN _wallclock DOUBLE, IN _cpu DOUBLE, IN _resource INT, IN _incomplete INT, IN _stage INT, IN _includeUnknown BOOLEAN)
	BEGIN
		INSERT IGNORE INTO job_stats (job_space_id, config_id, complete, correct, incorrect, failed, conflicts, wallclock,cpu,resource_out, incomplete, stage_number, include_unknowns)
		VALUES (_jobSpaceId, _configId, _complete, _correct, _incorrect, _failed, _conflicts, _wallclock, _cpu,_resource, _incomplete, _stage, _includeUnknown);
	END //

-- this version includes deleted configs; used to construct the solver summary table in the job space view
-- Alexander Brown, 9/20
DROP PROCEDURE IF EXISTS GetJobStatsInJobSpaceIncludeDeletedConfigs //
CREATE PROCEDURE GetJobStatsInJobSpaceIncludeDeletedConfigs(IN _jobSpaceId INT, IN _jobId INT, IN _stageNumber INT, IN _includeUnknown BOOLEAN)
BEGIN
SELECT *
FROM job_stats
JOIN configurations AS config ON config.id=job_stats.config_id
JOIN solvers AS solver ON solver.id=config.solver_id
LEFT JOIN anonymous_primitive_names AS anonymous_solver_names
	ON solver.id=anonymous_solver_names.primitive_id AND anonymous_solver_names.primitive_type="solver"
AND anonymous_solver_names.job_id=_jobId
LEFT JOIN anonymous_primitive_names AS anonymous_config_names
	ON config.id=anonymous_config_names.primitive_id AND anonymous_config_names.primitive_type="config"
AND anonymous_config_names.job_id=_jobId
WHERE job_stats.job_space_id = _jobSpaceId AND stage_number=_stageNumber AND include_unknowns=_includeUnknown;
END //

-- Clears the entire cache of job stats
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveAllJobStats //
CREATE PROCEDURE RemoveAllJobStats()
	BEGIN
		DELETE FROM job_stats;
	END //

-- Removes the cached job results for the hierarchy rooted at the given job space
-- Author: Eric Burns

DROP PROCEDURE IF EXISTS RemoveJobStatsInJobSpace //
CREATE PROCEDURE RemoveJobStatsInJobSpace(IN _jobSpaceId INT)
	BEGIN
		DELETE FROM job_stats
		WHERE job_stats.job_space_id = _jobSpaceId;
	END //

DROP PROCEDURE IF EXISTS RemoveJobStatsInJobSpaceForConfig //
CREATE PROCEDURE RemoveJobStatsInJobSpaceForConfig( IN _jobSpaceId INT, IN _configId INT )
	BEGIN
		DELETE FROM job_stats
		WHERE job_stats.job_space_id = _jobSpaceId
			AND job_stats.config_id = _configId;
	END //

-- Counts the number of pending pairs in a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS CountPendingPairs //
CREATE PROCEDURE CountPendingPairs(IN _jobId INT)
	BEGIN
		SELECT count(*) AS pending FROM job_pairs
		WHERE status_code BETWEEN 1 AND 6 AND job_id=_jobId;
	END //
-- Retrieves simple overall statistics for job pairs belonging to a job
-- Including the total number of pairs, how many are complete, pending or errored out
-- as well as how long the pairs ran
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairOverview //
CREATE PROCEDURE GetJobPairOverview(IN _jobId INT)
	BEGIN
		-- This is messy in order to get back pretty column names.
		-- Derived tables must have identifiers which is why a, b, c, d and e exist but aren't used
		SELECT * FROM (
			(SELECT total_pairs AS totalPairs FROM jobs WHERE id=_jobId) AS a, -- Gets the total number of pairs
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
DROP PROCEDURE IF EXISTS GetJobById //
CREATE PROCEDURE GetJobById(IN _id INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id = _id and deleted=false;
	END //

DROP PROCEDURE IF EXISTS SetHighPriority //
CREATE PROCEDURE SetHighPriority(IN _jobId INT, IN _isHighPriority BOOLEAN)
	BEGIN
		UPDATE jobs
		SET is_high_priority=_isHighPriority
		WHERE id=_jobId;
	END //


DROP PROCEDURE IF EXISTS GetOutputBenchmarksPath //
CREATE PROCEDURE GetOutputBenchmarksPath(IN _jobId INT)
	BEGIN
		SELECT output_benchmarks_directory_path
		FROM jobs
		WHERE id=_jobId;
	END //

DROP PROCEDURE IF EXISTS SetOutputBenchmarksPath //
CREATE PROCEDURE SetOutputBenchmarksPath(IN _jobId INT, IN _path TEXT)
	BEGIN
		UPDATE jobs
		SET output_benchmarks_directory_path=_path
		WHERE id=_jobId;
	END //

-- Retrieves basic info about a job from the jobs table
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobByIdIncludeDeleted //
CREATE PROCEDURE GetJobByIdIncludeDeleted(IN _id INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id = _id;
	END //

-- Retrieves basic info about job pairs for the given job id (simple version). Gets only the primary stage
-- Author: Julio Cervantes
DROP PROCEDURE IF EXISTS GetJobPairsByJobSimple //
CREATE PROCEDURE GetJobPairsByJobSimple(IN _id INT)
	BEGIN
		SELECT job_pairs.id, job_pairs.job_space_id, path, jobpair_stage_data.solver_name,jobpair_stage_data.solver_id,jobpair_stage_data.config_name,
		jobpair_stage_data.config_id,bench_name,bench_id,solver_pipelines.name,
		job_spaces.name,job_pairs.status_code,job_spaces.id, pipeline_stages.pipeline_id, jobpair_stage_data.stage_number
		FROM job_pairs
		JOIN job_spaces ON job_spaces.id=job_space_id
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
		LEFT JOIN pipeline_stages ON pipeline_stages.stage_id = jobpair_stage_data.stage_id
		LEFT JOIN solver_pipelines ON pipeline_stages.pipeline_id = solver_pipelines.id
		WHERE job_pairs.job_id=_id AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data;
	END //

-- Retrieves basic info about job pairs for the given job id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairsPrimaryStageByJob //
CREATE PROCEDURE GetJobPairsPrimaryStageByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs

									JOIN 	jobpair_stage_data AS jobpair_stage_data  ON jobpair_stage_data.jobpair_id=job_pairs.id
									JOIN	configurations	AS	config	ON	jobpair_stage_data.config_id = config.id
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
									LEFT JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id
									LEFT JOIN	job_spaces 		AS  jobSpace ON jobSpace.id=job_pairs.job_space_id

		WHERE job_pairs.job_id=_id AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data
		AND config.deleted = 0
		-- configs are no longer removed from the table, so only non-deleted ones should be selected
		ORDER BY job_pairs.end_time DESC;
	END //




-- Counts the entries in the job space closure table with the given ancestor and updates their last_used time
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RefreshEntriesByAncestor //
CREATE PROCEDURE RefreshEntriesByAncestor(IN _id INT, IN _time TIMESTAMP)
	BEGIN
		UPDATE job_space_closure
		SET last_used=_time
		WHERE ancestor=_id;

		SELECT COUNT(*) AS count
		FROM job_space_closure
		WHERE ancestor=_id;
	END //



-- Gets all the attribute values for benchmarks in the given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetAttrsOfNameForJob //
CREATE PROCEDURE GetAttrsOfNameForJob(IN _jobId INT, IN _attrName VARCHAR(128))
	BEGIN
		SELECT job_pairs.bench_id, attr_value
		FROM job_pairs JOIN bench_attributes ON job_pairs.bench_id = bench_attributes.bench_id

		WHERE attr_key=_attrName AND job_id=_jobId;
	END  //

-- Gets all the job pairs in a job space. No stages are retrieved
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsInJobSpace //
CREATE PROCEDURE GetJobPairsInJobSpace(IN _jobSpaceId INT, IN _jobId INT, IN _stageNumber INT)
	BEGIN
			SELECT job_pairs.status_code,
			job_pairs.id, job_pairs.bench_id, job_pairs.bench_name,
			completion_id, jobpair_stage_data.solver_id,jobpair_stage_data.solver_name, jobpair_stage_data.status_code,
			jobpair_stage_data.config_id,jobpair_stage_data.config_name,jobpair_stage_data.cpu,jobpair_stage_data.stage_id,
			jobpair_stage_data.wallclock, primary_jobpair_data, job_pairs.path,
			anonymous_solver_names.anonymous_name AS anon_solver_name,
			anonymous_config_names.anonymous_name AS anon_config_name,
			anonymous_bench_names.anonymous_name AS anon_bench_name,
			job_attributes.attr_value AS result
			FROM job_pairs
			JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
			LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id AND job_attributes.stage_number=jobpair_stage_data.stage_number and job_attributes.attr_key="starexec-result")
			LEFT JOIN job_pair_completion ON job_pairs.id=job_pair_completion.pair_id
			/* SPAGETT */
			LEFT JOIN anonymous_primitive_names AS anonymous_solver_names ON
						anonymous_solver_names.primitive_id=jobpair_stage_data.solver_id AND anonymous_solver_names.primitive_type="solver"
						AND anonymous_solver_names.job_id = _jobId
			LEFT JOIN anonymous_primitive_names AS anonymous_config_names ON
						anonymous_config_names.primitive_id=jobpair_stage_data.config_id AND anonymous_config_names.primitive_type="config"
						AND anonymous_config_names.job_id = _jobId
			LEFT JOIN anonymous_primitive_names AS anonymous_bench_names ON
						anonymous_bench_names.primitive_id=job_pairs.bench_id AND anonymous_bench_names.primitive_type="bench"
						AND anonymous_bench_names.job_id = _jobId
			WHERE jobpair_stage_data.job_space_id=_jobSpaceId AND
			(jobpair_stage_data.stage_number=_stageNumber OR (_stageNumber = 0 AND job_pairs.primary_jobpair_data=jobpair_stage_data.stage_number));

	END //

-- Gets all the job pairs in a job space hierarchy. No stages are retrieved
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsInJobSpaceHierarchy //
CREATE PROCEDURE GetJobPairsInJobSpaceHierarchy(IN _jobSpaceId INT, IN _since INT)
	BEGIN
		SELECT
		status_code,
		job_pairs.id,
		job_pairs.bench_id,
		job_pairs.bench_name,
		anonymous_primitive_names.anonymous_name AS anon_bench_name,
		job_pairs.path,
		completion_id,
		primary_jobpair_data
			FROM job_pairs
			JOIN job_spaces ON job_spaces.id = job_pairs.job_space_id
			LEFT JOIN anonymous_primitive_names ON
				anonymous_primitive_names.primitive_id=job_pairs.bench_id AND anonymous_primitive_names.primitive_type="bench"
						AND anonymous_primitive_names.job_id=job_spaces.job_id
			JOIN job_space_closure ON descendant=job_space_id
			LEFT JOIN job_pair_completion ON job_pairs.id=job_pair_completion.pair_id
			WHERE ancestor=_jobSpaceId AND ((_since is null) OR job_pair_completion.completion_id>_since);
	END //

-- Gets all the stages of job pairs in a particular job space
DROP PROCEDURE IF EXISTS GetJobPairStagesInJobSpace //
CREATE PROCEDURE GetJobPairStagesInJobSpace(IN _jobSpaceId INT)
	BEGIN
		SELECT job_pairs.id AS pair_id,jobpair_stage_data.solver_id,jobpair_stage_data.solver_name, jobpair_stage_data.status_code,
		jobpair_stage_data.config_id,jobpair_stage_data.config_name,jobpair_stage_data.cpu,jobpair_stage_data.stage_id,
		jobpair_stage_data.wallclock AS wallclock,job_pairs.id,
		job_attributes.attr_value AS result
		FROM job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id AND job_attributes.stage_number=jobpair_stage_data.stage_number and job_attributes.attr_key="starexec-result")
		WHERE job_space_id=_jobSpaceId;
	END //

-- Gets all the stages of job pairs in a particular job space
DROP PROCEDURE IF EXISTS GetJobPairStagesInJobSpaceHierarchy //
CREATE PROCEDURE GetJobPairStagesInJobSpaceHierarchy(IN _jobSpaceId INT, IN _since INT)
	BEGIN
		SELECT
		job_pairs.id AS pair_id,
		jobpair_stage_data.solver_id,
		jobpair_stage_data.solver_name,
		jobpair_stage_data.status_code,
		jobpair_stage_data.config_id,
		jobpair_stage_data.config_name,
		jobpair_stage_data.cpu,
		jobpair_stage_data.stage_id,
		jobpair_stage_data.wallclock AS wallclock,
		job_pairs.id, jobpair_stage_data.stage_number,
		jobpair_stage_data.max_vmem,
		bench_attributes.attr_value AS expected,
		job_attributes.attr_value AS result,
		anonymous_solver_names.anonymous_name AS anon_solver_name,
		anonymous_config_names.anonymous_name AS anon_config_name
			FROM job_pairs
			JOIN job_spaces ON job_spaces.id=job_pairs.job_space_id
			JOIN job_space_closure ON descendant=job_space_id
			JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
			LEFT JOIN anonymous_primitive_names AS anonymous_solver_names ON
						anonymous_solver_names.primitive_id=jobpair_stage_data.solver_id AND anonymous_solver_names.primitive_type="solver"
						AND anonymous_solver_names.job_id = job_spaces.job_id
			LEFT JOIN anonymous_primitive_names AS anonymous_config_names ON
						anonymous_config_names.primitive_id=jobpair_stage_data.config_id AND anonymous_config_names.primitive_type="config"
						AND anonymous_config_names.job_id = job_spaces.job_id
			LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id AND job_attributes.stage_number=jobpair_stage_data.stage_number and job_attributes.attr_key="starexec-result")
			LEFT JOIN job_pair_completion ON job_pairs.id=job_pair_completion.pair_id

			LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
			WHERE ancestor=_jobSpaceId AND ((_since is null) OR job_pair_completion.completion_id>_since);
	END //

-- Counts the number of pairs in a job
-- Author Eric Burns
DROP PROCEDURE IF EXISTS countPairsForJob //
CREATE PROCEDURE countPairsForJob(IN _id INT)
	BEGIN
		SELECT COUNT(*) AS count
		FROM job_pairs
		WHERE job_id=_id;
	END //

DROP PROCEDURE IF EXISTS GetAllJobPairsByJob //
CREATE PROCEDURE GetAllJobPairsByJob(IN _id INT)
	BEGIN
		SELECT *
		FROM job_pairs
						JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
						JOIN	configurations	AS	config	ON	jobpair_stage_data.config_id = config.id
						JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
						JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
						LEFT JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id
					    LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.job_id=_id AND job_pairs.primary_jobpair_data=jobpair_stage_data.stage_number
		AND config.deleted = 0;
		-- configs are no longer removed from the table, so only non-deleted ones should be selected
	END //

-- Retrieves basic info about job pairs for the given job id for pairs completed after _completionId
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewCompletedJobPairsByJob //
CREATE PROCEDURE GetNewCompletedJobPairsByJob(IN _id INT, IN _completionId INT)
	BEGIN
		SELECT *
		FROM job_pairs
						JOIN job_pair_completion AS complete ON job_pairs.id=complete.pair_id
						JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
						JOIN	configurations	AS	config	ON	jobpair_stage_data.config_id = config.id
						JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
						JOIN	solvers			AS	solver	ON	config.solver_id = solver.id
						LEFT JOIN	nodes 			AS node 	ON  job_pairs.node_id=node.id
					    LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.job_id=_id AND complete.completion_id>_completionId AND job_pairs.primary_jobpair_data=jobpair_stage_data.stage_number
		AND config.deleted = 0
		-- configs are no longer removed from the table, so only non-deleted ones should be selected
		ORDER BY job_pairs.end_time DESC;
	END //


-- Retrieves ids for job pairs with a given status in a given job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobPairsByStatus //
CREATE PROCEDURE GetJobPairsByStatus(IN _jobId INT, IN _statusCode INT)
	BEGIN
		SELECT id FROM job_pairs
		WHERE job_id=_jobId AND status_code=_statusCode ORDER BY id ASC;
	END //

-- Retrieves ids for job pairs in a given job where either cpu or wallclock is 0 for any stage that has the given status code
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetTimelessJobPairsByStatus //
CREATE PROCEDURE GetTimelessJobPairsByStatus(IN _jobId INT, IN _statusCode INT)
	BEGIN
		SELECT DISTINCT job_pairs.id FROM job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_id=_jobId AND jobpair_stage_data.status_code=_statusCode AND (jobpair_stage_data.cpu=0 OR jobpair_stage_data.wallclock=0);
	END //

-- Retrieves information for pending job pairs with the given job id. Returns all stages for _limit pairs.
-- Excludes any job pairs that are utilizing solvers that have still not been built
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetPendingJobPairsByJob //
CREATE PROCEDURE GetPendingJobPairsByJob(IN _id INT, IN _limit INT)
	BEGIN
		SELECT *,
		(SELECT count(*) FROM bench_dependency WHERE primary_bench_id = benchmarks.id) AS dependency_count
		FROM job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
		LEFT JOIN benchmarks ON benchmarks.id = job_pairs.bench_id
		LEFT JOIN solvers ON solvers.id = jobpair_stage_data.solver_id
		JOIN (SELECT DISTINCT job_pairs.id FROM job_pairs FORCE INDEX (job_id_2)
		WHERE job_id = _id AND job_pairs.status_code = 1
		AND NOT EXISTS (SELECT 1 FROM jobpair_stage_data
		LEFT JOIN solvers ON solvers.id = jobpair_stage_data.solver_id
		JOIN job_pairs AS jp ON jp.id=jobpair_id
		JOIN jobs ON jobs.id=jp.job_id
		WHERE jobpair_stage_data.jobpair_id = job_pairs.id AND solvers.build_status=0 AND buildJob=false)
		ORDER BY job_pairs.id ASC LIMIT _limit) AS temp
		ON temp.id=job_pairs.id;
	END //

-- Retrieves basic info about enqueued job pairs for the given job id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEnqueuedJobPairsByJob //
CREATE PROCEDURE GetEnqueuedJobPairsByJob(IN _id INT)
	BEGIN
		SELECT job_pairs.id,job_pairs.sge_id
		FROM job_pairs
		WHERE (job_id = _id AND status_code = 2)
		ORDER BY sge_id ASC;
	END //

-- Retrieves basic info about running job pairs for the given job id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRunningJobPairsByJob //
CREATE PROCEDURE GetRunningJobPairsByJob(IN _id INT)
	BEGIN
		SELECT job_pairs.id, job_pairs.sge_id
		FROM job_pairs
		WHERE (job_id = _id AND status_code = 4)
		ORDER BY sge_id ASC;
	END //

-- Returns true if the job in question has the deleted flag set as true
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsJobDeleted //
CREATE PROCEDURE IsJobDeleted(IN _jobId INT)
	BEGIN
		SELECT count(*) AS jobDeleted
		FROM jobs
		WHERE deleted=true AND id=_jobId;
	END //



-- Returns the paused and deleted columns for a job
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsJobPausedOrKilled //
CREATE PROCEDURE IsJobPausedOrKilled(IN _jobId INT)
	BEGIN
		SELECT paused,killed
		FROM jobs
		WHERE id=_jobId;
	END //


-- Sets the "deleted" property of a job to true
-- Also updates the total_pairs and disk_size columns to 0
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS DeleteJob //
CREATE PROCEDURE DeleteJob(IN _jobId INT)
	BEGIN
		UPDATE users JOIN jobs ON jobs.user_id=users.id
		SET users.disk_size=users.disk_size-jobs.disk_size
		WHERE jobs.id=_jobId;

		UPDATE jobs
		SET deleted=true, total_pairs=0, disk_size=0
		WHERE id = _jobId;
	END //

DROP PROCEDURE IF EXISTS UpdateJobDiskSize //
CREATE PROCEDURE UpdateJobDiskSize(IN _jobId INT, IN _diskSize BIGINT)
	BEGIN
		UPDATE users JOIN jobs ON jobs.user_id=users.id
		SET users.disk_size=(users.disk_size-jobs.disk_size)+_diskSize
		WHERE jobs.id=_jobId;


		UPDATE jobs
		SET disk_size=_diskSize
		WHERE id=_jobId;
	END //

-- Deletes every job pair belonging to the given job
DROP PROCEDURE IF EXISTS DeleteAllJobPairsInJob //
CREATE PROCEDURE DeleteAllJobPairsInJob(IN _jobId INT)
	BEGIN
		DELETE FROM job_pairs
		WHERE job_id=_jobId;
	END //

DROP PROCEDURE IF EXISTS GetOrphanedJobIds //
CREATE PROCEDURE GetOrphanedJobIds(IN _userId INT)
	BEGIN
		SELECT jobs.id FROM jobs
		LEFT JOIN job_assoc ON job_assoc.job_id=jobs.id
		WHERE jobs.user_id=_userId AND job_assoc.space_id IS NULL;
	END //

-- Sets the "paused" property of a job to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS PauseJob //
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
DROP PROCEDURE IF EXISTS PauseAll //
CREATE PROCEDURE PauseAll()
	BEGIN
		UPDATE system_flags SET paused = true;
	END //

-- Sets the "paused" property of a job to false
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ResumeJob //
CREATE PROCEDURE ResumeJob(IN _jobId INT)
	BEGIN
		UPDATE jobs
		SET paused=false
		WHERE id = _jobId;

		UPDATE job_pairs
		JOIN jobpair_stage_data on job_pairs.id=jobpair_stage_data.jobpair_id
		SET job_pairs.status_code = 1, jobpair_stage_data.status_code=1
		WHERE job_id = _jobId AND job_pairs.status_code = 20;
	END //

-- sets the global paused flag to false
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS ResumeAll //
CREATE PROCEDURE ResumeAll()
	BEGIN
		UPDATE system_flags SET paused = false;
	END //

-- Sets the "killed" property of a job to true
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS KillJob //
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
DROP PROCEDURE IF EXISTS ChangeQueue //
CREATE PROCEDURE ChangeQueue(IN _jobId INT, IN _queueId INT)
	BEGIN
		UPDATE jobs
		SET queue_id = _queueId
		WHERE id = _jobId;
	END //

-- Adds a new job pair record to the database
-- Author: Tyler Jensen + Eric Burns
DROP PROCEDURE IF EXISTS AddJobPair //
CREATE PROCEDURE AddJobPair(IN _jobId INT, IN _benchId INT, IN _status TINYINT, IN _path VARCHAR(2048),IN _jobSpaceId INT, IN _benchName VARCHAR(256), IN _stageNumber INT, OUT _id INT)
	BEGIN
		INSERT INTO job_pairs (job_id, bench_id, status_code, path,job_space_id,bench_name,primary_jobpair_data)
		VALUES (_jobId, _benchId, _status, _path, _jobSpaceId,  _benchName,_stageNumber);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

DROP PROCEDURE IF EXISTS AddJobPairStage //
CREATE PROCEDURE AddJobPairStage(IN _pairId INT, IN _stageId INT,IN _stageNumber INT, IN _primary BOOLEAN, IN _solverId INT, IN _solverName VARCHAR(255), IN _configId INT, IN _configName VARCHAR (255), IN _jobSpace INT)
	BEGIN
		INSERT INTO jobpair_stage_data (jobpair_id, stage_id,stage_number,solver_id,solver_name,config_id,config_name,job_space_id,status_code, disk_size)
		VALUES (_pairId, _stageId,_stageNumber,_solverId,_solverName,_configId,_configName, _jobSpace,1,0);
	END //

-- Adds a new job record to the database
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddJob //
CREATE PROCEDURE AddJob(
		IN _userId INT,
		IN _name VARCHAR(64),
		IN _desc TEXT,
		IN _queueId INT,
		IN _spaceId INT,
		IN _seed BIGINT,
		IN _cpu INT,
		IN _wall INT,
		IN _softTimeLimit INT,
		in _killDelay INT,
		IN _mem BIGINT,
		IN _suppressTimestamp BOOLEAN,
		IN _usingDeps INT,
		IN _buildJob BOOLEAN,
		IN _totalPairs INT,
		IN _benchmarkingFramework ENUM('BENCHEXEC', 'RUNSOLVER'),
		OUT _id INT)
	BEGIN
		INSERT INTO jobs (user_id, name, description, queue_id, primary_space,
				seed, cpuTimeout, clockTimeout, maximum_memory, paused,
				suppress_timestamp, using_dependencies, buildJob, total_pairs,
				soft_time_limit, kill_delay, disk_size, benchmarking_framework)
		VALUES (_userId, _name, _desc, _queueId, _spaceId,
				_seed, _cpu, _wall, _mem, true,
				_suppressTimestamp, _usingDeps, _buildJob, _totalPairs,
				_softTimeLimit, _killDelay, 0, _benchmarkingFramework);
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Retrieves all jobs belonging to a user (but not their job pairs)
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS GetUserJobsById //
CREATE PROCEDURE GetUserJobsById(IN _userId INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE user_id=_userId and deleted=false
		ORDER BY created DESC;
	END //

DROP PROCEDURE IF EXISTS GetQueueJobsById //
CREATE PROCEDURE GetQueueJobsById(IN _queueId INT)
	BEGIN
		SELECT *,
			total_pairs AS totalPairs,
			GetCompletePairs(id) AS completePairs,
			GetPendingPairs(id)  AS pendingPairs,
			GetErrorPairs(id)    AS errorPairs
		FROM jobs
		WHERE queue_id=_queueId
		  AND id IN
			(SELECT distinct job_id FROM job_pairs WHERE status_code BETWEEN 1 AND 6)
		  AND NOT paused
		  AND NOT killed
		ORDER BY created DESC;
	END //

-- Returns the number of jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetJobCount //
CREATE PROCEDURE GetJobCount()
	BEGIN
		SELECT COUNT(*) as jobCount
		FROM jobs;
	END //

-- Returns the number of running jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRunningJobCount //
CREATE PROCEDURE GetRunningJobCount()
	BEGIN
		SELECT COUNT(distinct jobs.id) as jobCount
		FROM jobs
		JOIN    job_pairs ON jobs.id = job_pairs.job_id
		WHERE 	job_pairs.status_code < 7;
	END //

-- Returns the number of paused jobs in the entire system
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetPausedJobCount //
CREATE PROCEDURE GetPausedJobCount()
	BEGIN
		SELECT COUNT(distinct jobs.id) as jobCount
		FROM jobs
		JOIN    job_pairs ON jobs.id = job_pairs.job_id
		WHERE 	job_pairs.status_code = 20;
	END //

-- Get the total count of the jobs belong to a specific user
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS GetJobCountByUser //
CREATE PROCEDURE GetJobCountByUser(IN _userId INT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE user_id = _userId and deleted=false;
	END //

-- Returns the number of jobs in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetJobCountByUserWithQuery //
CREATE PROCEDURE GetJobCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS jobCount
		FROM jobs
		WHERE user_id=_userId AND deleted=false AND
				(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'));
	END //
DROP PROCEDURE IF EXISTS GetNameofJobById //
CREATE PROCEDURE GetNameofJobById(IN _jobId INT)
	BEGIN
		SELECT name
		FROM jobs
		where id = _jobId and deleted=false;
	END //

-- Sets the primary space of a job to a new space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS UpdatePrimarySpace //
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
DROP PROCEDURE IF EXISTS SetNewColumns //
CREATE PROCEDURE SetNewColumns()
	BEGIN
		UPDATE job_pairs
			JOIN benchmarks AS bench ON bench.id=bench_id
			JOIN configurations AS config ON config.id=config_id
			JOIN solvers AS solve ON solve.id=config.solver_id
			SET bench_name=bench.name, solver_name=solve.name, config_name=config.name, job_pairs.solver_id=solve.id;
	END //

-- Gets back only the fields of a job pair that are necessary to determine where it is stored on disk
-- Gets pairs that have either completed after the given completionID or are still running
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNewJobPairFilePathInfoByJob //
CREATE PROCEDURE GetNewJobPairFilePathInfoByJob(IN _jobID INT, IN _completionID INT)
	BEGIN
		SELECT path,solver_name,config_name,bench_name,job_pairs.status_code,complete.completion_id, id, primary_jobpair_data FROM job_pairs
			LEFT JOIN job_pair_completion AS complete ON job_pairs.id=complete.pair_id
			JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobID AND (complete.completion_id>_completionId OR job_pairs.status_code=4)
		AND job_pairs.primary_jobpair_data=jobpair_stage_data.stage_number;
	END //



DROP PROCEDURE IF EXISTS RemovePairsFromComplete //
CREATE PROCEDURE RemovePairsFromComplete(IN _jobId INT)
	BEGIN
		DELETE job_pair_completion FROM job_pair_completion
		JOIN job_pairs ON job_pairs.id=job_pair_completion.pair_id
		WHERE job_id=_jobId;
	END //

-- Sets all the pairs of a given job to the given status
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetPairsToStatus //
CREATE PROCEDURE SetPairsToStatus(IN _jobId INT, In _statusCode INT)
	BEGIN
		UPDATE job_pairs
		SET status_code = _statusCode
		WHERE job_id = _jobId;
	END //


-- Sets all the pairs of a given job and status to the given status
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetPairsOfStatusToStatus //
CREATE PROCEDURE SetPairsOfStatusToStatus(IN _jobId INT, IN _newCode INT, IN _curCode INT)
	BEGIN
		UPDATE job_pairs
		SET status_code = _newCode
		WHERE job_id = _jobId AND status_code=_curCode;
	END //

-- Removes all jobs in the database that are deleted and also orphaned. Runs periodically.
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetDeletedJobs //
CREATE PROCEDURE GetDeletedJobs()
	BEGIN
		SELECT * FROM jobs WHERE deleted = true;
	END //

DROP PROCEDURE IF EXISTS GetJobsAssociatedWithSpaces //
CREATE PROCEDURE GetJobsAssociatedWithSpaces()
	BEGIN
		SELECT DISTINCT job_id AS ID FROM job_assoc;
	END //

-- Gives back the number of pairs with the given status
DROP PROCEDURE IF EXISTS CountPairsByStatusByJob //
CREATE PROCEDURE CountPairsByStatusByJob(IN _jobId INT, IN _status INT)
	BEGIN
		SELECT COUNT(*) AS count
		FROM job_pairs
		WHERE job_pairs.job_id=_jobId and _status=status_code;
	END //


-- Gives back the number of pairs with the given status
DROP PROCEDURE IF EXISTS CountTimelessPairsByStatusByJob //
CREATE PROCEDURE CountTimelessPairsByStatusByJob(IN _jobId INT, IN _status INT)
	BEGIN
		SELECT COUNT(distinct job_pairs.id) AS count
		FROM job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId and _status=jobpair_stage_data.status_code AND (jobpair_stage_data.wallclock=0 OR jobpair_stage_data.cpu=0);
	END //

-- For a given job, sets every pair at the complete status to the processing status, and also changes the post_processor
-- of the job to the given one
-- Choosing the primary stage is not allowed here-- an actual stage number must be supplied
DROP PROCEDURE IF EXISTS PrepareJobForPostProcessing //
CREATE PROCEDURE PrepareJobForPostProcessing(IN _jobId INT, IN _procId INT, IN _completeStatus INT, IN _processingStatus INT, IN _stageNumber INT)
	BEGIN


		UPDATE job_pairs
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id=job_pairs.id
		SET job_pairs.status_code=_processingStatus,
		jobpair_stage_data.status_code=_processingStatus
		WHERE job_id=_jobId AND job_pairs.status_code=_completeStatus
		AND jobpair_stage_data.status_code=_completeStatus AND
		(jobpair_stage_data.stage_number=_stageNumber);

	-- makes sure there is actually an entry in job_stage_params for this job / stage pair.
	INSERT IGNORE INTO job_stage_params (job_id,stage_number,cpuTimeout,clockTimeout,maximum_memory,space_id,post_processor,pre_processor)
	VALUES (_jobId, _stageNumber,(select cpuTimeout from jobs where jobs.id=_jobId),(select clockTimeout from jobs where jobs.id=_jobId),
	(select maximum_memory from jobs where jobs.id=_jobId), null, _procId,null);

	UPDATE job_stage_params SET post_processor = _procId WHERE job_id=_jobId AND stage_number=_stageNumber;

	END //

DROP PROCEDURE IF EXISTS SetJobStageParams //
CREATE PROCEDURE SetJobStageParams(IN _jobId INT, IN _stage INT, IN _cpu INT, IN _clock INT, IN _mem BIGINT,
IN _space INT, IN _postProc INT, IN _preProc INT, IN _suffix VARCHAR(64), IN _resultsInterval INT, IN _stdoutSave INT, IN _extraSave INT)
	BEGIN
		INSERT INTO job_stage_params (job_id, stage_number,cpuTimeout,clockTimeout,maximum_memory,
		space_id, post_processor, pre_processor, bench_suffix, results_interval, stdout_save_option, extra_output_save_option)
		VALUES (_jobId, _stage,_cpu,_clock,_mem,_space,_postProc,_preProc, _suffix, _resultsInterval, _stdoutSave, _extraSave);
	END //

-- Gets every incomplete Job
DROP PROCEDURE IF EXISTS GetIncompleteJobs //
CREATE PROCEDURE GetIncompleteJobs()
	BEGIN
		SELECT *,
			total_pairs AS totalPairs,
			GetCompletePairs(id) AS completePairs,
			GetPendingPairs(id)  AS pendingPairs,
			GetErrorPairs(id)    AS errorPairs
		FROM jobs
		WHERE GetJobStatus(id)="incomplete" OR paused=true
	;
	END //

-- Gets the ID of every job that is currently running (has incomplete pairs and
-- is not already paused / killed)
DROP PROCEDURE IF EXISTS GetRunningJobs //
CREATE PROCEDURE GetRunningJobs()
	BEGIN
		SELECT id FROM (
		SELECT id, GetJobStatus(id) AS status
		FROM jobs
		WHERE paused=false AND killed=false) AS temp
		WHERE status="incomplete";
	END //

DROP PROCEDURE IF EXISTS GetRunningJobsByUser //
CREATE PROCEDURE GetRunningJobsByUser(IN _userId INT)
	BEGIN
		SELECT id FROM (
		SELECT id, GetJobStatus(id) AS status
		FROM jobs
		WHERE paused=false AND killed=false AND user_id=_userId) AS temp
		WHERE status="incomplete";
	END //

DROP PROCEDURE IF EXISTS SetJobName //
CREATE PROCEDURE SetJobName(IN _jobId INT, IN _newName VARCHAR(64))
	BEGIN
		UPDATE jobs
		SET name = _newName
		WHERE id = _jobId;
	END //

DROP PROCEDURE IF EXISTS SetJobDescription //
CREATE PROCEDURE SetJobDescription(IN _jobId INT, IN _newDescription TEXT)
	BEGIN
		UPDATE jobs
		SET description = _newDescription
		WHERE id = _jobId;
	END //

-- Checks to see if there is a global pause on all jobs
DROP PROCEDURE IF EXISTS IsSystemPaused //
CREATE PROCEDURE IsSystemPaused()
	BEGIN
		SELECT paused
		FROM system_flags;
	END //

-- Permanently removes a job from the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveJobFromDatabase //
CREATE PROCEDURE RemoveJobFromDatabase(IN _jobId INT)
	BEGIN
		DELETE FROM jobs WHERE id=_jobId;
	END //

-- Gets all entries in the job_stage_params table referencing the given job
DROP PROCEDURE IF EXISTS getStageParamsByJob //
CREATE PROCEDURE getStageParamsByJob(IN _jobId INT)
	BEGIN
		SELECT * FROM job_stage_params WHERE job_id=_jobId;
	END //

-- Gets all benchmark inputs for all pairs in the given job
DROP PROCEDURE IF EXISTS GetAllJobPairBenchmarkInputsByJob //
CREATE PROCEDURE GetAllJobPairBenchmarkInputsByJob(IN _jobId INT)
	BEGIN
		SELECT jobpair_inputs.jobpair_id,jobpair_inputs.bench_id
		FROM jobpair_inputs JOIN job_pairs ON job_pairs.id=jobpair_inputs.jobpair_id
		WHERE job_pairs.job_id=_jobId ORDER BY input_number ASC;
	END //

DROP PROCEDURE IF EXISTS GetAllJobIds //
CREATE PROCEDURE GetAllJobIds()
	BEGIN
		SELECT id FROM jobs;
	END //

DROP PROCEDURE IF EXISTS CountPairsByUser //
CREATE PROCEDURE CountPairsByUser(IN _userId INT)
	BEGIN
		SELECT SUM(total_pairs) AS total_pairs FROM jobs WHERE user_id=_userId AND deleted=false;
	END //

DROP PROCEDURE IF EXISTS IncrementTotalJobPairsForJob //
CREATE PROCEDURE IncrementTotalJobPairsForJob(IN _jobId INT, IN _increment INT)
	BEGIN
		UPDATE jobs SET total_pairs=total_pairs+_increment WHERE id=_jobId;
	END //

DROP PROCEDURE IF EXISTS DoesJobCopyBackIncrementally //
CREATE PROCEDURE DoesJobCopyBackIncrementally(IN _jobId INT, OUT _jobCopiesBackIncrementally BOOLEAN)
    BEGIN
        SELECT (COUNT(*) <> 0) INTO _jobCopiesBackIncrementally
        FROM job_stage_params
        WHERE results_interval <> 0 and _jobId = job_id;
    END //

DROP PROCEDURE IF EXISTS GetJobAttributesTableHeaders //
CREATE PROCEDURE GetJobAttributesTableHeaders(IN _jobSpaceId INT)
    BEGIN
        SELECT ja.attr_value
        FROM job_attributes ja INNER JOIN job_pairs jp
            ON ja.pair_id=jp.id
        WHERE ja.attr_key = "starexec-result" AND jp.job_space_id=_jobSpaceId
        GROUP BY attr_value
				ORDER BY attr_value;
    END //

DROP PROCEDURE IF EXISTS GetJobAttributesTable //
CREATE PROCEDURE GetJobAttributesTable(IN _jobSpaceId INT)
    BEGIN
        SELECT solver_id, solver_name, config_id, config_name, attr_value, COUNT(attr_value) attr_count,
					SUM(wallclock) wallclock_sum, SUM(cpu) cpu_sum
        FROM job_attributes ja JOIN job_pairs jp ON ja.pair_id=jp.id
            JOIN jobpair_stage_data jsd ON jp.id = jsd.jobpair_id
        WHERE ja.attr_key = 'starexec-result' AND jp.job_space_id=_jobSpaceId
        GROUP BY solver_id, config_id, attr_value;
    END //

DROP PROCEDURE IF EXISTS GetSumOfJobAttributes //
CREATE PROCEDURE GetSumOfJobAttributes(IN _jobSpaceId INT)
    BEGIN
        SELECT attr_value, COUNT(attr_value) attr_count, SUM(wallclock) wallclock, SUM(cpu) cpu
        FROM job_attributes ja JOIN job_pairs jp ON ja.pair_id=jp.id
            JOIN jobpair_stage_data jsd ON jp.id=jsd.jobpair_id
        WHERE ja.attr_key='starexec-result' AND jp.job_space_id=_jobSpaceId
        GROUP BY attr_value
				ORDER BY attr_value;
    END //
