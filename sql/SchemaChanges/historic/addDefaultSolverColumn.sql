USE starexec;

ALTER TABLE space_default_settings ADD COLUMN default_solver INT DEFAULT NULL;

ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_default_solver FOREIGN KEY (default_solver) REFERENCES solvers(id) ON DELETE SET NULL;

ALTER TABLE space_default_settings ADD COLUMN bench_processor INT DEFAULT NULL;

ALTER TABLE space_default_settings ADD CONSTRAINT space_default_settings_bench_processor FOREIGN KEY (bench_processor) REFERENCES processors(id) ON DELETE SET NULL;