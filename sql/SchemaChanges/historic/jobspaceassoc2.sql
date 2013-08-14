USE starexec;

ALTER TABLE job_space_assoc
DROP FOREIGN KEY job_space_assoc_ibfk_2;

ALTER TABLE job_space_assoc
DROP child;

ALTER TABLE job_space_assoc
ADD child_id INT NOT NULL;

ALTER TABLE job_space_assoc
ADD FOREIGN KEY(child_id) REFERENCES job_spaces(id) ON DELETE CASCADE;