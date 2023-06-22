-- Adds a new queue given a name
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddQueue //
CREATE PROCEDURE AddQueue(IN _name VARCHAR(128),IN _wall INT, IN _cpu INT, OUT id INT)
	BEGIN
		INSERT IGNORE INTO queues (name,clockTimeout,cpuTimeout, status)
		VALUES (_name,_wall,_cpu, "INACTIVE");
		SELECT LAST_INSERT_ID() INTO id;
	END //

-- Remove a queue given its id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS RemoveQueue //
CREATE PROCEDURE RemoveQueue(IN _queueId INT)
	BEGIN
		DELETE FROM queues
		WHERE id = _queueId;
	END //

-- Retrieves the id of a queue given its name
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetIdByName //
CREATE PROCEDURE GetIdByName(IN _queueName VARCHAR(64))
	BEGIN
		SELECT id
		FROM queues
		WHERE name = _queueName;
	END //


-- Retrieves all jobs with pending job pairs for the given queue
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetPendingJobs //
CREATE PROCEDURE GetPendingJobs(IN _queueId INT)
	BEGIN
		SELECT distinct jobs.*
		FROM jobs WHERE queue_id = _queueId
		AND EXISTS (select 1 from job_pairs FORCE INDEX (job_id_2) WHERE status_code=1 and job_id=jobs.id);
	END //

-- Retrieves all pending job pairs for a give queue owned by a developer
DROP PROCEDURE IF EXISTS GetPendingDeveloperJobs //
CREATE PROCEDURE GetPendingDeveloperJobs(IN _queueId INT)
    BEGIN
        SELECT DISTINCT jobs.*
        FROM users u
        INNER JOIN user_roles ur
            ON u.email = ur.email
        INNER JOIN jobs
            ON jobs.user_id = u.id
        WHERE ur.role = 'developer' OR ur.role = 'admin' AND queue_id = _queueId
        AND EXISTS (select 1 from job_pairs FORCE INDEX (job_id_2) WHERE status_code=1 and job_id=jobs.id);
    END //

-- Retrieves the number of enqueued job pairs for the given queue
-- Author: Benton McCune and Aaron Stump
DROP PROCEDURE IF EXISTS GetNumEnqueuedJobs //
CREATE PROCEDURE GetNumEnqueuedJobs(IN _queueId INT)
	BEGIN
		SELECT COUNT(*) AS count FROM job_pairs JOIN jobs ON job_pairs.job_id = jobs.id
                WHERE job_pairs.status_code=2 AND jobs.queue_id = _queueId;
	END //


-- Gets the sum of wallclock timeouts for all
DROP PROCEDURE IF EXISTS GetUserLoadOnQueue //
CREATE PROCEDURE GetUserLoadOnQueue(IN _queueId INT, IN _user INT)
	BEGIN
		SELECT SUM(jobs.clockTimeout) AS queue_load FROM job_pairs JOIN jobs ON job_pairs.job_id = jobs.id
                WHERE (job_pairs.status_code=4 OR job_pairs.status_code=2)
                AND jobs.queue_id = _queueId AND jobs.user_id=_user;
	END //

-- Retrieves basic info about enqueued job pairs for the given queue id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetCountOfEnqueuedJobPairsByQueue //
CREATE PROCEDURE GetCountOfEnqueuedJobPairsByQueue(IN _id INT)
	BEGIN
		SELECT count(*) AS count
		FROM job_pairs
			-- Where the job_pair is running on the input Queue
			INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
		WHERE enqueued.queue_id = _id AND job_pairs.status_code = 2;
	END //

-- Get the name of a queue given its id
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNameById //
CREATE PROCEDURE GetNameById(IN _queueId INT)
	BEGIN
		SELECT name
		FROM queues
		WHERE id = _queueId;
	END //

-- Updates the max wallclock timeout for a queue
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS UpdateQueueClockTimeout //
CREATE PROCEDURE UpdateQueueClockTimeout(IN _queueId INT, IN _timeout INT)
	BEGIN
		UPDATE queues
		SET clockTimeout=_timeout
		WHERE id=_queueId;
	END //

