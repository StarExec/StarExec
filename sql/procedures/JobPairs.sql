-- Description: This file contains all job-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement
	
DROP PROCEDURE IF EXISTS UpdateJobPairStatus;
CREATE PROCEDURE UpdateJobPairStatus(IN _pairId INT, IN _statusCode INT)
	BEGIN
		UPDATE job_pairs
		SET status_code = _statusCode
		WHERE id = _pairId;
	END //
	
DROP PROCEDURE IF EXISTS UpdateJobSpaceId;
CREATE PROCEDURE UpdateJobSpaceId(IN _pairId INT, IN _jobSpaceId INT)
	BEGIN
		UPDATE job_pairs
		SET job_space_id = _jobSpaceId
		WHERE id = _pairId;
	END //
	
	
-- Updates a job pair's statistics directly from the execution node
-- Author: Benton McCune
-- TODO: This needs to be modified to work by stage! Not converting yet to avoid messing with the job scripts
DROP PROCEDURE IF EXISTS UpdatePairRunSolverStats;
CREATE PROCEDURE UpdatePairRunSolverStats(IN _jobPairId INT, IN _nodeName VARCHAR(64), IN _wallClock DOUBLE, IN _cpu DOUBLE, IN _userTime DOUBLE, IN _systemTime DOUBLE, IN _maxVmem DOUBLE, IN _maxResSet BIGINT)
	BEGIN
		UPDATE job_pairs SET node_id=(SELECT id FROM nodes WHERE name=_nodeName) WHERE id=_jobPairId;
		UPDATE jobline_stage_data
		SET wallclock = _wallClock,
			cpu=_cpu,
			user_time=_userTime,
			system_time=_systemTime,
			max_vmem=_maxVmem,
			max_res_set=_maxResSet
		WHERE jobline_id=_jobPairId;
	END //
	
-- Updates a job pairs node Id
-- Author: Wyatt	
DROP PROCEDURE IF EXISTS UpdateNodeId;
CREATE PROCEDURE UpdateNodeId(IN _jobPairId INT, IN _nodeName VARCHAR(128), IN _sandbox INT)
	BEGIN
		DECLARE _nodeId INT;

		SELECT id FROM nodes WHERE name=_nodeName INTO _nodeId;

		UPDATE job_pairs SET node_id=_nodeId WHERE id = _jobPairId;
		UPDATE job_pairs SET sandbox_num=_sandbox WHERE id=_jobPairId;
		
		-- Next lines finish a pair that is still in the "running" state despite another pair being in the same place now
		-- First, mark the end time of the pairs
		UPDATE job_pairs SET end_time=NOW() WHERE node_id = _nodeID AND status_code = 4 AND id!=_jobPairId AND sandbox_num=_sandbox;
		-- Then, update the stuck pairs to an error code
		UPDATE job_pairs SET status_code = 10 WHERE node_id = _nodeID AND status_code = 4 AND id!=_jobPairId AND sandbox_num=_sandbox;
	END //
	
-- Updates a job pair's status
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdatePairStatus;
CREATE PROCEDURE UpdatePairStatus(IN _jobPairId INT, IN _statusCode TINYINT)
	BEGIN
		UPDATE job_pairs
		SET status_code=_statusCode
		WHERE id=_jobPairId ;
		IF (_statusCode>6 AND _statusCode<19) THEN
			

			
			REPLACE INTO job_pair_completion (pair_id) VALUES (_jobPairId); 
			-- this checks to see if the job is done and sets its completion id if so.
			-- It checks by trying to find exactly 1 pair (for efficiency) that is not yet complete
			IF (SELECT COUNT(*) FROM 
				(select id from job_pairs WHERE job_id=(SELECT job_id FROM job_pairs WHERE job_pairs.id=_jobPairId) AND (status_code<7 || status_code>18) LIMIT 1) as theCount)=0 THEN
				UPDATE jobs SET completed=CURRENT_TIMESTAMP WHERE id=(SELECT job_id FROM job_pairs WHERE job_pairs.id=_jobPairId);
			END IF;
		END IF;
	END //
		
