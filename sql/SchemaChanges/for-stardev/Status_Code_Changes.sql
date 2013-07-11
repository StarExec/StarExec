USE starexec;

-- For updating the jobs table to include a new int
-- Author: Wyatt Kaiser

INSERT INTO status_codes (20, 'paused', 'the job is paused so all job_pairs that were not complete were sent to this status');
INSERT INTO status_codes (21, 'killed', 'the job was killed, so all job_pairs that were not complete were sent to this status');