USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns
ALTER TABLE space_default_settings
ADD default_benchmark INT DEFAULT -1;
ALTER TABLE space_default_settings
DROP PRIMARY KEY, ADD PRIMARY KEY (space_id, post_processor, cpu_timeout, clock_timeout, dependencies_enabled);
ALTER TABLE space_default_settings
ADD FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE CASCADE;