USE starexec;

ALTER TABLE job_stats ADD COLUMN stage_number INT NOT NULL DEFAULT 0;

ALTER TABLE job_stats ADD PRIMARY KEY (job_space_id,config_id,stage_number);

ALTER TABLE job_spaces ADD COLUMN max_stages INT DEFAULT 1;

