-- Description: This file contains all miscellaneous stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Adds a new column to the specified table only if it doesn't already exist.
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS AddColumnUnlessExists;
CREATE PROCEDURE AddColumnUnlessExists(IN dbName tinytext, IN tableName tinytext, IN fieldName tinytext, IN fieldDef text)
	BEGIN
		IF NOT EXISTS 
			(SELECT * FROM information_schema.COLUMNS
			WHERE column_name=fieldName
			AND table_name=tableName
			AND table_schema=dbName)
		THEN
			SET @addColumn = CONCAT('ALTER TABLE ', dbName, '.', tableName,
			' ADD COLUMN ', fieldName, ' ', fieldDef);
			PREPARE stmt FROM @addColumn;
			EXECUTE stmt;
		END IF;
	END //

-- Adds a new historical record to the logins table which tracks all user logins
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS LoginRecord;
CREATE PROCEDURE LoginRecord(IN _userId INT, IN _ipAddress VARCHAR(15), IN _agent TEXT)
	BEGIN		
		INSERT INTO logins (user_id, login_date, ip_address, browser_agent)
		VALUES (_userId, SYSDATE(), _ipAddress, _agent);
	END //

	
	
DELIMITER ; -- This should always be at the end of this file