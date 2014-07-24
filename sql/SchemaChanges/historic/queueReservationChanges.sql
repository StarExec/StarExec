USE starexec;


DELETE FROM queue_request;

 ALTER TABLE queue_request DROP COLUMN reserve_date;

 CREATE TABLE queue_request_assoc (
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	request_id INT NOT NULL,
	PRIMARY KEY (reserve_date, request_id),
CONSTRAINT queue_request_assoc FOREIGN KEY (request_id) REFERENCES queue_request(id) ON DELETE CASCADE
);


ALTER TABLE queue_request DROP FOREIGN KEY queue_request_user_id;

ALTER TABLE queue_request DROP FOREIGN KEY queue_request_space_id;
	
alter table queue_request drop primary key, add primary key(id);

ALTER TABLE queue_request ADD CONSTRAINT queue_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE queue_request ADD CONSTRAINT queue_request_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE;
ALTER TABLE queue_request ADD CONSTRAINT queue_request_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE;


