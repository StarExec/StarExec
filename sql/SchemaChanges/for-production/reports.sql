USE starexec;
-- table for storing statistics for the weekly report
CREATE TABLE report_data (
	id INT NOT NULL AUTO_INCREMENT,
	event_name VARCHAR(64),
	queue_id INT, -- NULL if data is not associated with a queue 
	occurrences INT NOT NULL,

	UNIQUE KEY(event_name),
	PRIMARY KEY(id)
	CONSTRAINT datas_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE NO ACTION
);

INSERT INTO report_data (event_name, queue_id, occurrences) VALUES ('logins', NULL, 0), ('jobs initiated', NULL, 0),
	('job pairs run', NULL, 0), ('solvers uploaded', NULL, 0), ('benchmarks uploaded', NULL, 0), ('benchmark archives uploaded', NULL, 0); 

ALTER TABLE users ADD COLUMN subscribed_to_reports BOOLEAN NOT NULL DEFAULT FALSE;

