USE starexec;

ALTER TABLE job_pairs
ADD INDEX (config_id, job_space_id);