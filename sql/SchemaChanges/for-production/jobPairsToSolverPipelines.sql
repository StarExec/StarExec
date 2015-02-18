USE starexec;

START TRANSACTION;
-- step 1: port timeouts over to the jobs table, removing them from job_pairs

ALTER TABLE jobs ADD COLUMN cpuTimeout INT;
ALTER TABLE jobs ADD COLUMN clockTimeout INT;
ALTER TABLE jobs ADD COLUMN maximum_memory BIGINT;

UPDATE jobs JOIN job_pairs ON job_pairs.job_id=jobs.id SET jobs.cpuTimeout=job_pairs.cpuTimeout, jobs.clockTimeout=job_pairs.clockTimeout, jobs.maximum_memory=job_pairs.maximum_memory;

ALTER TABLE job_pairs DROP COLUMN cpuTimeout;
ALTER TABLE job_pairs DROP COLUMN clockTimeout;
ALTER TABLE job_pairs DROP COLUMN maximum_memory;

-- Step 2: Create a new table that will contain any stage-specific timeouts for jobs

-- This table stores timeouts for individual pipeline stages for this job. 
-- These are essentially overrides for the columns in the jobs table
CREATE TABLE job_stage_params (
	job_id INT,
	stage_id INT,
	cpuTimeout INT, 
	clockTimeout INT,
	maximum_memory BIGINT DEFAULT 1073741824,
	space_id INT, -- if we're keeping benchmarks from this stage, where should we be putting them?
	PRIMARY KEY (job_id,stage_id),
	CONSTRAINT job_stage_params_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	CONSTRAINT job_stage_params_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE SET NULL
);

-- Step 3: Add several new columns to the pipeline_stages table that basically serve as a cache

ALTER TABLE pipeline_stages ADD COLUMN solver_name VARCHAR(128); -- These columns are redundant, but they allow us to keep stages even with deleted configs
ALTER TABLE pipeline_stages ADD COLUMN config_name VARCHAR(128);
ALTER TABLE pipeline_stages ADD COLUMN solver_id INT;
ALTER TABLE pipeline_stages DROP FOREIGN KEY pipeline_stages_config_id;

-- Step 4: Remove a set of unwanted job pair columns and constraints

ALTER TABLE job_pairs DROP FOREIGN KEY job_pairs_solver_id;
ALTER TABLE job_pairs DROP COLUMN page_reclaims;
ALTER TABLE job_pairs DROP COLUMN page_faults;
ALTER TABLE job_pairs DROP COLUMN block_input;
ALTER TABLE job_pairs DROP COLUMN block_output;
ALTER TABLE job_pairs DROP COLUMN vol_contex_swtch;
ALTER TABLE job_pairs DROP COLUMN invol_contex_swtch;
ALTER TABLE job_pairs DROP COLUMN io_data;
ALTER TABLE job_pairs DROP COLUMN io_wait;

-- Step 5: Create table for storing jobline data at the stage level

CREATE TABLE jobline_stage_data (
	id INT AUTO_INCREMENT, -- this id orders the stages
	jobline_id INT NOT NULL,
	stage_id INT, -- stages are ordered by this ID as well
	cpu DOUBLE,
	wallclock DOUBLE,
	mem_usage DOUBLE,
	max_vmem DOUBLE,
	max_res_set DOUBLE,
	user_time DOUBLE,
	system_time DOUBLE,
	PRIMARY KEY (id),
	KEY(jobline_id),
	CONSTRAINT jobline_stage_data_jobline_id FOREIGN KEY (jobline_id) REFERENCES job_pairs(id) ON DELETE CASCADE,
	CONSTRAINT jobline_stage_data_stage_id FOREIGN KEY (stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE SET NULL
);

-- Step 6: Populate the jobline_stage_data table with data from the job_pairs table and remove it from job pairs
INSERT INTO jobline_stage_data (jobline_id,stage_id,cpu,wallclock,mem_usage,max_vmem,max_res_set,user_time,system_time)
SELECT id,null,cpu,wallclock,mem_usage,max_vmem,max_res_set,user_time,system_time FROM job_pairs;


ALTER TABLE job_pairs DROP COLUMN cpu;
ALTER TABLE job_pairs DROP COLUMN max_vmem;
ALTER TABLE job_pairs DROP COLUMN mem_usage;
ALTER TABLE job_pairs DROP COLUMN user_time;
ALTER TABLE job_pairs DROP COLUMN system_time;
ALTER TABLE job_pairs DROP COLUMN max_res_set;


-- Step 7: Add primary stage column to the job_pairs table
ALTER TABLE job_pairs ADD COLUMN primary_stage INT; -- which of this pairs stages is the primary one? references jobline_stage_data.id

UPDATE job_pairs JOIN jobline_stage_data ON jobline_stage_data.jobline_id=job_pairs.id SET primary_stage=jobline_stage_data.id;

COMMIT;