USE starexec;

ALTER TABLE queues ADD column cpuTimeout INT DEFAULT 259200;

ALTER TABLE queues ADD COLUMN clockTimeout INT DEFAULT 259200;