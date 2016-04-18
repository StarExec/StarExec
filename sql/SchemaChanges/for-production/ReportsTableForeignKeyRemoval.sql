USE starexec;

ALTER TABLE report_data ADD COLUMN queue_name VARCHAR(64);

UPDATE report_data JOIN queues ON report_data.queue_id = queues.id SET report_data.queue_name = queues.name;

ALTER TABLE report_data DROP FOREIGN KEY report_data_queue_id;

ALTER TABLE report_data ADD CONSTRAINT UNIQUE `event_name_queue_name` (event_name, queue_name);

ALTER TABLE report_data DROP INDEX event_name;

ALTER TABLE report_data DROP COLUMN queue_id; 


