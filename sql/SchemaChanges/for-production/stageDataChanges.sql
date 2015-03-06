USE starexec;

ALTER TABLE jobpair_stage_data ADD COLUMN status_code TINYINT DEFAULT 0;

UPDATE jobpair_stage_data JOIN (SELECT id, status_code FROM job_pairs) AS pairs ON pairs.id=jobpair_stage_data.jobpair_id SET jobpair_stage_data.status_code = pairs.status_code;

ALTER TABLE job_attributes DROP FOREIGN KEY job_attributes_pair_id;

ALTER TABLE job_attributes ADD COLUMN jobpair_data INT NOT NULL;

UPDATE job_attributes JOIN (SELECT id, primary_jobpair_data FROM job_pairs) AS pairs ON pairs.id=job_attributes.pair_id SET jobpair_data=primary_jobpair_data;

ALTER TABLE job_attributes DROP PRIMARY KEY, ADD PRIMARY KEY (jobpair_data,attr_key);

ALTER TABLE job_stage_params ADD COLUMN post_processor INT;

ALTER TABLE job_stage_params ADD CONSTRAINT job_stage_params_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL;

ALTER TABLE job_attributes    ADD CONSTRAINT job_attributes_pair_id          FOREIGN KEY (pair_id)        REFERENCES job_pairs(id) ON DELETE CASCADE;