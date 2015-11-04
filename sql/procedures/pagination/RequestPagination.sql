DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

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

DROP PROCEDURE IF EXISTS GetNextPageOfPendingCommunityRequestsForCommunity;
CREATE PROCEDURE GetNextPageOfPendingCommunityRequestsForCommunity(IN _startingRecord INT, IN _recordsPerPage INT, IN _communityId INT)
	BEGIN
		SELECT 	user_id, 
				community, 
				code,
				message,
				created
		FROM	community_requests
		WHERE   community = _communityId
		ORDER BY 
			created
		 ASC
	 
		-- Shrink the results to only those required for the next page
		LIMIT _startingRecord, _recordsPerPage;
	END //

DELIMITER ; -- This should always go at the end of the file
