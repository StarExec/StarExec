-- Description: This file contains all solver-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a solver and returns the solver ID
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolver;
CREATE PROCEDURE AddSolver(IN _userId INT, IN _name VARCHAR(128), IN _downloadable BOOLEAN, IN _path TEXT, IN _description TEXT, OUT _id INT, IN _diskSize BIGINT, IN _type INT, IN _build_status INT)
	BEGIN
		INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size, executable_type, build_status)
		VALUES (_userId, _name, SYSDATE(), _path, _description, _downloadable, _diskSize, _type, _build_status);
		
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Gets all solvers that reside in public spaces
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetPublicSolvers;
CREATE PROCEDURE GetPublicSolvers()
	BEGIN
		SELECT * from solvers
		JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		JOIN spaces ON spaces.id=solver_assoc.space_id
		where public_access=1 AND deleted=false AND recycled=false
		GROUP BY(solvers.id);
	END //

	
	
-- Adds a Space/Solver association
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolverAssociation;
CREATE PROCEDURE AddSolverAssociation(IN _spaceId INT, IN _solverId INT)
	BEGIN
		INSERT IGNORE INTO solver_assoc VALUES (_spaceId, _solverId);
	END // 
	
-- Adds a run configuration to the specified solver
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddConfiguration;
CREATE PROCEDURE AddConfiguration(IN _solverId INT, IN _name VARCHAR(128), IN _description TEXT, IN _time TIMESTAMP, OUT configId INT)
	BEGIN
		INSERT INTO configurations (solver_id, name, description, updated)
		VALUES (_solverId, _name, _description, _time);
		
		SELECT LAST_INSERT_ID() INTO configId;
	END //
	

-- Deletes a configuration given that configuration's id
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteConfigurationById;
CREATE PROCEDURE DeleteConfigurationById(IN _configId INT)
	BEGIN
		DELETE FROM configurations
		WHERE id = _configId;
	END //	
	
	
