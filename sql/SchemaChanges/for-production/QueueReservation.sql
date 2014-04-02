USE starexec;

ALTER TABLE queue_reserved
ADD COLUMN message TEXT NOT NULL;

ALTER TABLE reservation_history
ADD COLUMN message TEXT NOT NULL;

ALTER TABLE queues
ADD COLUMN permanent BOOLEAN DEFAULT false;

ALTER TABLE queues
ADD COLUMN global_access BOOLEAN DEFAULT false;

ALTER TABLE reservation_history
CHANGE queue_id queue_name VARCHAR(64) NOT NULL;

CREATE TABLE system_flags (
	paused BOOLEAN DEFAULT FALSE,
	PRIMARY KEY (paused)
);