-- Updates the max cpu timeout for a queue
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS UpdateQueueCpuTimeout //
CREATE PROCEDURE UpdateQueueCpuTimeout(IN _queueId INT, IN _timeout INT)
	BEGIN
		UPDATE queues
		SET cpuTimeout=_timeout
		WHERE id=_queueId;
	END //

-- Determines if the queue has global access
-- Author: Wyatt kaiser
DROP PROCEDURE IF EXISTS IsQueueGlobal //
CREATE PROCEDURE IsQueueGlobal (IN _queueId INT)
	BEGIN
		SELECT global_access
		FROM queues
		WHERE id = _queueId;
	END //

-- Removes a queue's association with a space
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS RemoveQueueAssociation //
CREATE PROCEDURE RemoveQueueAssociation(IN _queueId INT)
	BEGIN
		DELETE FROM comm_queue
		WHERE queue_id = _queueId;
	END //

-- Make a queue have global access
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS MakeQueueGlobal //
CREATE PROCEDURE MakeQueueGlobal(IN _queueId INT)
	BEGIN
		UPDATE queues
		SET global_access = true
		WHERE id = _queueId;

		DELETE FROM comm_queue
		WHERE queue_id = _queueId;
	END //

-- remove global access from a queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS RemoveQueueGlobal //
CREATE PROCEDURE RemoveQueueGlobal(IN _queueId INT)
	BEGIN
		UPDATE queues
		SET global_access = false
		WHERE id = _queueId;
	END //

-- Sets the test queue in the database to a new value
DROP PROCEDURE IF EXISTS SetTestQueue //
CREATE PROCEDURE SetTestQueue(IN _qid INT)
	BEGIN
		UPDATE system_flags SET test_queue=_qid;
	END //
-- Gets the ID of the queue for running test jobs on solver uploads
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetTestQueue //
CREATE PROCEDURE GetTestQueue()
	BEGIN
		SELECT test_queue FROM system_flags;
	END //

-- Give the community (leaders) Access to a queue
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS SetQueueCommunityAccess //
CREATE PROCEDURE SetQueueCommunityAccess(IN _communityId INT, IN _queueId INT)
	BEGIN
		INSERT INTO comm_queue
		VALUES (_communityId, _queueId);
	END //


DROP PROCEDURE IF EXISTS GetPairsRunningOnNode //
CREATE PROCEDURE GetPairsRunningOnNode(IN _nodeId INT)
	BEGIN
		SELECT job_pairs.id,
			   job_pairs.path,
			   job_pairs.primary_jobpair_data,
			   job_pairs.job_id,
			   job_pairs.bench_id,
			   job_pairs.bench_name,
			   job_pairs.queuesub_time,
			   jobpair_stage_data.solver_id,
			   jobpair_stage_data.solver_name,
			   jobpair_stage_data.config_id,
			   jobpair_stage_data.config_name,
			   jobs.id,
			   jobs.name,
			   users.id,
			   users.first_name,
			   users.last_name
		FROM job_pairs
		JOIN jobs ON jobs.id = job_pairs.job_id
		JOIN users ON users.id = jobs.user_id
		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id

		WHERE node_id = _nodeId AND (job_pairs.status_code = 4 OR job_pairs.status_code = 3) AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data;
	END //


-- Gets all of the queues that the given user is allowed to use
DROP PROCEDURE IF EXISTS GetQueuesForUser //
CREATE PROCEDURE GetQueuesForUser(IN _userID INT)
	BEGIN
		SELECT DISTINCT id, name, status, global_access, cpuTimeout,clockTimeout
		FROM queues
			LEFT JOIN comm_queue ON queues.id = comm_queue.queue_id
		WHERE
			queues.status = "ACTIVE"
			AND (
				(IsLeader(comm_queue.space_id, _userId) = 1)	-- Either you are the leader of the community it was given access to
				OR
				(global_access)							-- or it is a global queue
				);
	END //


DROP PROCEDURE IF EXISTS GetDescForQueue //
CREATE PROCEDURE GetDescForQueue(IN _qID INT)
	BEGIN
		SELECT description 
		FROM queues 
		WHERE id = _qID;
	END //

DROP PROCEDURE IF EXISTS SetDescForQueue //
CREATE PROCEDURE SetDescForQueue(IN _qID INT, IN _desc VARCHAR(200)) 
	BEGIN
		UPDATE queues
		SET description = _desc
		WHERE id = _qid;
	END //