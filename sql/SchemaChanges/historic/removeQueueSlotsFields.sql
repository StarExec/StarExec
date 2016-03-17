USE starexec;

ALTER TABLE queues DROP COLUMN slots_used;
ALTER TABLE queues DROP COLUMN slots_reserved;
ALTER TABLE queues DROP COLUMN slots_free;
ALTER TABLE queues DROP COLUMN slots_total;