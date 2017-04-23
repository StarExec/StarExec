-- Description: Create tables for new Analytics feature

USE starexec;

-- A list of all events
INSERT INTO analytics_events (name) VALUES
	('JOB_ATTRIBUTES'),
	('JOB_DETAILS');
