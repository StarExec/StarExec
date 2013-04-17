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
		SELECT DISTINCT * from solvers where id in 
		(SELECT DISTINCT solver_id from solver_assoc where space_id in 
		(SELECT id from spaces where public_access=1));
	END //
	
-- Gets all solvers that reside in public spaces of a specific community
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetPublicSolversByCommunity;
CREATE PROCEDURE GetPublicSolversByCommunity(IN _commId INT, IN _pubUserId INT)
	BEGIN
		SELECT DISTINCT * from solvers where id in 
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
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS DeleteSolverById;
CREATE PROCEDURE DeleteSolverById(IN _solverId INT, OUT _path TEXT)
	BEGIN
		SELECT path INTO _path FROM solvers WHERE id = _solverId;
		DELETE FROM solvers
		WHERE id = _solverId;
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


-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of Solvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfSolvers;
CREATE PROCEDURE GetNextPageOfSolvers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of solvers without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	*
				
				FROM	solvers
				
				-- Exclude solvers that aren't in the specified space
				WHERE 	id 	IN (SELECT	solver_id
								FROM	solver_assoc
								WHERE	space_id = _spaceId)
								
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						WHEN 1 THEN description
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
				FROM	solvers
				WHERE 	id 	IN (SELECT	solver_id
								FROM	solver_assoc
								WHERE	space_id = _spaceId)
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						WHEN 1 THEN description
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target solvers contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	*
				FROM 	solvers
				
				-- Exclude solvers whose name and description don't contain the query string
				WHERE 	(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%'))
										
				-- Exclude solvers that aren't in the specified space
				AND 	id 	IN (SELECT	solver_id
								FROM	solver_assoc
								WHERE	space_id = _spaceId)
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name 
						WHEN 1 THEN description
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
				FROM 	solvers
				WHERE 	(name 				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%'))
				AND 	id 	IN (SELECT	solver_id
								FROM	solver_assoc
								WHERE	space_id = _spaceId)
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name 
						WHEN 1 THEN description
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //

	
-- Retrieves all solvers belonging to a space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceSolversById;
CREATE PROCEDURE GetSpaceSolversById(IN _id INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE id IN
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
		SELECT id
		FROM solvers
		WHERE id IN (SELECT solver_id
					FROM configurations
					WHERE id = _id);
	END //
	
-- Retrieves the solver with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSolverById;
CREATE PROCEDURE GetSolverById(IN _id INT)
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
		FROM solvers
		WHERE id IN (SELECT solver_id
					FROM solver_assoc
					WHERE space_id = _spaceId);
	END //
	
	
-- Retrieves the solvers owned by a given user id
-- Todd Elvers
DROP PROCEDURE IF EXISTS GetSolversByOwner;
CREATE PROCEDURE GetSolversByOwner(IN _userId INT)
	BEGIN
		SELECT *
		FROM solvers
		WHERE user_id = _userId;
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
	
-- Removes the association between a solver and a given space;
-- places the path of the solver in _path if it has no other
-- associations in solver_assoc and isn't being used for any jobs,
-- otherwise places NULL in _path
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveSolverFromSpace;
CREATE PROCEDURE RemoveSolverFromSpace(IN _solverId INT, IN _spaceId INT, OUT _path TEXT)
	BEGIN
		IF _spaceId >= 0 THEN
			DELETE FROM solver_assoc
			WHERE solver_id = _solverId
			AND space_id = _spaceId;
		END IF;
		
		-- Ensure the solver isn't being used in any other space
		IF NOT EXISTS(SELECT * FROM solver_assoc WHERE solver_id =_solverId) THEN
			-- Ensure the solver isn't being used for any other jobs
			IF NOT EXISTS(SELECT * FROM job_pairs JOIN configurations ON job_pairs.config_id=configurations.id WHERE solver_id=_solverId) THEN
				SELECT path INTO _path FROM solvers WHERE id = _solverId;
				DELETE FROM solvers
				WHERE id = _solverId;
			ELSE
				SELECT NULL INTO _path;
			END IF;
		ELSE
			SELECT NULL INTO _path;
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
		WHERE user_id = _userId;
	END //
	
	-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of SOlvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets solvers across all spaces for one user.  
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfUserSolvers;
CREATE PROCEDURE GetNextPageOfUserSolvers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Solvers without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id,  
						description
				
				FROM	solvers where user_id = _userId
				
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						ELSE description
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of Solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						description
						
				FROM	solvers where user_id = _userId

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						ELSE description
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Solvers contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						description
				
				FROM	solvers where user_id = _userId
				
				-- Exclude Solvers whose name doesn't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						ELSE description
					 END) ASC	 
				-- Shrink the results to only those required for the next page of Solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						description
						
				FROM	solvers where user_id = _userId
				
				AND 	(name				LIKE	CONCAT('%', _query, '%'))
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
						ELSE description
					 END) DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //


DELIMITER ; -- This should always be at the end of this file