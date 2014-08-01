-- Description: This file contains all community stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

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
		FROM set_assoc
			JOIN spaces AS space ON space.id=set_assoc.child_id
		WHERE _id=child_id AND space_id=1;
	END //

-- Removes the association a user has with a given space
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS LeaveSpace;
CREATE PROCEDURE LeaveSpace(IN _userId INT, IN _spaceId INT)
	BEGIN
		-- Remove the permission associated with this user/space
		DELETE FROM permissions
			WHERE id=(SELECT permission FROM user_assoc WHERE user_id = _userId	AND space_id = _spaceId);
		
		-- Delete the association	
		DELETE FROM user_assoc
		WHERE user_id = _userId
		AND space_id = _spaceId;
	END //
	
-- Get the default settings of the community given by id.
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS GetCommunityDefaultSettingsById;
CREATE PROCEDURE GetCommunityDefaultSettingsById(IN _id INT)
	BEGIN
		SELECT space_id, name, cpu_timeout, clock_timeout, post_processor
		FROM space_default_settings AS settings
		LEFT OUTER JOIN processors AS pros
		ON settings.post_processor = pros.id
		WHERE space_id = _id;
	END //

-- Set a default setting of a community given by id.
-- Author: Ruoyu Zhang
DROP PROCEDURE IF EXISTS SetCommunityDefaultSettingsById;
CREATE PROCEDURE SetCommunityDefaultSettingsById(IN _id INT, IN _num INT, IN _setting INT)
	BEGIN
      CASE _num
		WHEN 1 THEN
		UPDATE space_default_settings
		SET post_processor = _setting
		WHERE space_id = _id;
		
		WHEN 2 THEN
		UPDATE space_default_settings
		SET cpu_timeout = _setting
		WHERE space_id = _id;
		
		WHEN 3 THEN
		UPDATE space_default_settings
		SET clock_timeout = _setting
		WHERE space_id = _id;
    END CASE;
	END //
	
-- Removes every association a user has with every space in the hierarchy rooted at the given spacew
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS LeaveHierarchy;
CREATE PROCEDURE LeaveCommunity(IN _userId INT, IN _commId INT)
	BEGIN
		DELETE user_assoc FROM user_assoc
		JOIN closure ON closure.descendant=user_assoc.space_id
		WHERE closure.ancestor=_commId AND user_id=_userId;
	END //

DELIMITER ; -- This should always be at the end of this file