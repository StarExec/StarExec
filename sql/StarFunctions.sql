-- Description: This file contains all stored functions for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!
-- Author: Todd Elvers

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

	
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
	
-- Returns the result of a job pair
-- Author: Eric Burns
DROP FUNCTION IF EXISTS GetJobPairExpectedResult;
CREATE FUNCTION GetJobPairtExpectedResult(_jobPairId INT)
	RETURNS VARCHAR(128)
	BEGIN
		DECLARE expected VARCHAR(128);
		
		SELECT attr_value INTO expected 
		FROM bench_attributes
		JOIN job_pairs ON job_pairs.bench_id=bench_attributes.bench_id
		WHERE job_pairs.id = _jobPairId 
		AND attr_key = "starexec-expected-result";
		
		RETURN IFNULL(expected, '--');
	END //
	
-- Gets the job ID of the job that contains the job pair with the given id
-- Author: Eric Burns
DROP FUNCTION IF EXISTS GetJobId;
CREATE FUNCTION GetJobId(_pairId INT)
	RETURNS INT
	BEGIN
		DECLARE jobId INT;
		
		SELECT job_id INTO jobId
		FROM job_pairs
		WHERE id=_pairid;
		
		RETURN jobId;
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

--  Tells you whether a space is public or not
--  returns the number of times public_user is in the space (should be 0 or 1)	
--  Author: Benton McCune + Eric Burns
DROP FUNCTION IF EXISTS IsPublic;
CREATE FUNCTION IsPublic(_spaceId int)
	RETURNS BOOLEAN
	BEGIN
		DECLARE isPublic BOOLEAN;
	  		select public_access INTO isPublic 
	  		from spaces
	  		where id = _spaceId;
	  	RETURN isPublic;	
	END //
	
--  Determines if User is Leader of Space
--  Author: Benton McCune
DROP FUNCTION IF EXISTS IsLeader;
CREATE FUNCTION IsLeader(_spaceId int, _userId int)
	RETURNS BOOLEAN
	BEGIN
		DECLARE isLeader BOOLEAN;
	  		select is_Leader INTO isLeader  
	  		from permissions 
	  		where id = (select permission from user_assoc where space_id=_spaceId and user_id = _userId LIMIT 1);
	  	RETURN isLeader;	
	END //	
	
DELIMITER ; -- This should always be at the end of this file