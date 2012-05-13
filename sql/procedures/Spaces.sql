-- Description: This file contains all space-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Adds a new space with the given information
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddSpace;
CREATE PROCEDURE AddSpace(IN _name VARCHAR(32), IN _desc TEXT, IN _locked TINYINT(1), IN _permission INT, IN _parent INT, OUT id INT)
	BEGIN		
		INSERT INTO spaces (name, created, description, locked, default_permission)
		VALUES (_name, SYSDATE(), _desc, _locked, _permission);
		SELECT LAST_INSERT_ID() INTO id;
		INSERT INTO closure (ancestor, descendant)	-- Update closure table
			SELECT ancestor, id FROM closure
			WHERE descendant = _parent
			UNION ALL SELECT _parent, id UNION SELECT id, id; 
	END //

-- Adds an association between two spaces
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AssociateSpaces;
CREATE PROCEDURE AssociateSpaces(IN _parentId INT, IN _childId INT)
	BEGIN		
		INSERT IGNORE INTO set_assoc
		VALUES (_parentId, _childId);
	END //
	
-- Gets all the descendants of a space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetDescendantsOfSpace;
CREATE PROCEDURE GetDescendantsOfSpace(IN _spaceId INT)
	BEGIN
		SELECT descendant
		FROM closure
		WHERE ancestor = _spaceId AND NOT descendant=_spaceId;
	END //
	
-- Gets all the leaders of a space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetLeadersBySpaceId;
CREATE PROCEDURE GetLeadersBySpaceId(IN _id INT)
	BEGIN
		SELECT *
		FROM users
		WHERE email IN
			(SELECT DISTINCT users.email
			FROM spaces
			JOIN user_assoc ON spaces.id=user_assoc.space_id
			JOIN users ON user_assoc.user_id=users.id
			JOIN permissions ON user_assoc.permission=permissions.id
			WHERE spaces.id=_id AND permissions.is_leader=1);
	END //
	
	
