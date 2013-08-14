-- Description: This file contains all solver-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a solver and returns the solver ID
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS AddSolver;
CREATE PROCEDURE AddSolver(IN _userId INT, IN _name VARCHAR(128), IN _downloadable BOOLEAN, IN _path TEXT, IN _description TEXT, OUT _id INT, IN _diskSize BIGINT)
	BEGIN
		INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size)
		VALUES (_userId, _name, SYSDATE(), _path, _description, _downloadable, _diskSize);
		
		SELECT LAST_INSERT_ID() INTO _id;
	END //

-- Gets all solvers that reside in public spaces
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetPublicSolvers;
CREATE PROCEDURE GetPublicSolvers()
	BEGIN
		SELECT DISTINCT * from solvers where deleted=false AND id in 
		(SELECT DISTINCT solver_id from solver_assoc where space_id in 
		(SELECT id from spaces where public_access=1));
	END //
	
-- Gets all solvers that reside in public spaces of a specific community
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetPublicSolversByCommunity;
CREATE PROCEDURE GetPublicSolversByCommunity(IN _commId INT, IN _pubUserId INT)
	BEGIN
		SELECT DISTINCT * from solvers where deleted=false and id in 
		(SELECT DISTINCT solver_id from solver_assoc where space_id in 
		(SELECT id from spaces where (IsPublic(id,_pubUserId) = 1) AND id in (select descendant from closure where ancestor = _commId)));
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
CREATE PROCEDURE AddConfiguration(IN _solverId INT, IN _name VARCHAR(128), IN _description TEXT, OUT configId INT)
	BEGIN
		INSERT INTO configurations (solver_id, name, description)
		VALUES (_solverId, _name, _description);
		
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
DROP PROCEDURE IF EXISTS DeleteSolverById;
CREATE PROCEDURE DeleteSolverById(IN _solverId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM solvers WHERE id = _solverId;
		UPDATE solvers
		SET deleted=true
		WHERE id = _solverId;
		UPDATE solvers
		SET path=""
		WHERE id = _solverId;
		UPDATE solvers
		SET disk_size=0
		WHERE id = _solverId;
		-- if the solver is associated with no spaces, we can delete it from the database
		IF ((SELECT COUNT(*) FROM solver_assoc WHERE solver_id=_solverId)=0) THEN
			IF ((SELECT COUNT(*) FROM job_pairs WHERE solver_id=_solverId)=0) THEN
				DELETE FROM solvers
				WHERE id=_solverId;
			END IF;
		END IF;
		
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
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceSolversById;
CREATE PROCEDURE GetSpaceSolversById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE deleted=false AND id IN
				(SELECT solver_id
				FROM solver_assoc
				WHERE space_id = _id)
		ORDER BY name;
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
		WHERE id = _id and deleted=false;
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
	
	
-- Returns the number of solvers in a given space
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetSolverCountInSpace;
CREATE PROCEDURE GetSolverCountInSpace(IN _spaceId INT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solver_assoc
		WHERE _spaceId=space_id;
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
		WHERE user_id = _userId and deleted=false;
	END //

-- Returns the number of public spaces a solver is in
-- Benton McCune
DROP PROCEDURE IF EXISTS IsSolverPublic;
CREATE PROCEDURE IsSolverPublic(IN _solverId INT, IN _publicUserId INT)
	BEGIN
		SELECT count(*) as solverPublic
		FROM solver_assoc
		WHERE solver_id = _solverId
		AND (IsPublic(space_id,_publicUserId) = 1);
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
		
		-- Ensure the solver isn't being used in any other space
		IF NOT EXISTS(SELECT * FROM solver_assoc WHERE solver_id =_solverId) THEN
			-- if the solver has been deleted already, remove it from the database
			
			IF NOT EXISTS(SELECT * FROM solvers WHERE _solverId=id AND deleted=false) THEN
				IF ((SELECT COUNT(*) FROM job_pairs JOIN configurations AS config WHERE config.id=config_id AND config.solver_id=_solverId)=0) THEN
					DELETE FROM solvers
					WHERE id=_solverId;
				END IF;
			END IF;
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

-- Finds the spaces associated with a given solver
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolverAssoc;
CREATE PROCEDURE GetSolverAssoc(IN _solverId INT)
	BEGIN
		SELECT space_id
		FROM solver_assoc
		WHERE _solverId = solver_id;
	END //
	
-- Updates the details associated with a given configuration
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS UpdateConfigurationDetails;
CREATE PROCEDURE UpdateConfigurationDetails(IN _configId INT, IN _name VARCHAR(128), IN _description TEXT)
	BEGIN
		UPDATE configurations
		SET name = _name,
		description = _description
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
		WHERE user_id = _userId AND deleted=false;
	END //
	
-- Returns the number of solvers in a given space that match a given query
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetSolverCountByUserWithQuery;
CREATE PROCEDURE GetSolverCountByUserWithQuery(IN _userId INT, IN _query TEXT)
	BEGIN
		SELECT COUNT(*) AS solverCount
		FROM solvers
		WHERE user_id=_userId AND deleted=false AND
				(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%'));
	END //


DELIMITER ; -- This should always be at the end of this file