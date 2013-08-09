USE starexec;

ALTER TABLE job_pairs
ADD INDEX (job_space_id, config_id);