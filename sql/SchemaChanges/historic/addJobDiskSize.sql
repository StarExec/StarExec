USE starexec;
ALTER TABLE jobs ADD COLUMN disk_size BIGINT NOT NULL;
ALTER TABLE jobpair_stage_data ADD COLUMN disk_size BIGINT NOT NULL;