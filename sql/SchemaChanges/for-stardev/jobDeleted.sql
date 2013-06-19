USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns

ALTER TABLE jobs
ADD deleted BOOLEAN DEFAULT FALSE;

ALTER TABLE job_pairs
DROP FOREIGN KEY job_pairs_ibfk_2;

ALTER TABLE job_pairs
DROP FOREIGN KEY job_pairs_ibfk_3;