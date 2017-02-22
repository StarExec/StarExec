USE starexec;
ALTER TABLE jobs ADD COLUMN total_pairs INT NOT NULL;

UPDATE jobs SET total_pairs = (SELECT count(*) from job_pairs WHERE job_id = jobs.id);