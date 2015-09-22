USE starexec;

CREATE TABLE jobpair_time_delta (
	user_id INT NOT NULL,
	queue_id INT NOT NULL,
	time_delta INT DEFAULT 0,
	PRIMARY KEY (user_id, queue_id),
	CONSTRAINT jobpair_time_delta_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCASE,
	CONSTRAINT jobpair_time_delta_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);