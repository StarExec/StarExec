DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS AddErrorLog;
CREATE PROCEDURE AddErrorLog(IN _message TEXT, _logLevel VARCHAR(32), OUT _id INT)
  BEGIN
    SET @llid := (SELECT id FROM log_levels WHERE _logLevel = name);
    INSERT INTO error_logs (message, log_level_id) VALUES (_message, @llid);
    SELECT LAST_INSERT_ID() INTO _id;
  END //

DROP PROCEDURE IF EXISTS GetErrorLogById;
CREATE PROCEDURE GetErrorLogById(IN _id INT)
  BEGIN
    SELECT el.id AS id, el.message AS message, el.time AS time, ll.name AS level
    FROM error_logs el JOIN log_levels ll ON el.log_level_id=ll.id
    WHERE el.id=_id;
  END //

DROP PROCEDURE IF EXISTS DeleteWithId;
CREATE PROCEDURE DeleteWithId(IN _id INT)
  BEGIN
  END //

-- DROP PROCEDURE IF EXISTS ClearErrorLogsSince;
-- CREATE PROCEDURE ClearErrorLogsSince(IN _since DATE)
--  BEGIN
--    DELETE FROM error_logs
--    WHERE error_logs.time < _since;
--  END //



DELIMITER ; -- this should always be at the end of the file
