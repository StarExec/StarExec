-- This file changes the job_stats table to a new format. It drops all the old  cached items,
-- which can simply be recalculated
USE starexec;

DELETE FROM job_stats;

ALTER TABLE job_stats
DROP COLUMN error;

ALTER TABLE job_stats
ADD COLUMN correct INT NOT NULL;

ALTER TABLE job_stats
ADD COLUMN incorrect INT NOT NULL;