USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

	
	
-- Gets the fewest necessary Spaces in order to service a client's
-- request for the next page of Spaces in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfSpaces;
CREATE PROCEDURE GetNextPageOfSpaces(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _userId INT, IN _query TEXT, IN _publicUserId INT)
	BEGIN
		IF (_colSortedOn=0) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description
				
				FROM	spaces
				-- Exclude Spaces that aren't children of the specified space
				-- and ensure only Spaces that this user is a member of are returned	
				INNER JOIN set_assoc ON set_assoc.space_id=spaces.id
				JOIN user_assoc ON (user_assoc.space_id=set_assoc.child_id)
				WHERE	user_assoc.user_id=_userId OR user_assoc.user_id=_publicUserId
																
				-- Exclude Spaces whose name and description don't contain the query string
				AND 	(name			LIKE	CONCAT('%', _query, '%')
				OR		description		LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY name ASC
				
				-- Shrink the results to only those required for the next page of Spaces
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						description
				FROM	spaces
				-- Exclude Spaces that aren't children of the specified space
				-- and ensure only Spaces that this user is a member of are returned	
				INNER JOIN set_assoc ON set_assoc.space_id=spaces.id
				JOIN user_assoc ON (user_assoc.space_id=set_assoc.child_id)
				WHERE	user_assoc.user_id=_userId OR user_assoc.user_id=_publicUserId
				
				AND 	(name			LIKE	CONCAT('%', _query, '%')
				OR		description		LIKE 	CONCAT('%', _query, '%'))
				ORDER BY name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						name,
						description
				
				FROM	spaces
				
				-- Exclude Spaces that aren't children of the specified space
				-- and ensure only Spaces that this user is a member of are returned	
				INNER JOIN set_assoc ON set_assoc.space_id=spaces.id
				JOIN user_assoc ON (user_assoc.space_id=set_assoc.child_id)
				WHERE	user_assoc.user_id=_userId OR user_assoc.user_id=_publicUserId
				
								
				-- Exclude Spaces whose name and description don't contain the query string
				AND 	(name			LIKE	CONCAT('%', _query, '%')
				OR		description		LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY description ASC
				
				-- Shrink the results to only those required for the next page of Spaces
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						name,
						description
				FROM	spaces
				-- Exclude Spaces that aren't children of the specified space
				-- and ensure only Spaces that this user is a member of are returned	
				INNER JOIN set_assoc ON set_assoc.space_id=spaces.id
				JOIN user_assoc ON (user_assoc.space_id=set_assoc.child_id)
				WHERE	user_assoc.user_id=_userId OR user_assoc.user_id=_publicUserId
				
				AND 	(name			LIKE	CONCAT('%', _query, '%')
				OR		description		LIKE 	CONCAT('%', _query, '%'))
				ORDER BY description DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //

DELIMITER ; -- This should always go at the end of the file