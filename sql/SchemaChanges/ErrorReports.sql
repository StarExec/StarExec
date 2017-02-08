USE starexec;

CREATE TABLE log_levels(
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(32) NOT NULL,

	PRIMARY KEY(id),
	UNIQUE KEY(name)
);

INSERT INTO log_levels (name) VALUES ('OFF'),('FATAL'),('ERROR'),('WARN'),('INFO'),('DEBUG'),('TRACE'),('ALL');

CREATE TABLE error_reports(
	id INT NOT NULL AUTO_INCREMENT,
	message TEXT NOT NULL,
	time TIMESTAMP NOT NULL DEFAULT NOW(),
	log_level_id INT,

	PRIMARY KEY(id),
	CONSTRAINT error_level FOREIGN KEY (log_level_id) REFERENCES log_levels(id) ON DELETE SET NULL
);

