-- Gets the fewest necessary Benchmarks in order to service a client's
-- request for the next page of Benchmarks in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets benchmarks across all spaces for one user. Excludes deleted benchmarks
-- Author: Wyatt Kaiser

	
-- vars 
-- userId == the ID of the user to get the benchmarks of
-- query == the query to filter by
-- recycled == true or false to get recycled benchmarks or not	
				SELECT 	benchmarks.id AS id, 
						benchmarks.name AS name, 
						user_id,
						benchmarks.description AS description,
						deleted,
						recycled,
						user_id,
						processors.name							AS benchTypeName,
						processors.description					AS benchTypeDescription
						
				FROM	benchmarks
				LEFT JOIN	processors  ON benchmarks.bench_type=processors.id
				where user_id = :userId and deleted=false AND recycled=:recycled
				
				-- Exclude benchmarks whose name doesn't contain the query string
				AND 	(benchmarks.name	LIKE	CONCAT('%', :query, '%')
				OR		processors.name	LIKE 	CONCAT('%', :query, '%'))										
			