USE starexec;

START TRANSACTION;



-- Step 1: Change the ID of jobpair_stage_data to a simple stage number
ALTER TABLE jobpair_stage_data MODIFY id INT NOT NULL;

ALTER TABLE jobpair_stage_data DROP PRIMARY KEY;

ALTER TABLE jobpair_stage_data CHANGE id stage_number INT NOT NULL;

UPDATE jobpair_stage_data SET stage_number = 1; -- all current pairs have exactly one stage

ALTER TABLE jobpair_stage_data ADD PRIMARY KEY (jobpair_id,stage_number);

UPDATE job_pairs SET primary_jobpair_data=1; -- all current pairs have exactly 1 stage

-- Step 2: Give a status code to the jobpair_stage_data table (the status code is set in step 6 for efficiency)

ALTER TABLE jobpair_stage_data ADD COLUMN status_code TINYINT DEFAULT 0;

-- Step 3: Add a reference to a jobpair_stage_data stage to job_attributes

ALTER TABLE job_attributes DROP FOREIGN KEY job_attributes_pair_id;

ALTER TABLE job_attributes ADD COLUMN stage_number INT NOT NULL DEFAULT 1;

ALTER TABLE job_attributes DROP PRIMARY KEY, ADD PRIMARY KEY (pair_id,stage_number,attr_key);

ALTER TABLE job_attributes ADD CONSTRAINT job_attributes_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE;

-- Step 4: Add post/ pre processor columns to job_stage_params

ALTER TABLE job_stage_params ADD COLUMN post_processor INT;

ALTER TABLE job_stage_params ADD CONSTRAINT job_stage_params_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL;

ALTER TABLE job_stage_params CHANGE stage_id stage_number INT;

ALTER TABLE job_stage_params ADD COLUMN pre_processor INT;

ALTER TABLE job_stage_params ADD CONSTRAINT job_stage_params_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL;

-- move processors over to job_stage_params
REPLACE INTO job_stage_params (job_id,stage_number,cpuTimeout,clockTimeout,maximum_memory,space_id,post_processor,pre_processor) SELECT id,1,cpuTimeout,clockTimeout,maximum_memory,null,post_processor,pre_processor FROM jobs;

ALTER TABLE jobs DROP FOREIGN KEY jobs_post_processor;
ALTER TABLE jobs DROP FOREIGN KEY jobs_pre_processor;

ALTER TABLE jobs DROP COLUMN post_processor;
ALTER TABLE jobs DROP COLUMN pre_processor;


-- Step 5: Remove unnecessary pipline_stages columns

ALTER TABLE pipeline_stages DROP COLUMN solver_id, DROP COLUMN solver_name, DROP COLUMN config_name, DROP COLUMN keep_output;

-- Set 6: Add name columns to jobpair_stage_data;

ALTER TABLE jobpair_stage_data ADD COLUMN solver_id INT,
ADD COLUMN config_id INT, 
ADD COLUMN config_name VARCHAR(255),
ADD COLUMN solver_name VARCHAR(255),
ADD COLUMN job_space_id INT;

UPDATE jobpair_stage_data JOIN job_pairs ON job_pairs.id=jobpair_stage_data.jobpair_id 
SET jobpair_stage_data.solver_id = job_pairs.solver_id, jobpair_stage_data.solver_name=job_pairs.solver_name,
jobpair_stage_data.config_name=job_pairs.config_name, jobpair_stage_data.config_id=job_pairs.config_id,
jobpair_stage_data.job_space_id=job_pairs.job_space_id,
jobpair_stage_data.status_code=job_pairs.status_code;

ALTER TABLE jobpair_stage_data ADD INDEX (job_space_id, config_name),
ADD INDEX (job_space_id, solver_name),
ADD INDEX (job_space_id, config_id);

-- Step 7: Remove name columns from job_pairs
ALTER TABLE job_pairs DROP INDEX job_space_id_4,
DROP INDEX job_space_id_5, DROP INDEX job_pairs_solver_id,
DROP INDEX job_space_id_3, DROP INDEX config_id_2, DROP INDEX config_id_3;

ALTER TABLE job_pairs DROP COLUMN config_name,
DROP COLUMN solver_name, DROP COLUMN config_id, DROP COLUMN solver_id;

-- Step 8: Add primary stage column to solver_pipelines

ALTER TABLE solver_pipelines ADD COLUMN primary_stage_id INT;

UPDATE solver_pipelines JOIN pipeline_stages ON solver_pipelines.id = pipeline_stages.pipeline_id SET solver_pipelines.primary_stage_id = pipeline_stages.stage_id;

ALTER TABLE solver_pipelines ADD CONSTRAINT primary_stage_id FOREIGN KEY (primary_stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE SET NULL;


-- Step 9: Drop not null constraint on pipelines to support noops

ALTER TABLE pipeline_stages MODIFY config_id INT;

UPDATE pipeline_stages LEFT JOIN configurations ON configurations.id=pipeline_stages.config_id SET pipeline_stages.config_id=null where configurations.name is null;

ALTER TABLE pipeline_stages ADD CONSTRAINT pipeline_stages_config_id FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE SET NULL;

ALTER TABLE pipeline_stages ADD COLUMN is_noop BOOLEAN NOT NULL DEFAULT FALSE;

COMMIT;
