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
	PRIMARY KEY (id),
	UNIQUE KEY (email)
);

-- An associative table that maps a user to a role.
-- The application uses 'user' and 'admin' for now
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
-- the default of a space so when a new user is added, they take on
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

-- All of the spaces in starexec. A space is simply a set where
-- solvers, benchmarks, users and jobs exist (I like to think of it
-- as a folder)
CREATE TABLE spaces (
	id INT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(32) NOT NULL,
	created TIMESTAMP DEFAULT 0,
	description TEXT,
	locked BOOLEAN DEFAULT 0,
	default_permission INT,
	PRIMARY KEY (id),
	FOREIGN KEY (default_permission) REFERENCES permissions(id) ON DELETE SET NULL
);

-- All pre, post and bench processors in the system
CREATE TABLE processors (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(32) NOT NULL,
	description TEXT,
	path TEXT NOT NULL,
	community INT NOT NULL,
	processor_type TINYINT DEFAULT 0,
	PRIMARY KEY (id),
	FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
);

-- The record for an individual benchmark
CREATE TABLE benchmarks (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	name VARCHAR(32) NOT NULL,
	bench_type INT,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 1,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
	FOREIGN KEY (bench_type) REFERENCES processors(id) ON DELETE NO ACTION
);

-- The record for an individual solver
CREATE TABLE solvers (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,
	name VARCHAR(32) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 0,
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
	name VARCHAR(64) NOT NULL,
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
INSERT INTO status_codes VALUES (6, 'awaiting statistics', 'the job has completed execution and is waiting for its runtime statistics from the grid engine');
INSERT INTO status_codes VALUES (7, 'complete', 'the job has successfully completed execution and its statistics have been received from the grid engine');
INSERT INTO status_codes VALUES (8, 'statistics error', 'the job completed execution but there was a problem accuiring its statistics from the grid engine');
INSERT INTO status_codes VALUES (9, 'run script error', 'the job could not be executed because a valid run script was not present');
INSERT INTO status_codes VALUES (10, 'benchmark error', 'the job could not be executed because the benchmark could not be found');
INSERT INTO status_codes VALUES (11, 'environment error', 'the job could not be executed because its execution environment could not be properly set up');
INSERT INTO status_codes VALUES (12, 'error', 'an unknown error occurred which indicates a problem at any point in the job execution pipeline');

-- All of the jobs within the system, this is the overarching entity
-- that contains individual job pairs (solver/config -> benchmark)
CREATE TABLE jobs (
	id INT NOT NULL AUTO_INCREMENT,
	user_id INT NOT NULL,	
	name VARCHAR(32),
	queue_id INT,
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	pre_processor INT,
	post_processor INT,	
	timeout BIGINT,
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
	name VARCHAR(32) NOT NULL,
	description TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- Table which contains specific information about a job pai
CREATE TABLE job_pairs (
	id INT NOT NULL AUTO_INCREMENT,	
	job_id INT NOT NULL,
	sge_id INT,
	bench_id INT,
	config_id INT,		
	status_code TINYINT DEFAULT 0,
	short_result VARCHAR(64) DEFAULT "",
	node_id INT,
	queuesub_time TIMESTAMP DEFAULT 0,
	start_time TIMESTAMP DEFAULT 0,
	end_time TIMESTAMP DEFAULT 0,
	exit_status INT,
	wallclock BIGINT,
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
	PRIMARY KEY(id),
	UNIQUE KEY(sge_id),
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE SET NULL,
	FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE SET NULL,
	FOREIGN KEY (status_code) REFERENCES status_codes(code) ON DELETE NO ACTION,
	FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION
);

-- The set of all associations between each node and it's descendants
-- (see the hierarchical data represendation PDF on the wiki for more details)
CREATE TABLE closure (
	ancestor INT NOT NULL,
	descendant INT NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	FOREIGN KEY (ancestor) REFERENCES spaces(id),
	FOREIGN KEY (descendant) REFERENCES spaces(id) ON DELETE CASCADE
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