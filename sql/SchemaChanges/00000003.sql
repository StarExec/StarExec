-- Add `freeze_primitives` system flag
-- When this is set to TRUE, uploading Benchmarks and Solvers is disabled

DROP PROCEDURE IF EXISTS UpdateTo2_3 //
CREATE PROCEDURE UpdateTo2_3()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=2) THEN
		UPDATE system_flags SET minor_version=3;

		ALTER TABLE system_flags ADD freeze_primitives BOOLEAN DEFAULT FALSE;
	END IF;
END //

CALL UpdateTo2_3() //
DROP PROCEDURE IF EXISTS UpdateTo2_3 //
