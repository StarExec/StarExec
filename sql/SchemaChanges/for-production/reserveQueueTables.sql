USE starexec;

DROP TABLE if exists node_reserved;
DROP TABLE if exists queue_request;
DROP TABLE if exists queue_reserved;
DROP TABLE if exists reservation_history;

-- Pending requests to reserve a queue
-- Author: Wyatt Kaiser
CREATE TABLE queue_request (
	user_id INT NOT NULL,
	space_id INT NOT NULL,
	queue_name VARCHAR(64) NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	message TEXT NOT NULL,
	code VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, space_id, queue_name, reserve_date)
);


-- The history of queue_reservations (i.e. reservations that happened in the past)
-- Author: Wyatt Kaiser
CREATE TABLE reservation_history (
	space_id INT NOT NULL,
	queue_id INT NOT NULL,
	node_count INT NOT NULL,
	start_date DATE NOT NULL,
	end_date DATE NOT NULL,
	PRIMARY KEY (queue_Id, start_date)
);

-- Reserved queues. includes future reservations and current reservations
-- Author: Wyatt Kaiser
CREATE TABLE queue_reserved (
	space_id INT NOT NULL,
	queue_id INT NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	PRIMARY KEY (space_id, queue_id, reserve_date)
);

-- Includes temporary data when editing node_count information
-- Author: Wyatt Kaiser
CREATE TABLE temp_node_changes (
	space_id INT NOT NULL,
	queue_name VARCHAR(64) NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	PRIMARY KEY (space_id, queue_name, reserve_date)
);