USE starexec;

ALTER TABLE job_pairs
ADD COLUMN solver_name VARCHAR(256);

ALTER TABLE job_pairs
ADD COLUMN config_name VARCHAR(256);

ALTER TABLE job_pairs
ADD COLUMN bench_name VARCHAR(256);