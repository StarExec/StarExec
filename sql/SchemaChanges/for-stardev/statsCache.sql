use starexec;
drop table job_stats;
CREATE TABLE job_stats (
	job_id INT NOT NULL,
	job_space_id INT NOT NULL,
	config_id INT NOT NULL,
	complete INT NOT NULL,
	failed INT NOT NULL,
	error INT NOT NULL,
	wallclock DOUBLE,
	PRIMARY KEY (job_id,job_space_id),
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE CASCADE,
	FOREIGN KEY (job_space_id) REFERENCES job_spaces(id) ON DELETE CASCADE
);