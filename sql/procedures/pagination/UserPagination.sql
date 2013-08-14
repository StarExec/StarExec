USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfUsers;
CREATE PROCEDURE GetNextPageOfUsers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT, IN _publicUserId INT)
	BEGIN
		IF (_colSortedOn = 0 ) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				
				
				-- Exclude Users that aren't in the specified space
				AND 	assoc.space_id=_spaceId
							
				-- Exclude Users whose name and description don't contain the query string
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY full_name ASC
					 
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				AND	assoc.space_id=_spaceId
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
				ORDER BY full_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 1) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				
				
				-- Exclude Users that aren't in the specified space
				AND 	assoc.space_id=_spaceId
							
				-- Exclude Users whose name and description don't contain the query string
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY institution ASC
					 
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				AND	assoc.space_id=_spaceId
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
				ORDER BY institution DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				
				
				-- Exclude Users that aren't in the specified space
				AND 	assoc.space_id=_spaceId
							
				-- Exclude Users whose name and description don't contain the query string
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
								
				-- Order results depending on what column is being sorted on
				ORDER BY email ASC
					 
				-- Shrink the results to only those required for the next page of Users
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				FROM	users 
				INNER JOIN user_assoc AS assoc ON assoc.user_id=users.id
				WHERE (id != _publicUserId)
				AND	assoc.space_id=_spaceId
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', _query, '%')
				OR		institution							LIKE 	CONCAT('%', _query, '%')
				OR		email								LIKE 	CONCAT('%', _query, '%'))
				ORDER BY email DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;	
	END //
	
DELIMITER ; -- This should always go at the end of the file