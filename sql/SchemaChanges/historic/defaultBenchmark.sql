USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns
-- ALTER TABLE space_default_settings
-- ADD default_benchmark INT DEFAULT NULL;
-- ALTER TABLE space_default_settings
-- DROP PRIMARY KEY, ADD PRIMARY KEY (space_id);
ALTER TABLE space_default_settings
ADD FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE CASCADE;