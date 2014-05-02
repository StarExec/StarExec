USE starexec;

ALTER TABLE space_default_settings
ADD COLUMN maximum_memory BIGINT DEFAULT 1073741824;

ALTER TABLE job_pairs
ADD COLUMN maximum_memory BIGINT DEFAULT 1073741824;