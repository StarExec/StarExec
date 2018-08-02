-- Description: This file contains all miscellaneous stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

-- Adds a new historical record to the logins table which tracks all user logins
DROP PROCEDURE IF EXISTS LogDeletedPath //
CREATE PROCEDURE LogDeletedPath(IN _path TEXT)
	BEGIN
		INSERT INTO deleted_paths (deleted_path)
		VALUES (_path);
	END //

-- Adds a new historical record to the logins table which tracks all user logins
DROP PROCEDURE IF EXISTS GetDeletedPaths //
CREATE PROCEDURE GetDeletedPaths()
	BEGIN
		SELECT deleted_path FROM deleted_paths;
	END //
