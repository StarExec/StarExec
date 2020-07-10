-- Modify table configurations; add a deleted column
-- Column jobs.deleted is of type INT and not BOOL, so to be consistent I will do the same here
-- This is to resolve solvers being misrepresented when displayed in job spaces in the
-- case of their configurations being deleted (causes query to return empty set)
-- Alexander Brown

DROP PROCEDURE IF EXISTS UpdateTo5_6 //
CREATE PROCEDURE UpdateTo5_6()
BEGIN
    IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=5) THEN
        UPDATE system_flags SET minor_version=6;
		ALTER TABLE configurations ADD COLUMN deleted INT DEFAULT 0;
	END IF;
END //

CALL UpdateTo5_6() //
DROP PROCEDURE IF EXISTS UpdateTo5_6 //

