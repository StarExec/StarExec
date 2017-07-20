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
		AND (status_code BETWEEN 8 AND 17 OR status_code=0 OR status_code=24);

		RETURN errorPairs;
	END //

-- Returns "complete" if the job represented by the given id had no pending job pairs,
-- and returns "incomplete" otherwise
-- Author: Todd Elvers
DROP FUNCTION IF EXISTS GetJobStatus;
CREATE FUNCTION GetJobStatus(_jobId INT)
	RETURNS ENUM("incomplete", "complete")
	BEGIN
		DECLARE status ENUM("incomplete", "complete");

		SELECT IF (
			_jobId IN (
				SELECT job_id
				FROM job_pairs
				WHERE status_code BETWEEN 1 AND 6
			),
			"incomplete",
			"complete")
		INTO status;

		RETURN status;
	END //

-- Returns human readable description of this job's status
-- This Function looks intimidating, but it is just a big IF ELSE IF chain
DROP FUNCTION IF EXISTS GetJobStatusDetail;
CREATE FUNCTION GetJobStatusDetail(_jobId INT)
	RETURNS ENUM("RUNNING", "PROCESSING", "COMPLETE", "DELETED", "KILLED", "PAUSED", "GLOBAL_PAUSE")
	BEGIN
		DECLARE status ENUM("RUNNING", "PROCESSING", "COMPLETE", "DELETED", "KILLED", "PAUSED", "GLOBAL_PAUSE");

		SELECT
			IF ( _jobId IN ( SELECT id FROM jobs WHERE deleted ), "DELETED",
			IF ( _jobId IN ( SELECT id FROM jobs WHERE killed  ), "KILLED",
			IF ( _jobId IN ( SELECT id FROM jobs WHERE paused  ), "PAUSED",
			IF ( _jobId IN ( SELECT id FROM job_pairs WHERE status_code=22), "PROCESSING",
			IF ( _jobId IN ( SELECT id FROM job_pairs WHERE status_code BETWEEN 1 AND 6),
				IF ( TRUE IN (SELECT paused FROM system_flags)
					AND _jobId NOT IN (
						SELECT jobs.id
						FROM jobs
						JOIN users ON user_id=users.id
						JOIN user_roles ON user_roles.email=users.email
						WHERE (role = "admin" OR role = "developer")
					),
					"GLOBAL_PAUSE",
					"RUNNING"
				),
			"COMPLETE"
			)))))
		INTO status;

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

--  Tells you whether a space is public or not
-- Author: Eric Burns
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
