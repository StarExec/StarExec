-- This adds the descriptions column if it dosent exist. This implements the
-- requirements for ticket #326. Note that we need to have the complicated looking 
-- if statement. If we don't SQL will complain about duplicate columns.

-- Author: aguo2

DROP PROCEDURE IF EXISTS UpdateTo7_8 //
CREATE PROCEDURE UpdateTo7_8()
BEGIN
    IF NOT EXISTS (
        SELECT *
        FROM information_schema.columns
        WHERE table_name = 'queues' AND column_name = 'description'
    ) THEN
        ALTER TABLE queues
        ADD COLUMN description VARCHAR(200);
    END IF;
END //

CALL UpdateTo7_8() //
DROP PROCEDURE IF EXISTS UpdateTo7_8 //