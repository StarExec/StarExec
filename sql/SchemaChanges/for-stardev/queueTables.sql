USE starexec;

DROP TABLE node_reserved;

CREATE TABLE node_reserved (
	node_count INT NOT NULL,
	queue_id INT NOT NULL,
	reserve_date DATE NOT NULL,
	PRIMARY KEY (queue_id,reserve_date)
);

CREATE TABLE reservation_history (
	space_id INT NOT NULL,
	queue_id INT NOT NULL,
	node_count INT NOT NULL,
	start_date DATE NOT NULL,
	end_date DATE NOT NULL,
	PRIMARY KEY (queue_Id, start_date)
);
