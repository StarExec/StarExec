USE starexec;

START TRANSACTION;


-- Step 1: Change the ID of jobpair_stage_data to a simple stage number
ALTER TABLE jobpair_stage_data MODIFY id INT NOT NULL;

ALTER TABLE jobpair_stage_data DROP PRIMARY KEY;

ALTER TABLE jobpair_stage_data ADD COLUMN stage_number INT NOT NULL DEFAULT 1;

ALTER TABLE jobpair_stage_data DROP COLUMN id;

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

ALTER TABLE job_attributes    ADD CONSTRAINT job_attributes_pair_id          FOREIGN KEY (pair_id)        REFERENCES job_pairs(id) ON DELETE CASCADE;

-- Set 4: Add a post processor column to job_stage_params

ALTER TABLE job_stage_params ADD COLUMN post_processor INT;

ALTER TABLE job_stage_params ADD CONSTRAINT job_stage_params_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL;

COMMIT;