-- Gets the fewest necessary Spaces in order to service a client's
-- request for the next page of Spaces in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfSpaces;
CREATE PROCEDURE GetNextPageOfSpaces(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Spaces without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description
				
				FROM	spaces
				
				-- Exclude Spaces that aren't children of the specified space
				WHERE 	id 	IN (SELECT 	child_id
								FROM	set_assoc
								WHERE 	space_id = _spaceId)
										
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					(CASE _colSortedOn
						WHEN 0 THEN name
						WHEN 1 THEN description
					END) ASC
				
				-- Shrink the results to only those required for the next page of Spaces
				-- LIMIT _startingRecord, _recordsPerPage;
				LIMIT 0, 10;
			ELSE
				SELECT 	id,
						name,
						description
				FROM	spaces
				WHERE 	id 	IN (SELECT 	child_id
								FROM	set_assoc
								WHERE 	space_id = _spaceId)
				ORDER BY 
					(CASE _colSortedOn
						WHEN 0 THEN name
						WHEN 1 THEN description
					END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Spaces contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description
				
				FROM	spaces
				
				-- Exclude Spaces that aren't children of the specified space
				WHERE 	id 	IN (SELECT 	child_id
								FROM	set_assoc
								WHERE 	space_id = _spaceId)
								
				-- Exclude Spaces whose name and description don't contain the query string
				AND 	id	IN (SELECT	id
								FROM 	spaces
								WHERE 	name			LIKE	CONCAT('%', _query, '%')
								OR		description		LIKE 	CONCAT('%', _query, '%'))
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					(CASE _colSortedOn
						WHEN 0 THEN name
						WHEN 1 THEN description
					END) ASC
				
				-- Shrink the results to only those required for the next page of Spaces
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						description
				FROM	spaces
				WHERE 	id 	IN (SELECT 	child_id
								FROM	set_assoc
								WHERE 	space_id = _spaceId)
				AND 	id	IN (SELECT	id
								FROM 	spaces
								WHERE 	name			LIKE	CONCAT('%', _query, '%')
								OR		description		LIKE 	CONCAT('%', _query, '%'))
				ORDER BY 
					(CASE _colSortedOn
						WHEN 0 THEN name
						WHEN 1 THEN description
					END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
	
-- Returns basic space information for the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSpaceById;
CREATE PROCEDURE GetSpaceById(IN _id INT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id = _id;
	END //

	
-- Gets all the spaces that a user has access to
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetSpacesByUser;
CREATE PROCEDURE GetSpacesByUser(IN _userId INT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN 
			(SELECT space_id FROM user_assoc WHERE user_id = _userId);
	END //

	
-- Returns all spaces belonging to the space with the given id.
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetSubSpacesById;
CREATE PROCEDURE GetSubSpacesById(IN _spaceId INT, IN _userId INT)
	BEGIN
		IF _spaceId <= 0 THEN	-- If we get an invalid ID, return the root space (the space with the mininum ID)
			SELECT *
			FROM spaces
			WHERE id = 
				(SELECT MIN(id)
				FROM spaces);
		ELSE					-- Else find all children spaces that are an ancestor of a space the user is apart of
			SELECT *
			FROM spaces
			WHERE id IN
				(SELECT child_id 
				 FROM set_assoc 
					JOIN closure ON set_assoc.child_id=closure.ancestor 
					JOIN user_assoc ON (user_assoc.user_id=_userId AND user_assoc.space_id=closure.descendant) 
					WHERE set_assoc.space_id=_spaceId)
			ORDER BY name;
		END IF;
	END //
	
-- Returns all subsspaces of a given name belonging to the space with the given id.
-- Author: Benton McCune
DROP PROCEDURE IF EXISTS GetSubSpaceByName;
CREATE PROCEDURE GetSubSpaceByName(IN _spaceId INT, IN _userId INT, IN _name VARCHAR(64))
	BEGIN
		IF _spaceId <= 0 THEN	-- If we get an invalid ID, return the root space (the space with the mininum ID)
			SELECT *
			FROM spaces
			WHERE id = 
				(SELECT MIN(id)
				FROM spaces);
		ELSE					-- Else find all children spaces that are an ancestor of a space the user is apart of
			SELECT *
			FROM spaces
			WHERE id IN
				(SELECT child_id 
				 FROM set_assoc 
					JOIN closure ON set_assoc.child_id=closure.ancestor 
					JOIN user_assoc ON (user_assoc.user_id=_userId AND user_assoc.space_id=closure.descendant) 
					WHERE set_assoc.space_id=_spaceId)
			AND name = _name
			ORDER BY name;
		END IF;
	END //
	
	
-- Returns all spaces that are a subspace of the root
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetSubSpacesOfRoot;
CREATE PROCEDURE GetSubSpacesOfRoot()
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
				(SELECT child_id
				 FROM set_assoc
				 WHERE space_id=1)
		ORDER BY name;
	END //
	
	
-- Returns the number of subspaces in a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetSubspaceCountBySpaceId;
CREATE PROCEDURE GetSubspaceCountBySpaceId(IN _spaceId INT)
	BEGIN
		SELECT 	COUNT(*) AS spaceCount
		FROM 	spaces
		WHERE 	id	IN (SELECT	child_id
						FROM 	set_assoc
						WHERE 	space_id = _spaceId);
	END //
	
	
-- Removes the association between a space and a subspace and deletes the subspace
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS RemoveSubspace;
CREATE PROCEDURE RemoveSubspace(IN _subspaceId INT)
	BEGIN
		-- Remove that space's default permission
		DELETE FROM permissions 
			WHERE id=(SELECT default_permission FROM spaces WHERE id=_subspaceId);
			
		-- Remove the space
		DELETE FROM spaces
		WHERE id = _subspaceId;
		
	END //
	
-- Updates the name of the space with the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateSpaceName;
CREATE PROCEDURE UpdateSpaceName(IN _id INT, IN _name VARCHAR(32))
	BEGIN
		UPDATE spaces
		SET name = _name
		WHERE id = _id;
	END //

-- Updates the name of the spacewith the given id
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS UpdateSpaceDescription;
CREATE PROCEDURE UpdateSpaceDescription(IN _id INT, IN _desc TEXT)
	BEGIN
		UPDATE spaces
		SET description = _desc
		WHERE id = _id;
	END //
	
-- Updates all details of the space with the given id, and returns the permission id to
-- help update default permissions.
-- Author: Skylar Stark	
DROP PROCEDURE IF EXISTS UpdateSpaceDetails;
CREATE PROCEDURE UpdateSpaceDetails(IN _spaceId INT, IN _name VARCHAR(32), IN _desc TEXT, IN _locked BOOLEAN, OUT _perm INT)
	BEGIN
		UPDATE spaces
		SET name = _name,
		description = _desc,
		locked = _locked
		WHERE id = _spaceId;
		
		SELECT default_permission INTO _perm
		FROM spaces
		WHERE id = _spaceId;
	END //
	


DELIMITER ; -- This should always be at the end of this file