-- Gets all the stages for the given job pair
DROP PROCEDURE IF EXISTS GetJobPairStagesById;
CREATE PROCEDURE GetJobPairStagesById( IN _id INT)
	BEGIN
		SELECT *
		FROM jobline_stage_data
		LEFT JOIN pipeline_stages ON pipeline_stages.stage_id=jobline_stage_data.stage_id
		WHERE jobline_id=_id
		ORDER BY jobline_stage_data.stage_id ASC;
	END //
-- Gets the job pair with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairById;
CREATE PROCEDURE GetJobPairById(IN _Id INT)
	BEGIN
		SELECT *
		FROM job_pairs 
		LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
		WHERE job_pairs.id=_Id;
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
	
-- Updates a job pair's sge id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS SetSGEJobId;
CREATE PROCEDURE SetSGEJobId(IN _jobPairId INT, IN _sgeId INT)
	BEGIN
		UPDATE job_pairs
		SET sge_id=_sgeId
		WHERE id=_jobPairId;
	END //
	
DROP PROCEDURE IF EXISTS GetAllPairsShallow;
CREATE PROCEDURE GetAllPairsShallow()
	BEGIN
		SELECT job_id,path,solver_name,config_name,bench_name,jobs.user_id FROM job_pairs
			JOIN jobs ON jobs.id=job_pairs.job_id;
	END //
	
-- Gets back only the fields of a job pair that are necessary to determine where it is stored on disk
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS GetJobPairFilePathInfo;
CREATE PROCEDURE GetJobPairFilePathInfo(IN _pairId INT)
	BEGIN
		SELECT job_id,path,solver_name,config_name,bench_name FROM job_pairs
		WHERE job_pairs.id=_pairId;
	END //
	
-- Gets every pair_id and processor_id for pairs awiting processing
DROP PROCEDURE IF EXISTS GetPairsToBeProcessed;
CREATE PROCEDURE GetPairsToBeProcessed(IN _processingStatus INT)
	BEGIN
		SELECT post_processor ,job_pairs.id AS id 
		FROM job_pairs JOIN jobs ON job_pairs.job_id=jobs.id
		WHERE status_code=_processingStatus;
	END //
	
DROP PROCEDURE IF EXISTS RemovePairFromCompletedTable;
CREATE PROCEDURE RemovePairFromCompletedTable(IN _id INT)
	BEGIN
		DELETE FROM job_pair_completion
		WHERE pair_id=_id;
	END //
	
-- Sets the queue submission time to now for the pair with the given id
DROP PROCEDURE IF EXISTS SetQueueSubTime;
CREATE PROCEDURE SetQueueSubTime(IN _id INT)
BEGIN
	UPDATE job_pairs SET queuesub_time=NOW() WHERE id=_id;
END //

-- Sets the queue submission time to now (the moment this is called) for the pair with the given id
DROP PROCEDURE IF EXISTS SetPairStartTime;
CREATE PROCEDURE SetPairStartTime(IN _id INT)
	BEGIN
		UPDATE job_pairs SET start_time=NOW() WHERE id=_id;
	END //
	
-- Sets the completion time to now (the moment this is called) for the pair with the given id

DROP PROCEDURE IF EXISTS SetPairEndTime;
CREATE PROCEDURE SetPairEndTime(IN _id INT)
	BEGIN
		UPDATE job_pairs SET end_time=NOW() WHERE id=_id;
	END //
	
-- Counts the number of pairs with the given status code that completed in within the given
-- number of days
DROP PROCEDURE IF EXISTS CountRecentPairsByStatus;
CREATE PROCEDURE CountRecentPairsByStatus(IN _status INT, IN _days INT)
	BEGIN
		SELECT count(*) FROM job_pairs WHERE status_code=_status AND 
		
		status_code=_status;-- end_time BETWEEN DATE_SUB(NOW(), INTERVAL _days DAY) AND NOW();
	END //
	 
	
DELIMITER ; -- this should always be at the end of the file