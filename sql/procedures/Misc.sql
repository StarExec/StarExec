-- Description: This file contains all miscellaneous stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

-- Adds a new historical record to the logins table which tracks all user logins
-- Author: Tyler Jensen
DROP PROCEDURE IF EXISTS LoginRecord //
CREATE PROCEDURE LoginRecord(IN _userId INT, IN _ipAddress VARCHAR(15), IN _agent TEXT)
	BEGIN
		INSERT INTO logins (user_id, login_date, ip_address, browser_agent)
		VALUES (_userId, SYSDATE(), _ipAddress, _agent);
	END //

DROP PROCEDURE IF EXISTS SetReadOnly //
CREATE PROCEDURE SetReadOnly(IN readOnly BOOLEAN)
	BEGIN
		UPDATE system_flags SET read_only=readOnly;
	END //

DROP PROCEDURE IF EXISTS GetReadOnly //
CREATE PROCEDURE GetReadOnly()
	BEGIN
		SELECT read_only FROM system_flags;
	END //


DROP PROCEDURE IF EXISTS SetFreezePrimitives //
CREATE PROCEDURE SetFreezePrimitives(IN frozen BOOLEAN)
	BEGIN
		UPDATE system_flags SET freeze_primitives=frozen;
	END //

DROP PROCEDURE IF EXISTS GetFreezePrimitives //
CREATE PROCEDURE GetFreezePrimitives()
	BEGIN
		SELECT freeze_primitives FROM system_flags;
	END //

DROP PROCEDURE IF EXISTS SetStatusMessage //
CREATE PROCEDURE SetStatusMessage(IN _enabled BOOLEAN, IN _message TEXT, IN _url TEXT)
	BEGIN
		UPDATE ui_status_message SET enabled=_enabled, message=_message, url=_url;
	END //

DROP PROCEDURE IF EXISTS GetStatusMessage //
CREATE PROCEDURE GetStatusMessage()
	BEGIN
		SELECT enabled, message, url FROM ui_status_message;
	END //

DROP PROCEDURE IF EXISTS GetPairTimes //
CREATE PROCEDURE GetPairTimes(IN _jobID INT)
	BEGIN
		SELECT start_time, end_time FROM job_pairs WHERE job_id = _jobID;
	END //