USE starexec;

-- For updating the space settings table to include a new boolean
-- Author: Eric Burns
ALTER TABLE space_default_settings
ADD dependencies_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE space_default_settings
DROP PRIMARY KEY, ADD PRIMARY KEY (space_id, post_processor, cpu_timeout, clock_timeout, dependencies_enabled);