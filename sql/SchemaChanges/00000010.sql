-- this makes it so our job stats cache has a column stating if the resulting calulation came from 
-- including pairs with unknown stats.

DROP PROCEDURE IF EXISTS UpdateTo9_10 //
CREATE PROCEDURE UpdateTo9_10()
BEGIN
    IF NOT EXISTS (
        SELECT *
        FROM information_schema.columns
        WHERE table_name = 'job_stats' AND column_name = 'include_unknown'
    ) THEN
        ALTER TABLE job_stats
        ADD COLUMN include_unknown BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END //

CALL UpdateTo9_10() //
DROP PROCEDURE IF EXISTS UpdateTo9_10 //