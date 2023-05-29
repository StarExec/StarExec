
-- Gets the fewest necessary Spaces in order to service a client's
-- request for the next page of Spaces in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
-- vars
-- userId = The ID of the user making the call. Used to filter spaces by permissions
-- spaceId = The ID of the space to get subspaces of
-- query = The query to filter spaces by
				SELECT DISTINCT	child_id AS id,
						spaces.name AS name,
						spaces.description AS description,
						set_assoc.space_id AS parent
				
				FROM	set_assoc
				-- Exclude Spaces that aren't children of the specified space
				-- and ensure only Spaces that this user is a member of are returned	
				INNER JOIN spaces on spaces.id=set_assoc.child_id
				LEFT JOIN user_assoc ON (user_assoc.space_id=set_assoc.child_id)
				WHERE	set_assoc.space_id= :spaceId AND (user_assoc.user_id= :userId OR spaces.public_access=true
				
				OR (SELECT (role='admin') 
					FROM user_roles
					JOIN users ON users.email=user_roles.email
					WHERE users.id= :userId))
																
				-- Exclude Spaces whose name and description don't contain the query string
				AND 	(spaces.name			LIKE	CONCAT('%', :query, '%')
				OR		spaces.description		LIKE 	CONCAT('%', :query, '%'))
