-- Author: Tyler N Jensen (tylernjensen@gmail.com) --
-- Description: This file is the schema used to create the starexec database in MySQL --

DROP DATABASE IF EXISTS starexec;
CREATE DATABASE starexec;

USE starexec;

CREATE TABLE users (
	id BIGINT NOT NULL AUTO_INCREMENT,	
	email VARCHAR(64) NOT NULL,
	first_name VARCHAR(32) NOT NULL,
	last_name VARCHAR(32) NOT NULL,	
	institution VARCHAR(64) DEFAULT "None",
	created TIMESTAMP NOT NULL,
	password VARCHAR(128) NOT NULL,		
	verified BOOLEAN DEFAULT 0,
	PRIMARY KEY (id),
	UNIQUE KEY (email)
);

CREATE TABLE user_roles (
	email  VARCHAR(64) NOT NULL,
	role VARCHAR(24) NOT NULL,
	PRIMARY KEY (email, role),
	FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE
);

CREATE TABLE logins (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,
	login_date TIMESTAMP NOT NULL,
	ip_address VARCHAR(15) DEFAULT "0.0.0.0",
	browser_agent TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE TABLE verify (
	user_id BIGINT NOT NULL,
	code VARCHAR(36) NOT NULL,	
	PRIMARY KEY (user_id, code),
	UNIQUE KEY (user_id),
	UNIQUE KEY (code),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE benchmarks (
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,	
	name VARCHAR(32) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,	
	description TEXT,
	PRIMARY KEY (id),	
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE TABLE solvers (	
	id BIGINT NOT NULL AUTO_INCREMENT,
	user_id BIGINT NOT NULL,	
	name VARCHAR(32) NOT NULL,
	uploaded TIMESTAMP NOT NULL,
	path TEXT NOT NULL,	
	description TEXT,
	PRIMARY KEY (id),	
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE TABLE node_class (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	name VARCHAR(32) NOT NULL,
	PRIMARY KEY (id)
);

CREATE TABLE nodes (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	class_id BIGINT NOT NULL,	
	name VARCHAR(32) NOT NULL,		
	PRIMARY KEY (id),
	FOREIGN KEY (class_id) REFERENCES node_class(id) ON DELETE NO ACTION
);

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

CREATE TABLE configurations (
	id BIGINT NOT NULL AUTO_INCREMENT,
	solver_id BIGINT NOT NULL,
	name VARCHAR(32) NOT NULL,
	description TEXT,	
	PRIMARY KEY (id),
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);

CREATE TABLE job_pairs (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	config_id BIGINT NOT NULL,
	bench_id BIGINT NOT NULL,
	PRIMARY KEY (id),
	UNIQUE KEY (config_id, bench_id),
	FOREIGN KEY (config_id) REFERENCES configurations(id) ON DELETE NO ACTION,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE NO ACTION
);
	
CREATE TABLE sets (
	id BIGINT NOT NULL AUTO_INCREMENT, 
	user_id BIGINT NOT NULL,	
	name VARCHAR(32) NOT NULL,	
	created TIMESTAMP DEFAULT 0,
	description TEXT,
	PRIMARY KEY (id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
);

CREATE TABLE user_assoc (
	set_id BIGINT NOT NULL, 
	user_id BIGINT NOT NULL,	
	permission INT DEFAULT 0,
	PRIMARY KEY (set_id, user_id),
	FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE set_assoc (
	set_id BIGINT NOT NULL, 
	child_id BIGINT NOT NULL,	
	PRIMARY KEY (set_id, child_id),
	FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
	FOREIGN KEY (child_id) REFERENCES sets(id) ON DELETE CASCADE,
);

CREATE TABLE bench_assoc (
	set_id BIGINT NOT NULL, 
	bench_id BIGINT NOT NULL,	
	PRIMARY KEY (set_id, bench_id),
	FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
	FOREIGN KEY (bench_id) REFERENCES benchmarks(id) ON DELETE CASCADE
);

CREATE TABLE job_assoc (
	set_id BIGINT NOT NULL, 
	job_id BIGINT NOT NULL,	
	PRIMARY KEY (set_id, job_id),
	FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
	FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE TABLE solver_assoc (
	set_id BIGINT NOT NULL, 
	solver_id BIGINT NOT NULL,	
	PRIMARY KEY (set_id, solver_id),
	FOREIGN KEY (set_id) REFERENCES sets(id) ON DELETE CASCADE,
	FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE CASCADE
);