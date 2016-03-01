-- Description: This file contains all community stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Checks to see if the space with the given space ID is a community.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS IsCommunity;
CREATE PROCEDURE IsCommunity(IN _spaceId INT)
	BEGIN
		SELECT *
		FROM set_assoc
		WHERE space_id = 1 AND child_id = _spaceId;
	END //

-- Returns basic space information for the community with the given id
-- This ensures security by preventing malicious users from getting details about ANY space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetCommunityById;
CREATE PROCEDURE GetCommunityById(IN _id INT)
	BEGIN
		SELECT *
		FROM set_assoc
			JOIN spaces AS space ON space.id=set_assoc.child_id
		WHERE _id=child_id AND space_id=1;
	END //

-- Removes the association a user has with a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS LeaveSpace;
CREATE PROCEDURE LeaveSpace(IN _userId INT, IN _spaceId INT)
	BEGIN
		-- Remove the permission associated with this user/space
		DELETE FROM permissions
			WHERE id=(SELECT permission FROM user_assoc WHERE user_id = _userId	AND space_id = _spaceId);
		
		-- Delete the association	
		DELETE FROM user_assoc
		WHERE user_id = _userId
		AND space_id = _spaceId;
	END //
	

	
-- Removes every association a user has with every space in the hierarchy rooted at the given spacew
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS LeaveHierarchy;
CREATE PROCEDURE LeaveHierarchy(IN _userId INT, IN _spaceId INT)
	BEGIN
		DELETE user_assoc FROM user_assoc
		JOIN closure ON closure.descendant=user_assoc.space_id
		WHERE closure.ancestor=_spaceId AND user_id=_userId;
	END //

DROP PROCEDURE IF EXISTS GetCommunityStatsUsers;
CREATE PROCEDURE GetCommunityStatsUsers()
	BEGIN

		   SELECT community_assoc.comm_id, COUNT(DISTINCT user_assoc.user_id) AS userCount
		   FROM community_assoc
		   JOIN user_assoc
		   ON community_assoc.space_id=user_assoc.space_id
		   GROUP BY community_assoc.comm_id;

	END //

DROP PROCEDURE IF EXISTS GetCommunityStatsSolvers;
CREATE PROCEDURE GetCommunityStatsSolvers()
	BEGIN

		   SELECT comm_id, COUNT(DISTINCT solverId) as solverCount, SUM(solverDiskSize) as solverDiskUsage
		   FROM (SELECT DISTINCT community_assoc.comm_id,solvers.id as solverId, solvers.disk_size as solverDiskSize
		   FROM community_assoc JOIN solver_assoc On solver_assoc.space_id=community_assoc.space_id JOIN solvers ON solvers.id=solver_assoc.solver_id) as commStatSolver
		   GROUP BY comm_id;


	END //

DROP PROCEDURE IF EXISTS GetCommunityStatsBenches;
CREATE PROCEDURE GetCommunityStatsBenches()
	BEGIN

		   SELECT comm_id, COUNT(DISTINCT benchId) as benchCount, SUM(benchDiskSize) AS benchDiskUsage
		   FROM (SELECT DISTINCT comm_id,benchmarks.id as benchId, benchmarks.disk_size as benchDiskSize
		   FROM community_assoc JOIN bench_assoc On bench_assoc.space_id=community_assoc.space_id JOIN benchmarks ON benchmarks.id=bench_assoc.bench_id) as commStatBench
		   GROUP BY comm_id;



	END //

DROP PROCEDURE IF EXISTS GetCommunityStatsJobs;
CREATE PROCEDURE GetCommunityStatsJobs()
	BEGIN

		   SELECT community_assoc.comm_id, COUNT(DISTINCT job_pairs.job_id) AS jobCount, COUNT(DISTINCT job_pairs.id) AS jobPairCount
		   FROM community_assoc JOIN job_assoc ON job_assoc.space_id=community_assoc.space_id JOIN job_pairs ON job_pairs.job_id=job_assoc.job_id 
		   WHERE job_pairs.status_code IN (7,14,15,16,17)
		   GROUP BY community_assoc.comm_id;


	END //

DELIMITER ; -- This should always be at the end of this file