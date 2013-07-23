USE starexec;

-- Removes the space_id column from  the job pairs table, which has been replaced by a job_space_id column
-- Author: Eric Burns

ALTER TABLE job_pairs
DROP FOREIGN KEY job_pairs_ibfk_4;

ALTER TABLE job_pairs
DROP COLUMN space_id;