-- Deletes a solver given that solver's id
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS SetSolverToDeletedById;
CREATE PROCEDURE SetSolverToDeletedById(IN _solverId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM solvers WHERE id = _solverId;
		UPDATE solvers
		SET deleted=true
		WHERE id = _solverId;
		UPDATE solvers
		SET disk_size=0
		WHERE id = _solverId;		
	END //	
	
-- Gets the IDs of all the spaces associated with the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetAssociatedSpaceIdsBySolver;
CREATE PROCEDURE GetAssociatedSpaceIdsBySolver(IN _solverId INT) 
	BEGIN
		SELECT space_id
		FROM solver_assoc
		WHERE solver_id=_solverId;
	END //
	
-- Retrieves the configurations with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfiguration;
CREATE PROCEDURE GetConfiguration(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE id = _id;
	END //

DROP PROCEDURE IF EXISTS GetAllSolversInJob;
CREATE PROCEDURE GetAllSolversInJob(IN _jobId INT)
	BEGIN
		SELECT DISTINCT solver_id, solver_name
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //

DROP PROCEDURE IF EXISTS GetAllConfigsInJob;
CREATE PROCEDURE GetAllConfigsInJob(IN _jobId INT)
	BEGIN
		SELECT DISTINCT config_id, config_name
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //

DROP PROCEDURE IF EXISTS GetAllConfigIdsInJob;
CREATE PROCEDURE GetAllConfigIdsInJob( IN _jobId INT)
	BEGIN
		SELECT DISTINCT config_id
		FROM jobpair_stage_data
		INNER JOIN job_pairs ON jobpair_stage_data.jobpair_id=job_pairs.id
		WHERE job_pairs.job_id=_jobId;
	END //
	
	
-- Retrieves the configurations that belong to a solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetConfigsForSolver;
CREATE PROCEDURE GetConfigsForSolver(IN _id INT)
	BEGIN
		SELECT *
		FROM configurations
		WHERE solver_id = _id;
	END //




	
-- Retrieves all solvers belonging to a space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSpaceSolversById;
CREATE PROCEDURE GetSpaceSolversById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		WHERE deleted=false AND recycled=false AND solver_assoc.space_id=_id;
	END //	
	
-- Retrieves the solver associated with the configuration with the given id
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS GetSolverIdByConfigId;
CREATE PROCEDURE GetSolverIdByConfigId(IN _id INT)
	BEGIN
		SELECT solver_id AS id
		FROM configurations
		WHERE id=_id;
	END //
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverById;
CREATE PROCEDURE GetSolverById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id and deleted=false AND recycled=false;
	END //
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverByIdIncludeDeleted;
CREATE PROCEDURE GetSolverByIdIncludeDeleted(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id = _id;
	END //
	
-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS GetSolverCountInSpaceWithQuery;
CREATE PROCEDURE GetSolverCountInSpaceWithQuery(IN _spaceId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solver_assoc
			JOIN solvers AS solvers ON solvers.id=solver_assoc.solver_id
		WHERE _spaceId=solver_assoc.space_id AND
				(solvers.name 	LIKE	CONCAT('%', _query, '%')
				OR		solvers.description	LIKE 	CONCAT('%', _query, '%'));
	END //
	
	
	
-- Retrieves the solvers owned by a given user id
-- Todd Elvers
DROP PROCEDURE IF EXISTS GetSolversByOwner;
CREATE PROCEDURE GetSolversByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE user_id = _userId and deleted=false AND recycled=false;
	END //

-- Returns the number of public spaces a solver is in
-- Benton McCune
DROP PROCEDURE IF EXISTS IsSolverPublic;
CREATE PROCEDURE IsSolverPublic(IN _solverId INT)
	BEGIN
		SELECT count(*) as solverPublic
		FROM solver_assoc
		WHERE solver_id = _solverId
		AND IsPublic(space_id);
	END //
	
DROP PROCEDURE IF EXISTS IsSolverDeleted;
CREATE PROCEDURE IsSolverDeleted(IN _solverId INT)
	BEGIN
		SELECT count(*) AS solverDeleted
		FROM solvers
		WHERE deleted=true AND id=_solverId;
	END //
-- Removes the association between a solver and a given space;
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS RemoveSolverFromSpace;
CREATE PROCEDURE RemoveSolverFromSpace(IN _solverId INT, IN _spaceId INT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM solver_assoc
			WHERE solver_id = _solverId
			AND space_id = _spaceId;
		END IF;
	END // 

-- Updates the disk_size attribute of a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDiskSize;
CREATE PROCEDURE UpdateSolverDiskSize(IN _solverId INT, IN _newDiskSize BIGINT)
	BEGIN
		UPDATE solvers
		SET disk_size = _newDiskSize
		WHERE id = _solverId;
	END //

-- Updates the details associated with a given configuration
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateConfigurationDetails;
CREATE PROCEDURE UpdateConfigurationDetails(IN _configId INT, IN _name VARCHAR(128), IN _description TEXT, IN _time TIMESTAMP)
	BEGIN
		UPDATE configurations
		SET name = _name,
		description = _description,
		updated = _time
		WHERE id = _configId;
	END //
	
	
-- Updates the details associated with a given solver
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateSolverDetails;
CREATE PROCEDURE UpdateSolverDetails(IN _solverId INT, IN _name VARCHAR(128), IN _description TEXT, IN _downloadable BOOLEAN)
	BEGIN
		UPDATE solvers
		SET name = _name,
		description = _description,
		downloadable = _downloadable
		WHERE id = _solverId;
	END //
	
-- Get the total count of the solvers belong to a specific user
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetSolverCountByUser;
CREATE PROCEDURE GetSolverCountByUser(IN _userId INT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE user_id = _userId AND deleted=false AND recycled=false;
	END //
	
-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolverCountByUserWithQuery;
CREATE PROCEDURE GetSolverCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE user_id=_userId AND deleted=false AND recycled=false AND
				(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%'));
	END //

-- Sets the recycled attribute to the given value for the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetSolverRecycledValue;
CREATE PROCEDURE SetSolverRecycledValue(IN _solverId INT, IN _recycled BOOLEAN)
	BEGIN
		UPDATE solvers
		SET recycled=_recycled
		WHERE id=_solverId;
	END //
	
-- Checks to see whether the "recycled" flag is set for the given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS IsSolverRecycled;
CREATE PROCEDURE IsSolverRecycled(IN _solverId INT)
	BEGIN
		SELECT recycled FROM solvers
		WHERE id=_solverId;
	END //
	
-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns	
DROP PROCEDURE IF EXISTS GetRecycledSolverCountByUser;
CREATE PROCEDURE GetRecycledSolverCountByUser(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE solvers.user_id=_userId AND recycled=true AND deleted=false AND
				(solvers.name 	LIKE	CONCAT('%', _query, '%')
				OR		solvers.description	LIKE 	CONCAT('%', _query, '%'));
	END //
	
-- Gets the path to every recycled solver a user has
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetRecycledSolverPaths;
CREATE PROCEDURE GetRecycledSolverPaths(IN _userId INT)
	BEGIN
		SELECT path,id FROM solvers
		WHERE recycled=true AND user_id=_userId AND deleted=false;
	END //

-- Sets all the solvers the user has in the database to "deleted"
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS SetRecycledSolversToDeleted;
CREATE PROCEDURE SetRecycledSolversToDeleted(IN _userId INT) 
	BEGIN
		UPDATE solvers
		SET deleted=true, disk_size=0
		WHERE user_id = _userId AND recycled=true;
	END //
	
-- Gets all recycled solver ids a user has in the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetRecycledSolverIds;
CREATE PROCEDURE GetRecycledSolverIds(IN _userId INT) 
	BEGIN
		SELECT id FROM solvers
		WHERE user_id=_userId AND recycled=true;
	END //
	
-- Permanently removes a solver from the database
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RemoveSolverFromDatabase;
CREATE PROCEDURE RemoveSolverFromDatabase(IN _id INT)
	BEGIN
		DELETE FROM solvers
		WHERE id=_id;
	END //
	
-- Gets all the solver ids of solvers that are in at least one space
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversAssociatedWithSpaces;
CREATE PROCEDURE GetSolversAssociatedWithSpaces()
	BEGIN
		SELECT DISTINCT solver_id AS id FROM solver_assoc;
	END //

-- Gets the solver ids of all solvers associated with at least one pair
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversAssociatedWithPairs;
CREATE PROCEDURE GetSolversAssociatedWithPairs()
	BEGIN
		SELECT DISTINCT solver_id AS id from jobpair_stage_data;
	END //
	
-- Gets the solver ids of all deleted solvers
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetDeletedSolvers;
CREATE PROCEDURE GetDeletedSolvers()
	BEGIN	
		SELECT id FROM solvers WHERE deleted=true;
	END //
-- Sets the recycled flag for a single solver back to false
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS RestoreSolver;
CREATE PROCEDURE RestoreSolver(IN _solverId INT)
	BEGIN
		UPDATE solvers
		SET recycled=false
		WHERE _solverId=id;
	END //

-- Gets the timestamp of the configuration that was most recently added or updated
-- on this solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetMaxConfigTimestamp;
CREATE PROCEDURE GetMaxConfigTimestamp(IN _solverId INT)
	BEGIN
		SELECT MAX(updated) AS recent
		FROM configurations
		WHERE configurations.solver_id=_solverId;
	END //
	
-- Gets the ids of every orphaned solver a user owns (orphaned meaning the solver is in no spaces
DROP PROCEDURE IF EXISTS GetOrphanedSolverIds;
CREATE PROCEDURE GetOrphanedSolverIds(IN _userId INT)
	BEGIN
		SELECT solvers.id FROM solvers
		LEFT JOIN solver_assoc ON solver_assoc.solver_id=solvers.id
		WHERE solvers.user_id=_userId AND solver_assoc.space_id IS NULL;
	END //

-- returns every solver that shares a space with the given user
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolversInSharedSpaces;
CREATE PROCEDURE GetSolversInSharedSpaces(IN _userId INT) 
	BEGIN
		SELECT * FROM solvers
		JOIN solver_assoc ON solver_assoc.solver_id = solvers.id
		JOIN user_assoc ON user_assoc.space_id = solver_assoc.space_id
		WHERE user_assoc.user_id=_userId
		GROUP BY(solvers.id);
	END //

-- Sets the build_status status code of the solver
-- Author: Andrew Lubinus
DROP PROCEDURE IF EXISTS SetSolverBuildStatus;
CREATE PROCEDURE SetSolverBuildStatus(IN _solverId INT, IN _build_status INT)
    BEGIN
        UPDATE solvers
        SET build_status = _build_status
        WHERE id = _solverId;
    END //

-- Updates path to solver
-- Author: Andrew Lubinus
DROP PROCEDURE IF EXISTS SetSolverPath;
CREATE PROCEDURE SetSolverPath(IN _solverId INT, IN _path TEXT)
    BEGIN
        UPDATE solvers
        SET path = _path
        WHERE id = _solverId;
    END //

-- This deletes the dummy config from a solver built on Starexec
DROP PROCEDURE IF EXISTS DeleteBuildConfig;
CREATE PROCEDURE DeleteBuildConfig(IN _solverId INT)
    BEGIN
        DELETE FROM configurations
        WHERE solver_id=_solverId AND name="starexec_build";
    END //

DELIMITER ; -- This should always be at the end of this file
