USE starexec;

ALTER TABLE job_stage_params ADD COLUMN stdout_save_option INT NOT NULL DEFAULT 2;
ALTER TABLE job_stage_params ADD COLUMN	extra_output_save_option INT NOT NULL DEFAULT 2;