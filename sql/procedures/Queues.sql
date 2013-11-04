USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Adds a new queue given a name
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddQueue;
CREATE PROCEDURE AddQueue(IN _name VARCHAR(128), OUT id INT)
	BEGIN		
		INSERT INTO queues (name, status)
		VALUES (_name, "INACTIVE");
		SELECT LAST_INSERT_ID() INTO id;
	END //
	
-- Remove a queue given its id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS RemoveQueue;
CREATE PROCEDURE RemoveQueue(IN _queueId INT)
	BEGIN
		DELETE FROM queues
		WHERE id = _queueId;
	END //

-- Retrieves the id of a queue given its name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetIdByName;
CREATE PROCEDURE GetIdByName(IN _queueName VARCHAR(126))
	BEGIN
		SELECT id
		FROM queues
		WHERE name = _queueName;
	END //

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
	
-- Count the number of queues in a specific space with a specific name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS countQueueName;
CREATE PROCEDURE countQueueName(IN _name VARCHAR(128))
	BEGIN
		SELECT COUNT(*) FROM Queues WHERE name = _name;		
	END //
	
DELIMITER ; -- This should always be at the end of this file