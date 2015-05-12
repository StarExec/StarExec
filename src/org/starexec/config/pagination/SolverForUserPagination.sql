	-- Gets the fewest necessary Solvers in order to service a client's
-- request for the next page of SOlvers in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets solvers across all spaces for one user.  
-- Author: Wyatt Kaiser
-- vars
-- userId The ID of the user to get solvers for
-- query The query to filter solvers by
-- recycled true to include recycled solvers and false otherwise
				SELECT 	*
				FROM	solvers 
				INNER JOIN executable_types AS types ON types.type_id=solvers.executable_type		
				
				
				where user_id = :userId and deleted=false AND recycled= :recycled

				-- Exclude Solvers whose name doesn't contain the query string
				AND 	(name				LIKE	CONCAT('%', :query, '%')
				OR		description			LIKE 	CONCAT('%', :query, '%')
				OR 		type_name			LIKE	CONCAT('%', :query, '%'))										
						
			