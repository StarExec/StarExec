USE starexec;

ALTER TABLE space_default_settings ADD COLUMN pre_processor INT;
ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL;
