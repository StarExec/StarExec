USE starexec;

-- For updating the jobs table to include a new int
-- Author: Wyatt Kaiser

ALTER TABLE jobs
ADD killed BOOLEAN DEFAULT FALSE;