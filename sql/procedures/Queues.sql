USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Retrieves all jobs with pending job pairs for the given queue
-- Author: Benton McCune and Aaron Stump
DROP PROCEDURE IF EXISTS GetPendingJobs;
CREATE PROCEDURE GetPendingJobs(IN _queueId INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id in (select distinct job_id from job_pairs where status_code=1 and queue_id = _queueId);
	END //
	
-- Retrieves all jobs with enqueued job pairs for the given queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEnqueuedJobs;
CREATE PROCEDURE GetEnqueuedJobs(IN _queueId INT)
	BEGIN
		SELECT *
		FROM jobs
		WHERE id in (select distinct job_id from job_pairs where status_code=2 and queue_id = _queueId);
	END //
	
	
-- Retrieves the number of jobs with pending job pairs for the given queue
-- Author: Benton McCune and Aaron Stump
DROP PROCEDURE IF EXISTS GetNumEnqueuedJobs;
CREATE PROCEDURE GetNumEnqueuedJobs(IN _queueId INT)
	BEGIN
		SELECT COUNT(*) AS count FROM job_pairs JOIN jobs ON job_pairs.job_id = jobs.id
                WHERE job_pairs.status_code=2 AND jobs.queue_id = _queueId;
	END //	
	
-- Retrieves basic info about enqueued job pairs for the given queue id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetEnqueuedJobPairsByQueue;
CREATE PROCEDURE GetEnqueuedJobPairsByQueue(IN _id INT, IN _cap INT)
	BEGIN
		SELECT *
		FROM job_pairs
			-- Where the job_pair is running on the input Queue
			INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
		WHERE (enqueued.queue_id = _id AND status_code = 2)
		ORDER BY job_pairs.sge_id ASC
		LIMIT _cap;
	END //
	
-- Retrieves basic info about running job pairs for the given node id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetRunningJobPairsByQueue;
CREATE PROCEDURE GetRunningJobPairsByQueue(IN _id INT, IN _cap INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE node_id = _id AND (status_code = 4 OR status_code = 3)
		ORDER BY sge_id ASC
		LIMIT _cap;
	END //	

-- Gets the next page of job pairs running on a given node for a datatable
	
DROP PROCEDURE IF EXISTS GetNextPageOfRunningJobPairs;
CREATE PROCEDURE GetNextPageOfRunningJobPairs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _id INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of JobPairs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF (_sortASC = TRUE) THEN
				SELECT *
				FROM job_pairs
				WHERE node_id = _id AND (status_code = 4 OR status_code = 3)
				ORDER BY sge_id ASC
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT *
				FROM job_pairs
				WHERE node_id = _id AND (status_code = 4 OR status_code = 3)
				ORDER BY sge_id ASC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF (_sortASC = TRUE) THEN
				SELECT *
				FROM job_pairs
				WHERE node_id = _id AND (status_code = 4 OR status_code = 3)
				ORDER BY sge_id ASC
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT *
				FROM job_pairs
				WHERE node_id = _id AND (status_code = 4 OR status_code = 3)
				ORDER BY sge_id ASC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
-- Gets the next page of job pairs enqueued in a given queue for a datatable
DROP PROCEDURE IF EXISTS GetNextPageOfEnqueuedJobPairs;
CREATE PROCEDURE GetNextPageOfEnqueuedJobPairs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _id INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of JobPairs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF (_sortASC = TRUE) THEN
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND status_code = 2)
					ORDER BY job_pairs.sge_id ASC
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND status_code = 2)
					ORDER BY job_pairs.sge_id ASC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF (_sortASC = TRUE) THEN
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND status_code = 2)
					ORDER BY job_pairs.sge_id ASC
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND status_code = 2)
					ORDER BY job_pairs.sge_id ASC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
DELIMITER ; -- This should always be at the end of this file