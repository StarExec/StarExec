-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
-- vars
-- spaceId = The space to get users in
-- query = The query to filter users by
				SELECT 	id,
						institution,
						email,
						first_name,
						last_name,
						CONCAT(first_name, ' ', last_name) AS full_name
				
				FROM	users 
				INNER JOIN user_assoc ON user_assoc.user_id=users.id
				WHERE 
				
				
				-- Exclude Users that aren't in the specified space
				user_assoc.space_id= :spaceId
							
				-- Exclude Users whose name and description don't contain the query string
				AND 	(CONCAT(first_name, ' ', last_name)	LIKE	CONCAT('%', :query, '%')
				OR		institution							LIKE 	CONCAT('%', :query, '%')
				OR		email								LIKE 	CONCAT('%', :query, '%'))
