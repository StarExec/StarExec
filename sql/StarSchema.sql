-- Author: Tyler Jensen
-- Description: This file is the schema used to create the starexec database in MySQL

-- Drop and recreate the database to get a fresh slate
DROP DATABASE IF EXISTS starexec;
CREATE DATABASE starexec;

USE starexec;

-- The table of all users in the system
CREATE TABLE users (
	id INT NOT NULL AUTO_INCREMENT,	
	email VARCHAR(64) NOT NULL,
	first_name VARCHAR(32) NOT NULL,
	last_name VARCHAR(32) NOT NULL,	
	institution VARCHAR(64) NOT NULL,
	created TIMESTAMP NOT NULL,
	password VARCHAR(128) NOT NULL,
	pref_archive_type VARCHAR(8) NOT NULL DEFAULT ".zip",
	disk_quota BIGINT NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY (email)
);

-- An associative table that maps a user to a role.
-- The application uses 'unauthorized', 'user' and 'admin' for now.
CREATE TABLE user_roles (
	email  VARCHAR(64) NOT NULL,
	role VARCHAR(24) NOT NULL,
	PRIMARY KEY (email, role),
	FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE
);

-- A history record of all logins to the system
CREATE TABLE logins (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	login_date TIMESTAMP NOT NULL,
	ip_address VARCHAR(15) DEFAULT "0.0.0.0",
	browser_agent TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
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
-- solvers, benchmarks, users and jobs exist (I like to think of it
-- as a folder)
CREATE TABLE spaces (
	id INT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(128) NOT NULL,
	created TIMESTAMP DEFAULT 0,
	description TEXT,
	locked BOOLEAN DEFAULT 0,
	default_permission INT,
	public_access BOOLEAN DEFAULT 0,
	PRIMARY KEY (id),
	FOREIGN KEY (default_permission) REFERENCES permissions(id) ON DELETE SET NULL
);

-- The set of all associations between each node and it's descendants
-- (see the hierarchical data represendation PDF on the wiki for more details)
CREATE TABLE closure (
	ancestor INT NOT NULL,
	descendant INT NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	FOREIGN KEY (ancestor) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (descendant) REFERENCES spaces(id) ON DELETE CASCADE
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
	FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
);

-- The default 'no type' benchmark processor
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES 
('no_type', 'this is the default benchmark type for rejected benchmarks and benchmarks that are not associated with a type.', '/home/starexec/processor_scripts/no-type.bash', 1, 3, 145);

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
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
	FOREIGN KEY (bench_type) REFERENCES processors(id) ON DELETE NO ACTION
);

-- All attributes for each benchmark
CREATE TABLE bench_attributes (
	bench_id INT NOT NULL,
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

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
	PRIMARY KEY (id),	
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
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
	PRIMARY KEY (id),
	UNIQUE KEY (name)
);

-- All the SGE worker nodes that jobs can be executed on in the cluster.
-- This just maintains hardware information manually to be viewed by
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
	FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE,
	FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
);

-- Association between exclusive queues and spaces (initially only communities)
CREATE TABLE comm_queue (
	space_id INT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE, 	
	queue_id INT NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
	PRIMARY KEY (space_id, queue_id)
);

-- Status codes for jobs
CREATE TABLE status_codes (
	code TINYINT NOT NULL,
	status VARCHAR(64) NOT NULL,
	description TEXT,
	PRIMARY KEY(code)
);

-- Job status codes here MUST match the enumerated types in Java
-- If these change, the prolog, epilog and status_codes scripts must be changed on the head node
INSERT INTO status_codes VALUES (0, 'unknown', 'the job status is not known or has not been set');
INSERT INTO status_codes VALUES (1, 'pending submission', 'the job has been added to the starexec database but has not been submitted to the grid engine');
INSERT INTO status_codes VALUES (2, 'enqueued', 'the job has been submitted to the grid engine and is waiting for an available execution host');
INSERT INTO status_codes VALUES (3, 'preparing', 'the jobs environment on an execution host is being prepared');
INSERT INTO status_codes VALUES (4, 'running', 'the job is currently being ran on an execution host');
INSERT INTO status_codes VALUES (5, 'finishing', 'the jobs output is being stored and its environment is being cleaned up');
INSERT INTO status_codes VALUES (6, 'awaiting results', 'the job has completed execution and is waiting for its runtime statistics and attributes from the grid engine');
INSERT INTO status_codes VALUES (7, 'complete', 'the job has successfully completed execution and its statistics have been received from the grid engine');
INSERT INTO status_codes VALUES (8, 'rejected', 'the job was sent to the grid engine for execution but was rejected. this can indicate that there were no available queues or the grid engine is in an unclean state');
INSERT INTO status_codes VALUES (9, 'submit failed', 'there was an issue submitting your job to the grid engine. this can be caused be unexpected errors raised by the grid engine');
INSERT INTO status_codes VALUES (10, 'results error', 'the job completed execution but there was a problem accuiring its statistics or attributes from the grid engine');
INSERT INTO status_codes VALUES (11, 'run script error', 'the job could not be executed because a valid run script was not present');
INSERT INTO status_codes VALUES (12, 'benchmark error', 'the job could not be executed because the benchmark could not be found');
INSERT INTO status_codes VALUES (13, 'environment error', 'the job could not be executed because its execution environment could not be properly set up');
INSERT INTO status_codes VALUES (14, 'timeout (wallclock)', 'the job was terminated because it exceeded its run time limit');
INSERT INTO status_codes VALUES (15, 'timeout (cpu)', 'the job was terminated because it exceeded its virtual memory or cpu time limit');
INSERT INTO status_codes VALUES (16, 'file write exceeded', 'the job was terminated because it exceeded its file write limit');
INSERT INTO status_codes VALUES (17, 'error', 'an unknown error occurred which indicates a problem at any point in the job execution pipeline');

