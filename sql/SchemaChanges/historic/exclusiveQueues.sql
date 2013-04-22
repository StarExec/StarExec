USE starexec;

-- Association between exclusive queues and spaces (initially only communities)
CREATE TABLE comm_queue (
	space_id INT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE, 	
	queue_id INT NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
	PRIMARY KEY (space_id, queue_id)
);
