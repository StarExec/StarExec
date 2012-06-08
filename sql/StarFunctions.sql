-- Description: This file contains all stored functions for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!
-- Author: Todd Elvers

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Returns a benchmark type's description for a given benchmark id
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetBenchmarkTypeDescription;
CREATE FUNCTION GetBenchmarkTypeDescription(_benchTypeId INT)
	RETURNS TEXT
	BEGIN
		DECLARE benchTypeDescription TEXT;
		
		SELECT description INTO benchTypeDescription 
		FROM processors 
		WHERE id = _benchTypeId;
		
		RETURN benchTypeDescription;
	END //
	
	
-- Returns a benchmark type's name for a given benchmark id
-- Author: Todd Elvers 
DROP FUNCTION IF EXISTS GetBenchmarkTypeName;
CREATE FUNCTION GetBenchmarkTypeName(_benchTypeId INT)
	RETURNS VARCHAR(64)
	BEGIN
		DECLARE benchTypeName VARCHAR(64);
		
		SELECT name INTO benchTypeName 
		FROM processors 
		WHERE id = _benchTypeId;
		
		RETURN benchTypeName;
	END //
	
	
-- Gets the number of completed job pairs for a given job id
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetCompletePairs;
CREATE FUNCTION GetCompletePairs(_jobId INT) 
	RETURNS INT
	BEGIN
		DECLARE completePairs INT;
		
		SELECT COUNT(*) INTO completePairs 
		FROM job_pairs 
		WHERE job_id=_jobId 
		AND status_code=7;
		
		RETURN completePairs;
	END //
	
	
-- Gets the number of errored job pairs for a given job id
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetErrorPairs;
CREATE FUNCTION GetErrorPairs(_jobId INT) 
	RETURNS INT
	BEGIN
		DECLARE errorPairs INT;
		
		SELECT COUNT(*) INTO errorPairs 
		FROM job_pairs 
		WHERE job_id=_jobId 
		AND (status_code BETWEEN 8 AND 17 OR status_code=0);
		
		RETURN errorPairs;
	END //

-- Returns the result of a job pair
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetJobPairResult;
CREATE FUNCTION GetJobPairResult(_jobPairId INT)
	RETURNS VARCHAR(128)
	BEGIN
		DECLARE result VARCHAR(128);
		
		SELECT attr_value INTO result 
		FROM job_attributes 
		WHERE pair_id = _jobPairId 
		AND attr_key = "starexec-result";
		
		RETURN IFNULL(result, '--');
	END //
	
-- Returns "complete" if the job represented by the given id had no pending job pairs,
-- and returns "incomplete" otherwise
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetJobStatus;
CREATE FUNCTION GetJobStatus(_jobId INT) 
	RETURNS VARCHAR(10)
	BEGIN
		DECLARE status VARCHAR(10);
		
		IF(GetPendingPairs(_jobId) > 0) THEN
			SET status = "incomplete";
		ELSE
			SET status = "complete";
		END IF;
		
		RETURN status;
	END //
	
	
-- Gets the number of pending job pairs for a given job id
-- Author: Todd Elvers	
DROP FUNCTION IF EXISTS GetPendingPairs;
CREATE FUNCTION GetPendingPairs(_jobId INT) 
	RETURNS INT
	BEGIN
		DECLARE pendingPairs INT;
		
		SELECT COUNT(*) INTO pendingPairs 
		FROM job_pairs
		WHERE job_id=_jobId 
		AND (status_code BETWEEN 1 AND 6);
		
		RETURN pendingPairs;
	END //
	
	
-- Gets the total number of job pairs for a given job id
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetTotalPairs;
CREATE FUNCTION GetTotalPairs(_jobId INT) 
	RETURNS INT
	BEGIN
		DECLARE totalPairs INT;
		
		SELECT COUNT(*) INTO totalPairs 
		FROM job_pairs 
		WHERE job_id=_jobId;
		
		RETURN totalPairs;
	END //

	
-- Determines the wallclock time difference between two timestamps
-- and returns that in milliseconds
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetWallclock;
CREATE FUNCTION GetWallClock(start_time TIMESTAMP, end_time TIMESTAMP)
	RETURNS BIGINT
	BEGIN
		DECLARE wallclock BIGINT;
		SET wallclock = TIMESTAMPDIFF(MICROSECOND, start_time, end_time)/1000;
		RETURN wallclock;
	END//
	
DELIMITER ; -- This should always be at the end of this file