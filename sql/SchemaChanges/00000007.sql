-- Modify table solvers by dropping column "config_deleted", which was added in the
-- previous SchemaChange, 00000006.sql
-- 

-- Alexander Brown

DROP PROCEDURE IF EXISTS UpdateTo6_7 //
CREATE PROCEDURE UpdateTo6_7()
BEGIN
    IF EXISTS (SELECT 1 FROM system_flags WHERE major_version=1 AND minor_version=6) THEN
        UPDATE system_flags SET minor_version=7;
        ALTER TABLE solvers DROP COLUMN config_deleted;
    END IF;
END //

CALL UpdateTo6_7() //
DROP PROCEDURE IF EXISTS UpdateTo6_7 //

