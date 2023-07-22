-- this adds the read only mode, which disables job submission. This implements
-- the requirements as specified in ticket 353

-- Author: aguo2

DROP PROCEDURE IF EXISTS UpdateTo8_9 //
CREATE PROCEDURE UpdateTo8_9()
BEGIN
    IF NOT EXISTS (
        SELECT *
        FROM information_schema.columns
        WHERE table_name = 'system_flags' AND column_name = 'read_only' 
    ) THEN
        ALTER TABLE system_flags
            ADD COLUMN read_only VARCHAR(200) NOT NULL DEFAULT 'false';
    END IF;
END //

CALL UpdateTo8_9() //
DROP PROCEDURE IF EXISTS UpdateTo8_9 //

