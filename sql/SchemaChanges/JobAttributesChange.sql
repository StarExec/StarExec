USE prod_starexec;

-- Adds the job_id column to the schema
-- Author: Eric Burns
ALTER TABLE job_attributes
ADD job_id INT NOT NULL;
