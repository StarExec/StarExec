USE starexec;

-- For keeping stats for solvers
-- Author: Eric Burns
CREATE TABLE job_stats (
	id INT NOT NULL,
	job_id INT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
	solver_id INT NOT NULL,
	config_id INT NOT NULL,
	jp_complete INT DEFAULT 0,
	jp_incomplete INT DEFAULT 0,
	jp_error INT DEFAULT 0,
	PRIMARY KEY (id)
);