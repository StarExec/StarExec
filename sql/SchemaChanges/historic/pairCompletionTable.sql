USE starexec;


CREATE TABLE job_pair_completion (
	pair_id INT NOT NULL,
	completion_id INT NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (completion_id),
	FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);