USE starexec;

-- introduces a path through the space tree for job pairs
ALTER TABLE job_pairs
path VARCHAR(2048);