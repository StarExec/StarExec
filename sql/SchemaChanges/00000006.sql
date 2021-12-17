-- Modify table configurations by adding a "deleted" column
-- Modify table solvers by adding a "config_deleted" column
-- Column jobs.deleted is of type INT and not BOOL, so to be consistent I will do the same here
-- Adding the column to configurations resolves solvers being misrepresented when displayed in job spaces
-- in the case of their configurations being deleted (currently causes query to return empty set)
-- Adding the column to solvers allows for link modification in the solvers summary table in the 
-- job space; this is necessary to improve user experience and not have dead links to a 404 page in the
-- case of deleted configurations
-- Alexander Brown

DROP PROCEDURE IF EXISTS UpdateTo5_6 //
CREATE PROCEDURE UpdateTo5_6()
BEGIN
    IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=5) THEN
        UPDATE system_flags SET minor_version=6;
		ALTER TABLE configurations ADD COLUMN deleted INT DEFAULT 0;
		ALTER TABLE solvers ADD COLUMN config_deleted INT DEFAULT 0;
	END IF;
END //

CALL UpdateTo5_6() //
DROP PROCEDURE IF EXISTS UpdateTo5_6 //

