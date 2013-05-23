USE starexec;

-- For updating the space settings table to include a new int
-- Author: Eric Burns
-- TODO: Figure out how to get this schema change to work
ALTER TABLE space_default_settings
DROP FOREIGN KEY space_default_settings_ibfk_2, ADD FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL;
ALTER TABLE space_default_settings
DROP FOREIGN KEY space_default_settings_ibfk_3, ADD FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE SET NULL;