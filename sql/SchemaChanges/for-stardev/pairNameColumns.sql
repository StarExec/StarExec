USE starexec;

ALTER TABLE job_pairs
ADD COLUMN solver_name VARCHAR(256);

ALTER TABLE job_pairs
ADD COLUMN config_name VARCHAR(256);

ALTER TABLE job_pairs
ADD COLUMN bench_name VARCHAR(256);

ALTER TABLE job_pairs
ADD INDEX (job_space_id, solver_name);

ALTER TABLE job_pairs 
ADD INDEX (job_space_id, config_name);

ALTER TABLE job_pairs
ADD INEX (job_space_id, bench_name);