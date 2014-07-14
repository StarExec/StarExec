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
DROP PROCEDURE IF EXISTS UpdatePairRunSolverStats;
CREATE PROCEDURE UpdatePairRunSolverStats(IN _jobPairId INT, IN _nodeName VARCHAR(64), IN _wallClock DOUBLE, IN _cpu DOUBLE, IN _userTime DOUBLE, IN _systemTime DOUBLE, IN _maxVmem DOUBLE, IN _maxResSet BIGINT, IN _pageReclaims BIGINT, IN _pageFaults BIGINT, IN _blockInput BIGINT, IN _blockOutput BIGINT, IN _volContexSwtch BIGINT, IN _involContexSwtch BIGINT)
	BEGIN
		UPDATE job_pairs
		SET node_id=(SELECT id FROM nodes WHERE name=_nodeName),
			wallclock = _wallClock,
			cpu=_cpu,
			user_time=_userTime,
			system_time=_systemTime,
			max_vmem=_maxVmem,
			max_res_set=_maxResSet,
			page_reclaims=_pageReclaims,
			page_faults=_pageFaults,
			block_input=_blockInput,
			block_output=_blockOutput,
			vol_contex_swtch=_volContexSwtch,
			invol_contex_swtch=_involContexSwtch
		WHERE id=_jobPairId;
	END //
	
-- Updates a job pairs node Id
-- Author: Wyatt	
DROP PROCEDURE IF EXISTS UpdateNodeId;
CREATE PROCEDURE UpdateNodeId(IN _jobPairId INT, IN _nodeName VARCHAR(128))
	BEGIN
		DECLARE _nodeId INT;

		SELECT id FROM nodes WHERE name=_nodeName INTO _nodeId;

                -- record an error for job pairs that we think are still running on this node (they should not be, with 1 sandbox only)
		UPDATE job_pairs SET status_code = 10 where node_id = _nodeID and (status_code=3 or status_code=4 or status_code=5);

		UPDATE job_pairs SET node_id=_nodeId WHERE id = _jobPairId;
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
				(select id from job_pairs WHERE job_id=(SELECT job_id FROM job_pairs WHERE job_pairs.id=_jobPairId) AND (statusCode<7 || statusCode>18) LIMIT 1) as theCount)=0 THEN
				UPDATE jobs SET completed=CURRENT_TIMESTAMP WHERE id=(SELECT job_id FROM job_pairs WHERE job_pairs.id=_jobPairId);
			END IF;
		END IF;
	END //
	
-- Gets the job pair with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairBySGE;
CREATE PROCEDURE GetJobPairBySGE(IN _Id INT)
	BEGIN
		SELECT *
		FROM job_pairs
		WHERE job_pairs.sge_id=_Id;
	END //
	
-- Gets the job pair with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetJobPairById;
CREATE PROCEDURE GetJobPairById(IN _Id INT)
	BEGIN
		SELECT *
		FROM job_pairs LEFT JOIN job_spaces AS jobSpace ON job_pairs.job_space_id=jobSpace.id
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
DELIMITER ; -- this should always be at the end of the file