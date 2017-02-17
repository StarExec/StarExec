DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Adds an error log to the database.
DROP PROCEDURE IF EXISTS AddErrorLog;
CREATE PROCEDURE AddErrorLog(IN _message TEXT, _logLevel VARCHAR(32), OUT _id INT)
  BEGIN
    SET @llid := (SELECT id FROM log_levels WHERE _logLevel = name);
    INSERT INTO error_logs (message, log_level_id) VALUES (_message, @llid);
    SELECT LAST_INSERT_ID() INTO _id;
  END //

-- Gets an error log from the database with the given id.
DROP PROCEDURE IF EXISTS GetErrorLogById;
CREATE PROCEDURE GetErrorLogById(IN _id INT)
  BEGIN
    SELECT el.id AS id, el.message AS message, el.time AS time, ll.name AS level
    FROM error_logs el JOIN log_levels ll ON el.log_level_id=ll.id
    WHERE el.id=_id;
  END //

-- Deletes an error log with the given id.
DROP PROCEDURE IF EXISTS DeleteErrorLogWithId;
CREATE PROCEDURE DeleteErrorLogWithId(IN _id INT)
  BEGIN
    DELETE FROM error_logs
    WHERE id=_id;
  END //

-- Deletes all error log from before the given time.
DROP PROCEDURE IF EXISTS DeleteErrorLogsBefore;
CREATE PROCEDURE DeleteErrorLogsBefore(IN _time TIMESTAMP)
  BEGIN
    DELETE FROM error_logs
    WHERE error_logs.time < _time;
  END //

-- Deletes all error log from before the given time.
DROP PROCEDURE IF EXISTS GetErrorLogsBefore;
CREATE PROCEDURE GetErrorLogsBefore(IN _time TIMESTAMP)
  BEGIN
    SELECT el.id AS id, el.message AS message, el.time AS time, ll.name AS level
    FROM error_logs el JOIN log_levels ll ON el.log_level_id=ll.id
    WHERE error_logs.time < _time
    ORDER BY error_logs.time DESC;
  END //

-- Gets all error logs since the given time (inclusive).
DROP PROCEDURE IF EXISTS GetErrorLogsSince;
CREATE PROCEDURE GetErrorLogsSince(IN _since TIMESTAMP)
  BEGIN
    SELECT el.id AS id, el.message AS message, el.time AS time, ll.name AS level
    FROM error_logs el JOIN log_levels ll ON el.log_level_id = ll.id
    WHERE error_logs.time >= _since
    ORDER BY error_logs.time DESC;
  END //

DROP PROCEDURE IF EXISTS GetAllErrorLogs;
CREATE PROCEDURE GetAllErrorLogs()
  BEGIN
    SELECT el.id AS id, el.message AS message, el.time AS time, ll.name AS level
    FROM error_logs el JOIN log_levels ll ON el.log_level_id = ll.id
    ORDER BY error_logs.time DESC;
  END //


DELIMITER ; -- this should always be at the end of the file
