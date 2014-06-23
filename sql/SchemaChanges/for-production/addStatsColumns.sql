USE starexec;

-- Adds newly necessary columns to the job_stats table
-- Author: Eric Burns

ALTER TABLE job_stats
ADD cpu DOUBLE;

ALTER TABLE job_stats
ADD resource_out INT NOT NULL;