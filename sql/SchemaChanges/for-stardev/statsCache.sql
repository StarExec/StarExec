USE starexec;

drop table if exists job_stats;
CREATE TABLE job_stats (
	job_id INT NOT NULL,
	job_space_id INT NOT NULL,
	config_id INT NOT NULL,
	complete INT NOT NULL,
	failed INT NOT NULL,
	error INT NOT NULL,
	wallclock DOUBLE,
	KEY (job_space_id),
	KEY (job_id),
	KEY (config_id)
);