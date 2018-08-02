-- Add `deleted_paths` table
-- This table keeps track of files that were deleted during Migration Mode, so
-- that we can ensure these files have actually been deleted once Migration Mode
-- has ended

DROP PROCEDURE IF EXISTS UpdateTo3_4 //
CREATE PROCEDURE UpdateTo3_4()
BEGIN
	IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=3) THEN
		UPDATE system_flags SET minor_version=4;

		CREATE TABLE deleted_paths (
			id INT NOT NULL AUTO_INCREMENT,
			deleted_path TEXT NOT NULL,
			PRIMARY KEY (id)
		);
	END IF;
END //

CALL UpdateTo3_4() //
DROP PROCEDURE IF EXISTS UpdateTo3_4 //
