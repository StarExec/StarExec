USE starexec;

START TRANSACTION;


-- Step 1: Change the ID of jobpair_stage_data to a simple stage number
ALTER TABLE jobpair_stage_data MODIFY id INT NOT NULL;

ALTER TABLE jobpair_stage_data DROP PRIMARY KEY;

ALTER TABLE jobpair_stage_data CHANGE id stage_number INT;

UPDATE jobpair_stage_data SET stage_number = 1;

ALTER TABLE jobpair_stage_data ADD PRIMARY KEY (jobpair_id,stage_number);

UPDATE job_pairs SET primary_jobpair_data=1; -- all current pairs have exactly 1 stage

-- Step 2: Give a status code to the jobpair_stage_data table

ALTER TABLE jobpair_stage_data ADD COLUMN status_code TINYINT DEFAULT 0;

UPDATE jobpair_stage_data JOIN (SELECT id, status_code FROM job_pairs) AS pairs ON pairs.id=jobpair_stage_data.jobpair_id SET jobpair_stage_data.status_code = pairs.status_code;

-- Step 3: Add a reference to a jobpair_stage_data stage to job_attributes

ALTER TABLE job_attributes DROP FOREIGN KEY job_attributes_pair_id;

ALTER TABLE job_attributes ADD COLUMN jobpair_data INT NOT NULL;

UPDATE job_attributes JOIN (SELECT id, primary_jobpair_data FROM job_pairs) AS pairs ON pairs.id=job_attributes.pair_id SET jobpair_data=primary_jobpair_data;

ALTER TABLE job_attributes DROP PRIMARY KEY, ADD PRIMARY KEY (jobpair_data,attr_key);

ALTER TABLE job_attributes ADD CONSTRAINT job_attributes_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE;

-- Set 5: Add a post processor column to job_stage_params

ALTER TABLE job_stage_params ADD COLUMN post_processor INT;

ALTER TABLE job_stage_params ADD CONSTRAINT job_stage_params_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL;

ALTER TABLE job_stage_params CHANGE stage_id stage_number INT;

-- Step 5: Remove unnecessary pipline_stages columns

ALTER TABLE pipeline_stages DROP COLUMN solver_id, DROP COLUMN solver_name, DROP COLUMN config_name;

-- Set 6: Add name columns to jobpair_stage_data;

ALTER TABLE jobpair_stage_data ADD COLUMN solver_id INT,
ADD COLUMN config_id INT, 
ADD COLUMN config_name VARCHAR(255),
ADD COLUMN solver_name VARCHAR(255),
ADD COLUMN job_space_id INT;

UPDATE jobpair_stage_data JOIN job_pairs ON job_pairs.id=jobpair_stage_data.jobpair_id 
SET jobpair_stage_data.solver_id = job_pairs.solver_id, jobpair_stage_data.solver_name=job_pairs.solver_name,
jobpair_stage_data.config_name=job_pairs.config_name, jobpair_stage_data.config_id=job_pairs.config_id,
jobpair_stage_data.job_space_id=job_pairs.job_space_id;

ALTER TABLE jobpair_stage_data ADD INDEX (job_space_id, config_name),
ADD INDEX (job_space_id, solver_name),
ADD INDEX (job_space_id, config_id);

-- Step 7: Remove name columns from job_pairs

ALTER TABLE job_pairs DROP INDEX job_space_id_4,
DROP INDEX job_space_id_5, DROP INDEX job_pairs_solver_id,
DROP INDEX job_space_id_3, DROP INDEX config_id_2, DROP INDEX config_id_3;

ALTER TABLE job_pairs DROP COLUMN config_name,
DROP COLUMN solver_name, DROP COLUMN config_id, DROP COLUMN solver_id;

COMMIT;
