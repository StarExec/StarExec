-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  

SELECT 	id,
	institution,
	email,
	first_name,
	last_name,
	CONCAT(first_name, ' ', last_name) AS full_name,
	role,
	subscribed_to_reports
	FROM	users NATURAL JOIN user_roles
    WHERE
                -- Exclude Users whose name and description don't contain the query string
                (CONCAT(first_name, ' ', last_name) LIKE    CONCAT('%', :query, '%')
                OR      institution                         LIKE    CONCAT('%', :query, '%')
                OR      email                               LIKE    CONCAT('%', :query, '%'))
