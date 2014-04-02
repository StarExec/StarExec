USE starexec;

ALTER TABLE queue_reserved
ADD COLUMN message TEXT NOT NULL;

ALTER TABLE reservation_history
ADD COLUMN message TEXT NOT NULL;

ALTER TABLE jobs
ADD COLUMN paused_admin BOOLEAN DEFAULT false;

ALTER TABLE queues
ADD COLUMN permanent BOOLEAN DEFAULT false;

ALTER TABLE queues
ADD COLUMN global_access BOOLEAN DEFAULT false;

ALTER TABLE reservation_history
CHANGE queue_id queue_name VARCHAR(64) NOT NULL;

