USE starexec;

ALTER TABLE system_flags ADD COLUMN	test_queue INT DEFAULT NULL;
ALTER TABLE system_flags ADD CONSTRAINT system_flags_test_queue FOREIGN KEY (test_queue) REFERENCES queues(id) ON DELETE SET NULL;