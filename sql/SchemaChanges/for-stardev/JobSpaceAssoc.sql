USE starexec;

ALTER TABLE job_space_assoc
DROP FOREIGN KEY job_space_assoc_ibfk_1;

ALTER TABLE job_space_assoc
DROP parent;

ALTER TABLE job_space_assoc
ADD space_id INT NOT NULL;

ALTER TABLE job_space_assoc
ADD FOREIGN KEY(space_id) REFERENCES job_spaces(id) ON DELETE CASCADE;