-- All of the jobs within the system, this is the overarching entity
-- that contains individual job pairs (solver/config -> benchmark)
CREATE TABLE jobs (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,	
	name VARCHAR(64),
	queue_id INT,
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	pre_processor INT,
	post_processor INT,		
	description TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
	FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE SET NULL,
	FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL,
	FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL
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
	PRIMARY KEY (id),
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- Table which contains specific information about a job pair
-- When changing to using runsolver, wallclock changed from bigint to double
CREATE TABLE job_pairs (
	id INT NOT NULL AUTO_INCREMENT,	
	job_id INT NOT NULL,
	sge_id INT,
	bench_id INT,
	config_id INT,		
	status_code TINYINT DEFAULT 0,
	node_id INT,
	cpuTimeout INT,
	clockTimeout INT,
	queuesub_time TIMESTAMP DEFAULT 0,
	start_time TIMESTAMP DEFAULT 0,
	end_time TIMESTAMP DEFAULT 0,
	exit_status INT,
	wallclock DOUBLE,
	cpu DOUBLE,
	user_time DOUBLE,
	system_time DOUBLE,
	io_data DOUBLE,
	io_wait DOUBLE,
	mem_usage DOUBLE,
	max_vmem DOUBLE,
	max_res_set DOUBLE,
	page_reclaims DOUBLE,
	page_faults DOUBLE,
	block_input DOUBLE,
	block_output DOUBLE,
	vol_contex_swtch DOUBLE,
	invol_contex_swtch DOUBLE,
	space_id INT,
	PRIMARY KEY(id),
	UNIQUE KEY(sge_id),
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE SET NULL,
	FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE SET NULL,
	FOREIGN KEY (status_code) REFERENCES status_codes(code) ON DELETE NO ACTION,
	FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION,
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE SET NULL
);

-- All attributes for each job pair
CREATE TABLE job_attributes (
	job_id INT NOT NULL,
	pair_id INT NOT NULL,
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
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
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE	
);

-- Which user belongs to which space. Proxy simulates inheritance
-- by saying "I'm a member of this space because I'm in this proxy space"
CREATE TABLE user_assoc (
	user_id INT NOT NULL,
	space_id INT NOT NULL,	
	proxy INT NOT NULL,
	permission INT,
	PRIMARY KEY (user_id, space_id, proxy),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (permission) REFERENCES permissions(id) ON DELETE SET NULL,
	FOREIGN KEY (proxy) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Which spaces exists within another space
CREATE TABLE set_assoc (
	space_id INT NOT NULL, 
	child_id INT NOT NULL,
	PRIMARY KEY (space_id, child_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (child_id) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Which benchmarks belong to which spaces
CREATE TABLE bench_assoc (
	space_id INT NOT NULL, 
	bench_id INT NOT NULL,	
	PRIMARY KEY (space_id, bench_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- Which jobs belong to which spaces
CREATE TABLE job_assoc (
	space_id INT NOT NULL, 
	job_id INT NOT NULL,
	PRIMARY KEY (space_id, job_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Which solvers belong to which spaces
CREATE TABLE solver_assoc (
	space_id INT NOT NULL,
	solver_id INT NOT NULL,
	PRIMARY KEY (space_id, solver_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
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
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
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
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Benchmark dependencies - e.g. a benchmark may reference other benchmarks such as axioms
-- Author: Benton McCune
CREATE TABLE bench_dependency (
	id INT NOT NULL AUTO_INCREMENT,
	primary_bench_id INT NOT NULL,
	secondary_bench_id INT NOT NULL,
	include_path TEXT not NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (primary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE,
	FOREIGN KEY (secondary_bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- Comments which are associated with either a space, a solver, or a benchmark.
-- It only makes sense to have one of the two id's set to a value,
-- they're all included for convienience though so we don't have to
-- have 2 redundant tables
-- Author : Vivek Sardeshmukh
CREATE TABLE comments (
	id INT NOT NULL AUTO_INCREMENT, 
	solver_id INT, 
	benchmark_id INT,
	space_id INT,
	user_id INT,
	cmt_date TIMESTAMP NOT NULL,
	cmt TEXT NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (benchmark_id) REFERENCES benchmarks(id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE	
);

-- Default settings for a community space.
-- The settings contains post processor, CPU timeout and clock timeout at the moment.
-- Author: Ruoyu Zhang

CREATE TABLE space_default_settings (
    space_id INT,
    post_processor INT,
    cpu_timeout INT DEFAULT 1,
	clock_timeout INT DEFAULT 1,
	dependencies_enabled BOOLEAN DEFAULT FALSE,
	PRIMARY KEY (space_id, post_processor, cpu_timeout, clock_timeout, dependencies_enabled),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE CASCADE
);

-- For Status Updates on a Benchmark upload
-- Author: Benton McCune
CREATE TABLE benchmark_uploads (
	id INT NOT NULL AUTO_INCREMENT, 
    space_id INT REFERENCES spaces(id) ON DELETE CASCADE,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
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
    error_message VARCHAR(512) DEFAULT "no error",
	PRIMARY KEY (id)
);

-- For benchmarks that fail validation
-- Author: Benton McCune
CREATE TABLE unvalidated_benchmarks (
	id INT NOT NULL AUTO_INCREMENT, 
    status_id INT REFERENCES benchmark_uploads(id) ON DELETE CASCADE,
    bench_name VARCHAR(256) NOT NULL,
	PRIMARY KEY (id)
);

