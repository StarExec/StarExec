USE prod_starexec;

-- For keeping stats for solvers
-- Author: Eric Burns
CREATE TABLE job_stats (
	id INT NOT NULL,
	job_id INT NOT NULL,
	solver_id INT NOT NULL,
	config_ID INT NOT NULL,
	jp_complete INT DEFAULT 0,
	jp_incomplete INT DEFAULT 0,
	jp_error INT DEFAULT 0,
	PRIMARY KEY (id)
);