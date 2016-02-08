USE starexec;

ALTER TABLE job_spaces ADD COLUMN job_id INT;

ALTER TABLE job_spaces ADD CONSTRAINT job_spaces_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE;