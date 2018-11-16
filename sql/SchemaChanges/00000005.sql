-- Modify timestamp column so they are not updated automatically

DROP PROCEDURE IF EXISTS UpdateTo4_5 //
CREATE PROCEDURE UpdateTo4_5()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=4) THEN
		UPDATE system_flags SET minor_version=5;
		ALTER TABLE users CHANGE COLUMN created created timestamp DEFAULT NOW();
	END IF;
END //

CALL UpdateTo4_5() //
DROP PROCEDURE IF EXISTS UpdateTo4_5 //
