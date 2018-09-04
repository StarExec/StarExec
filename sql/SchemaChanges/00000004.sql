-- Add `freeze_primitives` system flag
-- When this is set to TRUE, uploading Benchmarks and Solvers is disabled

DROP PROCEDURE IF EXISTS UpdateTo3_4 //
CREATE PROCEDURE UpdateTo3_4()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=3) THEN
		UPDATE system_flags SET minor_version=4;

		CREATE TABLE ui_status_message (
			integrity_keeper ENUM('') NOT NULL,
			enabled BOOLEAN DEFAULT FALSE,
			message TEXT,
			url TEXT,
			PRIMARY KEY (integrity_keeper)
		);

		INSERT INTO ui_status_message (enabled) VALUES (false);
	END IF;
END //

CALL UpdateTo3_4() //
DROP PROCEDURE IF EXISTS UpdateTo3_4 //
