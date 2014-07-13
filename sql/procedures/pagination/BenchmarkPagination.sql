DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary Benchmarks in order to service a client's
-- request for the next page of Benchmarks in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfBenchmarks;
CREATE PROCEDURE GetNextPageOfBenchmarks(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		IF (_colSortedOn = 0) THEN
			IF _sortASC = TRUE THEN
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId	
					LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id


				
				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
								
										
				-- Exclude benchmarks that aren't in the specified space

										
				-- Order results depending on what column is being sorted on
				ORDER BY benchmarks.name ASC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					-- Exclude benchmarks that aren't in the specified space
						JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId		
						LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id

				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
						
				-- Order results depending on what column is being sorted on
				ORDER BY benchmarks.name DESC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		ELSEIF (_colSortedOn = 2) THEN
			IF _sortASC = TRUE THEN
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId	
					LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id


				
				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
								
										
				-- Exclude benchmarks that aren't in the specified space

										
				-- Order results depending on what column is being sorted on
				ORDER BY bench_assoc.order_id ASC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					-- Exclude benchmarks that aren't in the specified space
						JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId		
						LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
						
				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
						
				-- Order results depending on what column is being sorted on
				ORDER BY bench_assoc.order_id DESC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId	
					LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id


				
				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
								
										
				-- Exclude benchmarks that aren't in the specified space

										
				-- Order results depending on what column is being sorted on
				ORDER BY benchType.name ASC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	benchmarks.id AS id,
						benchmarks.name AS name,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
				
				FROM 	benchmarks
					-- Exclude benchmarks that aren't in the specified space
						JOIN	bench_assoc AS assoc ON benchmarks.id = assoc.bench_id	AND assoc.space_id=_spaceId		
						LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id

				-- Query Filtering
				WHERE 	(benchmarks.name 									LIKE	CONCAT('%', _query, '%')
				OR		benchType.name	LIKE 	CONCAT('%', _query, '%'))
						
				-- Order results depending on what column is being sorted on
				ORDER BY benchType.name DESC
					 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;			
	END //
	
	
	-- Gets the fewest necessary Benchmarks in order to service a client's
-- request for the next page of Benchmarks in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets benchmarks across all spaces for one user. Excludes deleted benchmarks
-- TODO: Make this look like the space version, assuming that one works correctly
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfUserBenchmarks;
CREATE PROCEDURE GetNextPageOfUserBenchmarks(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT, IN _recycled BOOLEAN)
	BEGIN
		IF (_colSortedOn = 0 ) THEN
			IF _sortASC = TRUE THEN
				SELECT 	benchmarks.id AS id, 
						benchmarks.name AS name, 
						user_id,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
						
				FROM	benchmarks
				LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				-- Exclude benchmarks whose name doesn't contain the query string
				AND 	(benchmarks.name				LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY benchmarks.name ASC	 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	benchmarks.id AS id, 
						benchmarks.name AS name, 
						user_id,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
						
				FROM	benchmarks 
				LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
				
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				AND 	(benchmarks.name				LIKE	CONCAT('%', _query, '%'))
				ORDER BY benchmarks.name DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	benchmarks.id AS id, 
						benchmarks.name AS name, 
						user_id,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
						
				FROM	benchmarks
				LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				-- Exclude benchmarks whose name doesn't contain the query string
				AND 	(benchmarks.name				LIKE	CONCAT('%', _query, '%'))										
										
				-- Order results depending on what column is being sorted on
				ORDER BY benchTypeName ASC	 
				-- Shrink the results to only those required for the next page of benchmarks
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	benchmarks.id AS id, 
						benchmarks.name AS name, 
						user_id,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						benchType.name							AS benchTypeName,
						benchType.description					AS benchTypeDescription
						
				FROM	benchmarks
				LEFT JOIN	processors  AS benchType ON benchmarks.bench_type=benchType.id
				where user_id = _userId and deleted=false AND recycled=_recycled
				
				AND 	(benchmarks.name				LIKE	CONCAT('%', _query, '%'))
				ORDER BY benchTypeName DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
			
	END //
	


DELIMITER ; -- This should always go at the end of the file