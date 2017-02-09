DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS AddErrorLog;
CREATE PROCEDURE AddErrorLog(IN _message TEXT, _logLevel VARCHAR(32))
  BEGIN
    SET @llid := (SELECT id FROM log_levels WHERE _logLevel = name);
    INSERT INTO error_reports (message, log_level_id) VALUES (_message, @llid);
  END //


DELIMITER ; -- this should always be at the end of the file
