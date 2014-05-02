DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the next page of data table for queue reservation requests
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfPendingQueueRequests;
CREATE PROCEDURE GetNextPageOfPendingQueueRequests(IN _startingRecord INT, IN _recordsPerPage INT)
	BEGIN
		SELECT 	user_id, 
				space_id, 
				queue_name,
				MAX(node_count),
				MIN(reserve_date),
				MAX(reserve_date),
				message,
				code,
				created
		FROM	queue_request
		GROUP BY user_id, space_id, queue_name
		ORDER BY 
			created
		 ASC
	 
		-- Shrink the results to only those required for the next page
		LIMIT _startingRecord, _recordsPerPage;
	END //
	
-- Gets the next page of data table for queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfQueueReservations;
CREATE PROCEDURE GetNextPageOfQueueReservations(IN _startingRecord INT, IN _recordsPerPage INT)
	BEGIN
		SELECT 	space_id, queue_id, node_count, MIN(reserve_date), MAX(reserve_date)
		FROM	queue_reserved
		GROUP BY space_id, queue_id
	 
		-- Shrink the results to only those required for the next page
		LIMIT _startingRecord, _recordsPerPage;
	END //
	
-- Gets the next page of data table for historic queue reservations
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfHistoricQueueReservations;
CREATE PROCEDURE GetNextPageOfHistoricQueueReservations(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _query TEXT)
	BEGIN
		IF (_colSortedOn = 0) THEN
			IF _sortASC = TRUE THEN
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY queue_name ASC
				
				LIMIT _startingRecord, _recordsPerPage;
			ELSE 
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY queue_name DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 1) THEN
			IF _sortASC = TRUE THEN
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY node_count ASC
				
				LIMIT _startingRecord, _recordsPerPage;
			ELSE 
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY node_count DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 2) THEN
			IF _sortASC = TRUE THEN
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY start_date ASC
				
				LIMIT _startingRecord, _recordsPerPage;
			ELSE 
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY start_date DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 3) THEN
			IF _sortASC = TRUE THEN
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY end_date ASC
				
				LIMIT _startingRecord, _recordsPerPage;
			ELSE 
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY end_date DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_colSortedOn = 4) THEN
			IF _sortASC = TRUE THEN
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY message ASC
				
				LIMIT _startingRecord, _recordsPerPage;
			ELSE 
				SELECT 	queue_name,
						node_count,
						start_date,
						end_date,
						message
				
				FROM	reservation_history
				
				-- Exclude reservations whose name and message don't contain the query string
				WHERE 	(		
								queue_name 	LIKE 	CONCAT('%', _query, '%')
						OR 		message		LIKE	CONCAT('%', _query, '%')
						)
				
				ORDER BY message DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
-- Gets the next page of data table for community requests
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfPendingCommunityRequests;
CREATE PROCEDURE GetNextPageOfPendingCommunityRequests(IN _startingRecord INT, IN _recordsPerPage INT)
	BEGIN
		SELECT 	user_id, 
				community, 
				code,
				message,
				created
		FROM	community_requests
		ORDER BY 
			created
		 ASC
	 
		-- Shrink the results to only those required for the next page
		LIMIT _startingRecord, _recordsPerPage;
	END //


DELIMITER ; -- This should always go at the end of the file