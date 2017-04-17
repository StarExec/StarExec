-- Description: Create tables for new Analytics feature

USE starexec;

-- Contains all events that can be logged as analytics
CREATE TABLE analytics_events (
	event_id INT NOT NULL AUTO_INCREMENT,
	name CHAR(32) NOT NULL,
	PRIMARY KEY (event_id),
	UNIQUE KEY (name)
);

-- A list of all events
INSERT INTO analytics_events (name) VALUES
	('JOB_CREATE_QUICKJOB'),
	('JOB_PAUSE'),
	('JOB_RESUME'),
	('PAGEVIEW_HELP');

-- Contains historical analytics data:
--  * number of times an event was recorded on a particular date
CREATE TABLE analytics_historical (
	event_id INT NOT NULL,
	date_recorded DATE NOT NULL,
	count INT NOT NULL DEFAULT 0,
	PRIMARY KEY (event_id, date_recorded),
	CONSTRAINT id_assoc FOREIGN KEY (event_id) REFERENCES analytics_events(event_id)
);
