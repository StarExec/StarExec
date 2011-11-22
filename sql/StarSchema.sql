-- Author: Tyler Jensen
-- Description: This file is the schema used to create the starexec database in MySQL

-- Drop and recreate the database to get a fresh slate
DROP DATABASE IF EXISTS starexec;
CREATE DATABASE starexec;

USE starexec;

-- The table of all users in the system
CREATE TABLE users (
	id BIGINT NOT NULL AUTO_INCREMENT,	
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
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	login_date TIMESTAMP NOT NULL,
	ip_address VARCHAR(15) DEFAULT "0.0.0.0",
	browser_agent TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

-- The record for an individual benchmark
CREATE TABLE benchmarks (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	name VARCHAR(32) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 1,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

-- The record for an individual solver
CREATE TABLE solvers (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	name VARCHAR(32) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,
	description TEXT,
	downloadable BOOLEAN DEFAULT 0,
	PRIMARY KEY (id),	
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

-- All the classes of nodes on the system. A class is
-- a specific hardware configuration common across more than one node
CREATE TABLE node_class (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(32) NOT NULL,
	PRIMARY KEY (id)
);

-- All the worker nodes that jobs can be executed on in the cluster.
-- This just maintains hardware information manually to be viewed by
CREATE TABLE nodes (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	class_id BIGINT NOT NULL,
	name VARCHAR(32) NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (class_id) REFERENCES node_class(id) ON DELETE NO ACTION
);

-- All of the jobs within the system, this is the overarching entity
-- that contains individual job pairs (solver/config -> benchmark)
CREATE TABLE jobs (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	node_class BIGINT,
	name VARCHAR(32),
	submitted TIMESTAMP DEFAULT 0,
	finished TIMESTAMP DEFAULT 0,
	timeout BIGINT,
	status VARCHAR(24) DEFAULT "Ready",
	description TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
	FOREIGN KEY (node_class) REFERENCES node_class(id) ON DELETE NO ACTION	
);

-- All the configurations that belong to a solver. A solver
-- may have different settings that the user wants to run it with,
-- so they provide one or more configuration that tells us how they want
-- us to run their solver.
CREATE TABLE configurations (
	id BIGINT NOT NULL AUTO_INCREMENT,
	solver_id BIGINT NOT NULL,
	name VARCHAR(32) NOT NULL,
	description TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- All the job pairs in starexec which are configuration to benchmark mappings.
-- This is the smallest atomic 'unit' that is ran on a worker node. Note a job
-- pair may be shared by different jobs, this is a sort of 'cache' or pre-made pairs
CREATE TABLE job_pairs (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	config_id BIGINT NOT NULL,
	bench_id BIGINT NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY (config_id, bench_id),
	FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE NO ACTION,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE NO ACTION
);

-- Table which contains specific information about a job pair run
-- that is unique to the job it ran under (these will not belong to
-- more than one job unlike the job_pairs table)
CREATE TABLE job_pair_attr (
	id BIGINT NOT NULL AUTO_INCREMENT,
	job_id BIGINT NOT NULL,
	pair_id BIGINT NOT NULL,
	node_id BIGINT NOT NULL,
	start TIMESTAMP DEFAULT 0,
	stop TIMESTAMP DEFAULT 0,
	result VARCHAR(64) DEFAULT "",
	status VARCHAR(64) DEFAULT "",
	PRIMARY KEY(id),
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
	FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE NO ACTION,
	FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION
);

-- A convienience table, this contains a specific set of
-- permissions that can be associated with a user (or set as
-- the default of a space so when a new user is added, they take on
-- these permissions)
CREATE TABLE permissions (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	add_solver BOOLEAN DEFAULT 0,
	add_bench BOOLEAN DEFAULT 0,
	add_user BOOLEAN DEFAULT 0,
	add_space BOOLEAN DEFAULT 0,
	remove_solver BOOLEAN DEFAULT 0,
	remove_bench BOOLEAN DEFAULT 0,
	remove_user BOOLEAN DEFAULT 0,
	remove_space BOOLEAN DEFAULT 0,
	is_leader BOOLEAN DEFAULT 0,
	PRIMARY KEY(id)
);

-- All of the spaces in starexec. A space is simply a set where
-- solvers, benchmarks, users and jobs exist (I like to think of it
-- as a folder)
CREATE TABLE spaces (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(32) NOT NULL,
	created TIMESTAMP DEFAULT 0,
	description TEXT,
	locked BOOLEAN DEFAULT 0,
	default_permission BIGINT NOT NULL,
	PRIMARY KEY (id),
	FOREIGN KEY (default_permission) REFERENCES permissions(id) ON DELETE CASCADE
);

-- The set of all associations between each node and it's descendants
-- (see the hierarchical data represendation PDF on the wiki for more details)
CREATE TABLE closure (
	ancestor BIGINT NOT NULL,
	descendant BIGINT NOT NULL,
	UNIQUE KEY (ancestor, descendant),
	FOREIGN KEY (ancestor) REFERENCES spaces(id),
	FOREIGN KEY (descendant) REFERENCES spaces(id)
);

-- The table that keeps track of verification codes that should
-- be redeemed when the user verifies their e-mail address
CREATE TABLE verify (
	user_id BIGINT NOT NULL,
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
	id BIGINT NOT NULL AUTO_INCREMENT, 
	space_id BIGINT, 
	user_id BIGINT,
	solver_id BIGINT,
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
	user_id BIGINT NOT NULL,
	space_id BIGINT NOT NULL,	
	proxy BIGINT NOT NULL,
	permission BIGINT NOT NULL,
	PRIMARY KEY (user_id, space_id, proxy),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (permission) REFERENCES permissions(id) ON DELETE CASCADE,
	FOREIGN KEY (proxy) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Which spaces exists within another space
CREATE TABLE set_assoc (
	space_id BIGINT NOT NULL, 
	child_id BIGINT NOT NULL,
	permission BIGINT NOT NULL,
	PRIMARY KEY (space_id, child_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (child_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (permission) REFERENCES permissions(id) ON DELETE CASCADE
);

-- Which benchmarks belong to which spaces
CREATE TABLE bench_assoc (
	space_id BIGINT NOT NULL, 
	bench_id BIGINT NOT NULL,	
	PRIMARY KEY (space_id, bench_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

-- Which jobs belong to which spaces
CREATE TABLE job_assoc (
	space_id BIGINT NOT NULL, 
	job_id BIGINT NOT NULL,
	PRIMARY KEY (space_id, job_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Which solvers belong to which spaces
CREATE TABLE solver_assoc (
	space_id BIGINT NOT NULL,
	solver_id BIGINT NOT NULL,
	PRIMARY KEY (space_id, solver_id),
	FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

-- Pending requests to join a community
-- Author: Todd Elvers
CREATE TABLE community_requests (
	user_id BIGINT NOT NULL,
	community BIGINT NOT NULL,
	code VARCHAR(36) NOT NULL,
	message VARCHAR(300) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, community),
	UNIQUE KEY (code),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (community) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Pending requests to reset a user's password
-- Author: Todd Elvers
CREATE TABLE pass_reset (
	user_id BIGINT NOT NULL,
	code VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, code),
	UNIQUE KEY (user_id),
	UNIQUE KEY (code),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);