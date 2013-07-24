USE starexec;

ALTER TABLE job_pairs
ADD INDEX (config_id);

ALTER TABLE job_pairs
ADD INDEX (job_space_id);