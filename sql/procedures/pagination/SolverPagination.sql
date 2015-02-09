DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of Solvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfSolvers;
CREATE PROCEDURE GetNextPageOfSolvers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		IF (_colSortedOn = 0) THEN
			IF _sortASC = TRUE THEN
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type
				-- Exclude solvers whose name and description don't contain the query string
				WHERE 	(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
										
				-- Exclude solvers that aren't in the specified space
				AND assoc.space_id=_spaceId
										
				-- Order results depending on what column is being sorted on
				ORDER BY name ASC
					 
				-- Shrink the results to only those required for the next page of solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				WHERE 	(name 				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId
				ORDER BY name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 1) THEN
			IF _sortASC = TRUE THEN
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type
				-- Exclude solvers whose name and description don't contain the query string
				WHERE 	(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
										
				-- Exclude solvers that aren't in the specified space
				AND assoc.space_id=_spaceId
										
				-- Order results depending on what column is being sorted on
				ORDER BY description ASC
					 
				-- Shrink the results to only those required for the next page of solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type
				WHERE 	(name 				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId
				ORDER BY description DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				-- Exclude solvers whose name and description don't contain the query string
				WHERE 	(name 		LIKE	CONCAT('%', _query, '%')
				OR		description	LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
										
				-- Exclude solvers that aren't in the specified space
				AND assoc.space_id=_spaceId
										
				-- Order results depending on what column is being sorted on
				ORDER BY type_name ASC
					 
				-- Shrink the results to only those required for the next page of solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type
				WHERE 	(name 				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId
				ORDER BY type_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;	
	END //
	
	
	-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of SOlvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets solvers across all spaces for one user.  
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfUserSolvers;
CREATE PROCEDURE GetNextPageOfUserSolvers(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT, IN _recycled BOOLEAN)
	BEGIN
		IF (_colSortedOn = 0) THEN
			IF _sortASC = TRUE THEN
				SELECT 	*
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type		
				
				
				where user_id = _userId and deleted=false AND recycled=_recycled

				-- Exclude Solvers whose name doesn't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY name ASC	 
				-- Shrink the results to only those required for the next page of Solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
						
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				where user_id = _userId and deleted=false AND recycled=_recycled
				
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				ORDER BY name DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;

		ELSEIF (_colSortedOn = 1) THEN
			IF _sortASC = TRUE THEN
				SELECT 	*
				
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				-- Exclude Solvers whose name doesn't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY description ASC	 
				-- Shrink the results to only those required for the next page of Solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
						
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				ORDER BY description DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	*
				
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				-- Exclude Solvers whose name doesn't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY type_name ASC	 
				-- Shrink the results to only those required for the next page of Solvers
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	*
						
				FROM	solvers
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type

				where user_id = _userId and deleted=false AND recycled=_recycled
				
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		description			LIKE 	CONCAT('%', _query, '%')
				OR 		type_name			LIKE	CONCAT('%', _query, '%'))
				ORDER BY type_name DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //


DELIMITER ; -- this should always be at the end of the file