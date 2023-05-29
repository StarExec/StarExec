-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of Solvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
-- vars
-- spaceId The space to get solvers in
-- query The query to filter solvers by
				SELECT 	*
				FROM 	solvers
				INNER JOIN solver_assoc AS assoc ON assoc.solver_id=id
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type
				-- Exclude solvers whose name and description don't contain the query string
				WHERE 	(name 		LIKE	CONCAT('%', :query, '%')
				OR		description	LIKE 	CONCAT('%', :query, '%')
				OR 		type_name			LIKE	CONCAT('%', :query, '%'))
										
				-- Exclude solvers that aren't in the specified space
				AND assoc.space_id= :spaceId