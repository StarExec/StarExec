USE starexec;

-- Stores the ids of job pairs that need to be processed and which processor should run on them
CREATE TABLE processing_job_pairs (
	pair_id INT,
	proc_id INT NOT NULL,
	old_status_code INT NOT NULL,
	PRIMARY KEY (pair_id),
	CONSTRAINT processing_job_pairs_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE,
	CONSTRAINT processing_job_pairs_proc_id FOREIGN KEY (proc_id) REFERENCES processors(id) ON DELETE CASCADE
);

UPDATE job_pairs SET status_code=7 WHERE status_code=22;