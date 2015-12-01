
-- Author: Tyler Jensen
-- Description: This file is the schema used to create the starexec database in MySQL

-- The table of all users in the system
CREATE TABLE users (
	id INT NOT NULL AUTO_INCREMENT,	
	email VARCHAR(64) NOT NULL,
	first_name VARCHAR(32) NOT NULL,
	last_name VARCHAR(32) NOT NULL,	
	institution VARCHAR(64) NOT NULL,
	created TIMESTAMP NOT NULL,
	password VARCHAR(128) NOT NULL,
	disk_quota BIGINT NOT NULL,
	subscribed_to_reports BOOLEAN NOT NULL DEFAULT FALSE,
	default_page_size INT NOT NULL DEFAULT 10,
	default_settings_profile INT DEFAULT NULL,
	PRIMARY KEY (id),
	-- the following foreign key is used, but it is added at the end because you can't declare a foreign key before declaring the table
	-- CONSTRAINT users_default_settings_profile FOREIGN KEY (default_settings_profile) REFERENCES default_settings(id) ON DELETE SET NULL,
	UNIQUE KEY (email)
);

-- An associative table that maps a user to a role.
-- The application uses 'unauthorized', 'user' and 'admin' for now.
CREATE TABLE user_roles (
	email  VARCHAR(64) NOT NULL,
	role VARCHAR(24) NOT NULL,
	PRIMARY KEY (email, role),
	CONSTRAINT user_roles_email FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE ON UPDATE CASCADE
);

