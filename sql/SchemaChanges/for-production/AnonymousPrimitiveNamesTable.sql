USE starexec;

CREATE TABLE anonymous_primitive_names (
	anonymous_name VARCHAR(36) NOT NULL,
	primitive_id INT NOT NULL,
	primitive_type ENUM('solver', 'job', 'bench', 'config') NOT NULL,
	job_id INT NOT NULL,

	PRIMARY KEY (primitive_id, primitive_type),
	CONSTRAINT anonymous_names_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
