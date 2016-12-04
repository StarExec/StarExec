USE starexec;

CREATE TABLE pairs_rerun (
	pair_id INT NOT NULL,
	PRIMARY KEY (pair_id),
	CONSTRAINT id_of_rerun_pair FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);
