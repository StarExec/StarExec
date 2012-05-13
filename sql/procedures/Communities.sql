-- Description: This file contains all community stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Checks to see if the space with the given space ID is a community.
-- Author: Skylar Stark
DROP PROCEDURE IF EXISTS IsCommunity;
CREATE PROCEDURE IsCommunity(IN _spaceId INT)
	BEGIN
		SELECT *
		FROM set_assoc
		WHERE space_id = 1 AND child_id = _spaceId;
	END //

-- Returns basic space information for the community with the given id
-- This ensures security by preventing malicious users from getting details about ANY space
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS GetCommunityById;
CREATE PROCEDURE GetCommunityById(IN _id INT)
	BEGIN
		SELECT *
		FROM spaces
		WHERE id IN
			(SELECT child_id
			 FROM set_assoc
			 WHERE space_id=1
			 AND child_id = _id);
	END //

-- Removes the association a user has with a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS LeaveCommunity;
CREATE PROCEDURE LeaveCommunity(IN _userId INT, IN _commId INT)
	BEGIN
		-- Remove the permission associated with this user/community
		DELETE FROM permissions
			WHERE id=(SELECT permission FROM user_assoc WHERE user_id = _userId	AND space_id = _commId);
		
		-- Delete the association	
		DELETE FROM user_assoc
		WHERE user_id = _userId
		AND space_id = _commId;
	END //
	



DELIMITER ; -- This should always be at the end of this file