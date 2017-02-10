DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

DROP PROCEDURE IF EXISTS AddErrorLog;
CREATE PROCEDURE AddErrorLog(IN _message TEXT, _logLevel VARCHAR(32), OUT _id)
  BEGIN
    SET @llid := (SELECT id FROM log_levels WHERE _logLevel = name);
    INSERT INTO error_logs (message, log_level_id) VALUES (_message, @llid);
  END //

-- DROP PROCEDURE IF EXISTS ClearErrorLogsSince;
-- CREATE PROCEDURE ClearErrorLogsSince(IN _since DATE)
--  BEGIN
--    DELETE FROM error_logs
--    WHERE error_logs.time < _since;
--  END //



DELIMITER ; -- this should always be at the end of the file
