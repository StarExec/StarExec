-- Gets the fewest necessary Users in order to service a client's
-- request for the next page of Users in their DataTable object.
-- This services the DataTable object by supporting filtering by a query,
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Author: Todd Elvers
-- vars
-- spaceId = The space to get users in
-- query = The query to filter users by

	SELECT id,
	       institution,
	       email,
	       first_name,
	       last_name
	FROM users
	INNER JOIN user_assoc ON user_assoc.user_id=users.id
	WHERE user_assoc.space_id=:spaceId AND (
		CONCAT(users.first_name, ' ', users.last_name) LIKE CONCAT('%', :query, '%')
		OR users.institution                           LIKE CONCAT('%', :query, '%')
		OR users.email                                 LIKE CONCAT('%', :query, '%')
	)
