-- This file contains procedures for DefaultSettings functionality

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS getProfileByIdAndName;
CREATE PROCEDURE getProfileByIdAndName(IN _id INT, IN _name VARCHAR(32), IN _type VARCHAR(8))
	BEGIN
		SELECT * FROM default_settings WHERE id=_id AND name=_name AND setting_type=_type;
	END //

DROP PROCEDURE IF EXISTS getProfilesByUser;
CREATE PROCEDURE getProfilesByUser(IN _id INT)
	BEGIN
		SELECT * FROM default_settings WHERE id=_id AND setting_type="user";
	END //


DELIMITER ; -- This should always be at the end of this file