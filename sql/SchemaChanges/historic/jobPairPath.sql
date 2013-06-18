USE starexec;

-- introduces a path through the space tree for job pairs
ALTER TABLE job_pairs
ADD path VARCHAR(2048);