-- A history record of all logins to the system
CREATE TABLE logins (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	login_date TIMESTAMP NOT NULL,
	ip_address VARCHAR(15) DEFAULT "0.0.0.0",
	browser_agent TEXT,
	PRIMARY KEY (id),
	CONSTRAINT logins_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

-- A convienience table, this contains a specific set of
-- permissions that can be associated with a user (or set as
-- the default of a space so when a new user is ed, they take on
-- these permissions)
CREATE TABLE permissions (
	id INT NOT NULL AUTO_INCREMENT, 
	add_solver BOOLEAN DEFAULT 0,
	add_bench BOOLEAN DEFAULT 0,
	add_user BOOLEAN DEFAULT 0,
	add_space BOOLEAN DEFAULT 0,
	add_job BOOLEAN DEFAULT 0,
	remove_solver BOOLEAN DEFAULT 0,
	remove_bench BOOLEAN DEFAULT 0,
	remove_user BOOLEAN DEFAULT 0,
	remove_space BOOLEAN DEFAULT 0,
	remove_job BOOLEAN DEFAULT 0,
	is_leader BOOLEAN DEFAULT 0,
	PRIMARY KEY(id)
);

-- Default permission for the root space
INSERT INTO permissions VALUES (1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

-- All of the spaces in starexec. A space is simply a set where
-- solvers, benchmarks, users and jobs exist (think of it as a folder)
CREATE TABLE spaces (
	id INT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(255) NOT NULL,
	created TIMESTAMP DEFAULT 0,
	description TEXT,
	locked BOOLEAN DEFAULT 0,
	default_permission INT,
	public_access BOOLEAN DEFAULT 0,
	sticky_leaders BOOLEAN DEFAULT 0,
	PRIMARY KEY (id),
	CONSTRAINT spaces_default_permission FOREIGN KEY (default_permission) REFERENCES permissions(id) ON DELETE SET NULL
);

-- The set of all associations between each node and it's descendants
-- (see the hierarchical data represendation PDF on the wiki for more details)
CREATE TABLE closure (
	ancestor INT NOT NULL,
	descendant INT NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	CONSTRAINT closure_ancestor FOREIGN KEY (ancestor) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT closure_descendant FOREIGN KEY (descendant) REFERENCES spaces(id) ON DELETE CASCADE
);

-- The root space
INSERT INTO spaces (name, created, description, locked, default_permission, public_access) VALUES 
('root', SYSDATE(), 'this is the starexec container space which holds all communities.', 1, 1, 1);
INSERT INTO closure VALUES(1,1);

-- All pre, post and bench processors in the system
CREATE TABLE processors (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(64) NOT NULL,
	description TEXT,
	path TEXT NOT NULL,
	community INT NOT NULL,
	processor_type TINYINT DEFAULT 0,
	disk_size BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT processors_community FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
);


-- The record for an individual benchmark
CREATE TABLE benchmarks (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	name VARCHAR(256) NOT NULL,
	bench_type INT,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 1,
	disk_size BIGINT NOT NULL,
	deleted BOOLEAN DEFAULT FALSE,
	recycled BOOLEAN DEFAULT FALSE,
	PRIMARY KEY (id),
	CONSTRAINT benchmarks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT benchmarks_bench_type FOREIGN KEY (bench_type) REFERENCES processors(id) ON DELETE SET NULL
);

-- All attributes for each benchmark
CREATE TABLE bench_attributes (
	bench_id INT NOT NULL,
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	PRIMARY KEY (bench_id, attr_key),
	CONSTRAINT bench_attributes_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- This table holds the names of executable types so that they are accessible for SQL sorts and filters.
-- The contents in this table should match with the enum in Solver.java to ensure proper sorts!
CREATE TABLE executable_types (
	type_id INT NOT NULL,
	type_name VARCHAR(32),
	PRIMARY KEY (type_id)
);

INSERT INTO executable_types (type_id, type_name) VALUES (1,"solver"), (2,"transformer"),(3,"result checker"),(4,"other");

-- The record for an individual solver
CREATE TABLE solvers (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	name VARCHAR(128) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 0,
	disk_size BIGINT NOT NULL,
	deleted BOOLEAN DEFAULT FALSE,
	recycled BOOLEAN DEFAULT FALSE,
	executable_type INT DEFAULT 1, 
	PRIMARY KEY (id),	
	CONSTRAINT solvers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT solvers_executable_type FOREIGN KEY (executable_type) REFERENCES executable_types(type_id) ON DELETE SET NULL 
);

-- All the configurations that belong to a solver. A solver
-- may have different settings that the user wants to run it with,
-- so they provide one or more configuration that tells us how they want
-- us to run their solver.
CREATE TABLE configurations (
	id INT NOT NULL AUTO_INCREMENT,
	solver_id INT,
	name VARCHAR(128) NOT NULL,
	description TEXT,
	updated TIMESTAMP NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT configurations_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- All the SGE node queues on the system
CREATE TABLE queues (
	id INT NOT NULL AUTO_INCREMENT, 	
	name VARCHAR(64) NOT NULL,
	status VARCHAR(32),
	slots_used INTEGER DEFAULT 0,
	slots_reserved INTEGER DEFAULT 0,
	slots_free INTEGER DEFAULT 0,
	slots_total INTEGER DEFAULT 0,
	global_access BOOLEAN DEFAULT FALSE,
	cpuTimeout INT DEFAULT 259200,
	clockTimeout INT DEFAULT 259200, -- timeouts are maxes for any jobs created on the queue
	PRIMARY KEY (id),
	UNIQUE KEY (name)
);

-- All the SGE worker nodes that jobs can be executed on in the cluster.
-- This just maintains hardware information manually to be viewed by
-- TODO: Do we actually want any node data except these three columns?
CREATE TABLE nodes (
	id INT NOT NULL AUTO_INCREMENT, 	
	name VARCHAR(128) NOT NULL,
	status VARCHAR(32),
	PRIMARY KEY (id),
	UNIQUE KEY (name)
);

-- All the SGE node queues on the system
CREATE TABLE queue_assoc (
	queue_id INT NOT NULL, 	
	node_id INT NOT NULL,	
	PRIMARY KEY (queue_id, node_id),
	CONSTRAINT queue_assoc_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE,
	CONSTRAINT queue_assoc_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
);

-- Association between exclusive queues and spaces (initially only communities)
CREATE TABLE comm_queue (
	space_id INT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE, 	
	queue_id INT NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
	PRIMARY KEY (space_id, queue_id)
);



-- table for storing the top level of solver pipelines. These should generally not be deleted
-- if there are jobs making use of them.
CREATE TABLE solver_pipelines (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(128),
	user_id INT NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	primary_stage_id INT, 
	PRIMARY KEY(id)
);
-- Stages for solver pipelines. Stages are ordered by their stage_id primary key
CREATE TABLE pipeline_stages (
	stage_id INT NOT NULL AUTO_INCREMENT, -- orders the stages of this pipeline
	pipeline_id INT NOT NULL,
	config_id INT, 
	is_noop BOOLEAN NOT NULL DEFAULT FALSE, -- note that we cannot say that this is a noop if config_id is null, because the config
								   -- could have just been deleted. We really do need to store this explicitly
	PRIMARY KEY (stage_id), -- pipelines can have many stages
	CONSTRAINT pipeline_stages_pipeline_id FOREIGN KEY (pipeline_id) REFERENCES solver_pipelines(id) ON DELETE CASCADE,
	CONSTRAINT pipeline_stages_config_id FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE SET NULL
);


-- Stores any dependencies that a particular stage has.
CREATE TABLE pipeline_dependencies (
	stage_id INT NOT NULL, -- ID of the stage that must recieve output from a previous stage
	
	input_type TINYINT NOT NULL, -- ID of the stage that produces the output
	input_id SMALLINT NOT NULL, -- if the type is an artifact, this is the the 1-indexed number of the stage that is needed
						   -- if the type is a benchmark, this is the the 1-indexed number of the benchmark that is needed
	input_number SMALLINT NOT NULL, -- which input to the stage is this? First input, second input, and so on
	PRIMARY KEY (stage_id, input_number), -- obviously a given stage may only have one dependency per number
	CONSTRAINT pipeline_dependencies_stage_id FOREIGN KEY (stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE CASCADE
);

-- All of the jobs within the system, this is the overarching entity
-- that contains individual job pairs (solver/config -> benchmark)
CREATE TABLE jobs (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,	
	name VARCHAR(64),
	queue_id INT,
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	completed TIMESTAMP,
	description TEXT,
	deleted BOOLEAN DEFAULT FALSE,
	paused BOOLEAN DEFAULT FALSE,
	killed BOOLEAN DEFAULT FALSE,
	seed BIGINT DEFAULT 0,
	cpuTimeout INT, 
	clockTimeout INT, 
	maximum_memory BIGINT DEFAULT 1073741824,
	primary_space INT, -- This is a JOB_SPACE, not simply a "space"
	suppress_timestamp BOOLEAN NOT NULL DEFAULT FALSE,
	PRIMARY KEY (id),
	CONSTRAINT jobs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT jobs_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE SET NULL
);
-- This table stores timeouts for individual pipeline stages for this job. 
-- These are essentially overrides for the columns in the jobs table
CREATE TABLE job_stage_params (
	job_id INT,
	stage_number INT,
	cpuTimeout INT, 
	clockTimeout INT,
	maximum_memory BIGINT DEFAULT 1073741824,
	space_id INT, -- if we're keeping benchmarks from this stage, where should we be putting them? null if not keeping them
	bench_suffix VARCHAR(64), -- if we're keeping benchmarks, what suffix? If none given, we use the suffix of the input benchmark
	post_processor INT,
	pre_processor INT,
	PRIMARY KEY (job_id,stage_number),
	CONSTRAINT job_stage_params_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	CONSTRAINT job_stage_params_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE SET NULL,
	CONSTRAINT job_stage_params_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL,
	CONSTRAINT job_stage_params_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL

);



-- Table which contains specific information about a job pair
-- When changing to using runsolver, wallclock changed from bigint to double
-- note: while some data is redundant in this table (solver_name, config_name, and so on), 
-- it is essential for speeding up queries.
CREATE TABLE job_pairs (
	id INT NOT NULL AUTO_INCREMENT,	
	job_id INT NOT NULL,
	sge_id INT,
	bench_id INT,
	bench_name VARCHAR(255),
	status_code TINYINT DEFAULT 0,
	node_id INT,
	queuesub_time TIMESTAMP DEFAULT 0,
	start_time TIMESTAMP DEFAULT 0,
	end_time TIMESTAMP DEFAULT 0,
	exit_status INT,
	job_space_id INT,
	path VARCHAR(2048),
	sandbox_num INT,
	primary_jobpair_data INT, -- which of this pairs stages is the primary one? references jobpair_stage_data.stage_number
	PRIMARY KEY(id),
	KEY(sge_id),
	KEY (job_space_id, bench_name),
	KEY (node_id, status_code),
--	KEY (status_code), -- TODO: Do we actually want this change?
	KEY (job_id, status_code), -- we very often get all pairs with a particular status code for a job
	CONSTRAINT job_pairs_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE, -- not necessary as an index
	CONSTRAINT job_pairs_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION -- not used as an index
);

CREATE TABLE jobpair_stage_data (
	stage_number INT NOT NULL, -- this id orders the stages
	jobpair_id INT NOT NULL,
	stage_id INT, -- References pipeline_stages stages are ordered by this ID as well.
	cpu DOUBLE,
	wallclock DOUBLE,
	max_vmem DOUBLE,
	max_res_set DOUBLE,
	user_time DOUBLE,
	system_time DOUBLE,
	status_code TINYINT DEFAULT 0,
	solver_name VARCHAR(128), -- These columns are redundant, but they allow us to keep stages even with deleted configs
	config_name VARCHAR(128),
	solver_id INT,
	config_id INT,
	job_space_id INT,
	KEY (job_space_id, config_id),
	KEY (job_space_id, solver_name),
	-- KEY (job_space_id, bench_name),
	KEY (job_space_id, config_name),
	PRIMARY KEY (jobpair_id,stage_number),
	CONSTRAINT jobpair_stage_data_jobpair_id FOREIGN KEY (jobpair_id) REFERENCES job_pairs(id) ON DELETE CASCADE,
	CONSTRAINT jobpair_stage_data_stage_id FOREIGN KEY (stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE SET NULL
);

-- this table stores, for every user, the difference in time
-- between that user's job pair wallclock timeouts and actual
-- runtime. This data is used by the JobManager for scheduling pairs
-- among different users. This data is stored only temporarily between calls
-- to submitJobs. It is completely wiped out after each call,
-- so data here is generally stored for only about 30 seconds
-- at a time.
CREATE TABLE jobpair_time_delta (
	user_id INT NOT NULL,
	queue_id INT NOT NULL,
	time_delta INT DEFAULT 0,
	PRIMARY KEY (user_id, queue_id),
	CONSTRAINT jobpair_time_delta_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE,
	CONSTRAINT jobpair_time_delta_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Stores all inputs to a particular job pair, outside of the primary benchmark
-- TODO: Do we want delete cascades on benchmarks? Might confuse users who accidentally delete benchmark inputs
CREATE TABLE jobpair_inputs (
	jobpair_id INT NOT NULL,
	input_number SMALLINT NOT NULL, -- ordered from 1 to n, with n being the number of inputs
	bench_id INT NOT NULL,
	PRIMARY KEY (jobpair_id,input_number),
	CONSTRAINT jobpair_inputs_jobpair_id FOREIGN KEY (jobpair_id) REFERENCES job_pairs(id) ON DELETE CASCADE,
	CONSTRAINT jobpair_inputs_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- Stores the IDs of completed jobs and gives each a completion ID, indicating order of completion
-- TODO: Consider eliminating this table, as we store end_time in the job_pairs table. Need to be careful porting 
-- over old pairs.
CREATE TABLE job_pair_completion (
	pair_id INT NOT NULL,
	completion_id INT NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (completion_id),
	UNIQUE KEY (pair_id),
	CONSTRAINT job_pair_completion_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);

-- All attributes for each job pair
CREATE TABLE job_attributes (
	pair_id INT NOT NULL, -- This column is not strictly necessary, but it might be useful for efficiency
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	job_id INT NOT NULL,
	stage_number INT NOT NULL, 
    PRIMARY KEY (pair_id,stage_number, attr_key),
    KEY (job_id),
	CONSTRAINT job_attributes_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);

-- The table that keeps track of verification codes that should
-- be redeemed when the user verifies their e-mail address
CREATE TABLE verify (
	user_id INT NOT NULL,
	code VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, code),
	UNIQUE KEY (user_id),
	UNIQUE KEY (code),
	CONSTRAINT verify_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Websites which are associated with either a space, solver or user.
-- It only makes sense to have one of the three id's set to a value,
-- they're all included for convienience though so we don't have to
-- have 3 redundant tables
CREATE TABLE website (
	id INT NOT NULL AUTO_INCREMENT, 
	space_id INT, 
	user_id INT,
	solver_id INT,
	url TEXT NOT NULL,
	name VARCHAR(64),
	PRIMARY KEY (id),
	CONSTRAINT website_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT website_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE,
	CONSTRAINT website_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE	
);

-- Which user belongs to which space.
CREATE TABLE user_assoc (
	user_id INT NOT NULL,
	space_id INT NOT NULL,	
	permission INT,
	PRIMARY KEY (user_id, space_id),
	CONSTRAINT user_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT user_assoc_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT user_assoc_permission FOREIGN KEY (permission) REFERENCES permissions(id) ON DELETE SET NULL
);

-- Which spaces exists within another space
CREATE TABLE set_assoc (
	space_id INT NOT NULL, 
	child_id INT NOT NULL,
	PRIMARY KEY (space_id, child_id),
	CONSTRAINT set_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT set_assoc_child_id FOREIGN KEY (child_id) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Which benchmarks belong to which spaces
CREATE TABLE bench_assoc (
	space_id INT NOT NULL, 
	bench_id INT NOT NULL,
	order_id INT NOT NULL AUTO_INCREMENT UNIQUE KEY,
	PRIMARY KEY (space_id, bench_id),
	CONSTRAINT bench_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT bench_assoc_bench_id FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- Which jobs belong to which spaces
CREATE TABLE job_assoc (
	space_id INT NOT NULL, 
	job_id INT NOT NULL,
	PRIMARY KEY (space_id, job_id),
	CONSTRAINT job_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT job_assoc_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Which solvers belong to which spaces
CREATE TABLE solver_assoc (
	space_id INT NOT NULL,
	solver_id INT NOT NULL,
	PRIMARY KEY (space_id, solver_id),
	CONSTRAINT solver_assoc_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT solver_assoc_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- Pending requests to join a community
-- Author: Todd Elvers
CREATE TABLE community_requests (
	user_id INT NOT NULL,
	community INT NOT NULL,
	code VARCHAR(36) NOT NULL,
	message TEXT NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, community),
	UNIQUE KEY (code),
	CONSTRAINT community_requests_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT community_requests_community FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
);

CREATE TABLE change_email_requests (
	user_id INT NOT NULL,
	new_email VARCHAR(64) NOT NULL,
	code VARCHAR(36) NOT NULL,
	PRIMARY KEY (user_id),
	UNIQUE KEY (code),
	CONSTRAINT change_email_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Pending requests to reset a user's password
-- Author: Todd Elvers
CREATE TABLE pass_reset_request (
	user_id INT NOT NULL,
	code VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, code),
	UNIQUE KEY (user_id),
	UNIQUE KEY (code),
	CONSTRAINT pass_reset_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Benchmark dependencies - e.g. a benchmark may reference other benchmarks such as axioms
-- Author: Benton McCune
CREATE TABLE bench_dependency (
	id INT NOT NULL AUTO_INCREMENT,
	primary_bench_id INT NOT NULL,
	secondary_bench_id INT NOT NULL,
	include_path TEXT not NULL,
	PRIMARY KEY (id),
	CONSTRAINT bench_dependency_primary_bench_id FOREIGN KEY (primary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE,
	CONSTRAINT bench_dependency_secondary_bench_id FOREIGN KEY (secondary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);



-- Default settings for a community space.
-- Author: Ruoyu Zhang + Eric Burns

CREATE TABLE default_settings (
	id INT NOT NULL AUTO_INCREMENT, -- unique ID
    prim_id INT, -- either a user ID or community ID depending on what the setting_type is
    post_processor INT,
    pre_processor INT,
    cpu_timeout INT DEFAULT 1,
	clock_timeout INT DEFAULT 1,
	dependencies_enabled BOOLEAN DEFAULT FALSE,
	maximum_memory BIGINT DEFAULT 1073741824,
	default_benchmark INT DEFAULT NULL,
	default_solver INT DEFAULT NULL,
	bench_processor INT,
	setting_type INT DEFAULT 1,
	name VARCHAR(32) DEFAULT "settings",
	PRIMARY KEY (id),
	CONSTRAINT default_settings_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL,
	CONSTRAINT default_settings_default_benchmark FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE SET NULL,
	CONSTRAINT default_settings_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL,
	CONSTRAINT default_settings_default_solver FOREIGN KEY (default_solver) REFERENCES solvers(id) ON DELETE SET NULL,
	CONSTRAINT default_settings_bench_processor FOREIGN KEY (bench_processor) REFERENCES processors(id) ON DELETE SET NULL
);


-- For Status Updates on a space XML upload
-- Author: Eric Burns
CREATE TABLE space_xml_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    user_id INT NOT NULL,
    upload_time TIMESTAMP NOT NULL,
    file_upload_complete BOOLEAN DEFAULT 0,
    everything_complete BOOLEAN DEFAULT 0,
    total_spaces INT DEFAULT 0,
    completed_spaces INT DEFAULT 0,
    total_benchmarks INT DEFAULT 0,
    completed_benchmarks INT DEFAULT 0,
    total_solvers INT DEFAULT 0,
    completed_solvers INT DEFAULT 0,
    total_updates INT DEFAULT 0,
    completed_updates INT DEFAULT 0,
    error_message TEXT,
	PRIMARY KEY (id),
	CONSTRAINT space_xml_uploads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- For Status Updates on a Benchmark upload
-- Author: Benton McCune
CREATE TABLE benchmark_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    space_id INT NOT NULL,
    user_id INT NOT NULL,
    upload_time TIMESTAMP NOT NULL,
    file_upload_complete BOOLEAN DEFAULT 0,
    file_extraction_complete BOOLEAN DEFAULT 0,
    processing_begun BOOLEAN DEFAULT 0,
    everything_complete BOOLEAN DEFAULT 0,
    total_spaces INT DEFAULT 0,
    total_benchmarks INT DEFAULT 0,
    validated_benchmarks INT DEFAULT 0,
    failed_benchmarks INT DEFAULT 0,
    completed_benchmarks INT DEFAULT 0,
    completed_spaces INT DEFAULT 0,
    error_message TEXT,
	PRIMARY KEY (id),
	CONSTRAINT benchmark_uploads_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT benchmark_uploads_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- For benchmarks that fail validation
-- Author: Benton McCune
CREATE TABLE unvalidated_benchmarks (
	id INT NOT NULL AUTO_INCREMENT, 
    status_id INT REFERENCES benchmark_uploads(id) ON DELETE CASCADE,
    bench_name VARCHAR(256) NOT NULL,
    error_message TEXT,
	PRIMARY KEY (id)
);

-- Saves all job space information
-- Author: Eric Burns
CREATE TABLE job_spaces (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(255),
	max_stages INT DEFAULT 1, -- This columns stores the maximum number of stages any job pair has
							  -- anywhere in the job space hierarchy rooted at this job space
	PRIMARY KEY (id)
);

-- The set of all associations between each job space and it's descendants
CREATE TABLE job_space_closure (
	ancestor INT NOT NULL,
	descendant INT NOT NULL,
	last_used TIMESTAMP NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	CONSTRAINT job_space_ancestor FOREIGN KEY (ancestor) REFERENCES job_spaces(id) ON DELETE CASCADE,
	CONSTRAINT job_space_descendant FOREIGN KEY (descendant) REFERENCES job_spaces(id) ON DELETE CASCADE
);


-- Saves associations between spaces relevent to a particular job
-- Author: Eric Burns
CREATE TABLE job_space_assoc (
	space_id INT NOT NULL,
	child_id INT NOT NULL,
	CONSTRAINT job_space_assoc_space_id FOREIGN KEY (space_id) REFERENCES job_spaces(id) ON DELETE CASCADE,
	CONSTRAINT job_space_assoc_child_id FOREIGN KEY (child_id) REFERENCES job_spaces(id) ON DELETE CASCADE
);
-- Stores a cache of stats for a particular job space. Incomplete pairs are not stored,
-- as we only store complete jobs, so incomplete=failed. Stats are hierarchical,
-- so stats at a particular job space include all pairs below that job space
-- Author: Eric Burns
CREATE TABLE job_stats (
	job_space_id INT NOT NULL,
	config_id INT NOT NULL,
	complete INT NOT NULL,
	correct INT NOT NULL,
	incorrect INT NOT NULL,
	incomplete INT NOT NULL,
	failed INT NOT NULL,
	wallclock DOUBLE,
	cpu DOUBLE,
	resource_out INT NOT NULL,
	stage_number INT NOT NULL DEFAULT 0, -- what stage is this? from 1...n, with 0 meaning the primary stage
	PRIMARY KEY (job_space_id,config_id,stage_number),
	CONSTRAINT job_stats_job_space_id FOREIGN KEY (job_space_id) REFERENCES job_spaces(id) ON DELETE CASCADE,
	KEY (config_id)
);

-- Associates space IDs with the cache of their downloads. cache_type refers to the type of the archive that is stored-- space,
-- solver, benchmark, job, etc
-- Author: Eric Burns
CREATE TABLE file_cache (
	id INT NOT NULL,
	path TEXT NOT NULL,
	cache_type INT NOT NULL,
	last_access TIMESTAMP NOT NULL,
	PRIMARY KEY (id,cache_type)
);

-- Table that contains some global flags
-- Author: Wyatt Kaiser
CREATE TABLE system_flags (
	integrity_keeper ENUM('') NOT NULL,
	paused BOOLEAN DEFAULT FALSE,
	test_queue INT,
	PRIMARY KEY (integrity_keeper),
	CONSTRAINT system_flags_test_queue FOREIGN KEY (test_queue) REFERENCES queues(id) ON DELETE SET NULL
);

-- table for storing statistics for the weekly report
CREATE TABLE report_data (
	id INT NOT NULL AUTO_INCREMENT,
	event_name VARCHAR(64),
	queue_id INT, -- NULL if data is not associated with a queue 
	occurrences INT NOT NULL,

	UNIQUE KEY(event_name, queue_id),
	PRIMARY KEY(id),
	CONSTRAINT report_data_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE NO ACTION
);

ALTER TABLE solver_pipelines ADD CONSTRAINT primary_stage_id FOREIGN KEY (primary_stage_id) REFERENCES pipeline_stages(stage_id) ON DELETE SET NULL;

ALTER TABLE users ADD CONSTRAINT users_default_settings_profile FOREIGN KEY (default_settings_profile) REFERENCES default_settings(id) ON DELETE SET NULL;


INSERT INTO report_data (event_name, queue_id, occurrences) VALUES ('unique logins', NULL, 0), ('jobs initiated', NULL, 0),
	('job pairs run', NULL, 0), ('solvers uploaded', NULL, 0), ('benchmarks uploaded', NULL, 0), ('benchmark archives uploaded', NULL, 0); 
