USE starexec;

ALTER TABLE jobs ADD COLUMN (
	kill_delay INT,
	soft_time_limit INT
);
