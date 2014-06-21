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
	PRIMARY KEY (id),
	UNIQUE KEY (email)
);

-- An associative table that maps a user to a role.
-- The application uses 'unauthorized', 'user' and 'admin' for now.
CREATE TABLE user_roles (
	email  VARCHAR(64) NOT NULL,
	role VARCHAR(24) NOT NULL,
	PRIMARY KEY (email, role),
	CONSTRAINT user_roles_email FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE
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

-- The default 'no type' benchmark processor
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES 
('no_type', 'this is the default benchmark type for rejected benchmarks and benchmarks that are not associated with a type.', '/home/starexec/processor_scripts', 1, 3, 145);

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
	CONSTRAINT benchmarks_user_iddas FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
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

-- The record for an individual solver
-- TODO: Should we be setting user ids to null maybe? Or 0?
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
	PRIMARY KEY (id),	
	CONSTRAINT solvers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION
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
	permanent BOOLEAN DEFAULT FALSE,
	global_access BOOLEAN DEFAULT FALSE,
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
	CONSTRAINT queue_assoc_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE,
	CONSTRAINT queue_assoc_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
);

-- Association between exclusive queues and spaces (initially only communities)
CREATE TABLE comm_queue (
	space_id INT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE, 	
	queue_id INT NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
	PRIMARY KEY (space_id, queue_id)
);

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
	deleted BOOLEAN DEFAULT FALSE,
	paused BOOLEAN DEFAULT FALSE,
	killed BOOLEAN DEFAULT FALSE,
	seed BIGINT DEFAULT 0,
	primary_space INT, -- This is a JOB_SPACE, not simply a "space"
	PRIMARY KEY (id),
	CONSTRAINT jobs_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE NO ACTION,
	CONSTRAINT jobs_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE SET NULL,
	CONSTRAINT jobs_pre_processor FOREIGN KEY (pre_processor) REFERENCES processors(id) ON DELETE SET NULL,
	CONSTRAINT jobs_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL
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

-- Table which contains specific information about a job pair
-- When changing to using runsolver, wallclock changed from bigint to double
-- note: while some data is redundant in this table (solver_name, config_name, and so on), 
-- it is essential for speeding up queries.
CREATE TABLE job_pairs (
	id INT NOT NULL AUTO_INCREMENT,	
	job_id INT NOT NULL,
	sge_id INT,
	bench_id INT,
	bench_name VARCHAR(256),
	config_id INT,	
	solver_id INT,
	config_name VARCHAR(256),
	solver_name VARCHAR(256),
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
	job_space_id INT,
	path VARCHAR(2048),
	maximum_memory BIGINT DEFAULT 1073741824,
	PRIMARY KEY(id),
	KEY(sge_id),
	KEY (job_space_id, config_id),
	KEY (job_space_id, solver_name),
	KEY (job_space_id, bench_name),
	KEY (job_space_id, config_name),
	KEY (node_id, status_code),
--	KEY (status_code), -- TODO: Do we actually want this change
	KEY (job_id, status_code), -- we very often get all pairs with a particular status code for a job
	CONSTRAINT job_pairs_job_id FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE, -- not necessary as an index
	CONSTRAINT job_pairs_node_id FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE NO ACTION, -- not used as an index
	CONSTRAINT job_pairs_solver_id FOREIGN KEY (solver_id) REFERENCES solvers(id) ON DELETE SET NULL -- not used as an index
);

-- Stores the IDs of completed jobs and gives each a completion ID, indicating order of completion
CREATE TABLE job_pair_completion (
	pair_id INT NOT NULL,
	completion_id INT NOT NULL AUTO_INCREMENT,
	PRIMARY KEY (completion_id),
	CONSTRAINT job_pair_completion_pair_id FOREIGN KEY (pair_id) REFERENCES job_pairs(id) ON DELETE CASCADE
);

-- All attributes for each job pair
CREATE TABLE job_attributes (
	pair_id INT NOT NULL,
	attr_key VARCHAR(128) NOT NULL,
	attr_value VARCHAR(128) NOT NULL,
	job_id INT NOT NULL,
    PRIMARY KEY (pair_id, attr_key),
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

-- Pending requests to reserve a queue
-- Author: Wyatt Kaiser
CREATE TABLE queue_request (
	user_id INT NOT NULL,
	space_id INT NOT NULL,
	queue_name VARCHAR(64) NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	message TEXT NOT NULL,
	code VARCHAR(36) NOT NULL,
	created TIMESTAMP NOT NULL,	
	PRIMARY KEY (user_id, space_id, queue_name, reserve_date),
	CONSTRAINT queue_request_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT queue_request_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
);

-- Reserved queues. includes future reservations and current reservations
-- Author: Wyatt Kaiser
CREATE TABLE queue_reserved (
	space_id INT NOT NULL,
	queue_id INT NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	message TEXT NOT NULL,
	PRIMARY KEY (space_id, queue_id, reserve_date),
	CONSTRAINT queue_reserved_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT queue_reserved_queue_id FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE
);

-- The history of queue_reservations (i.e. reservations that happened in the past)
-- Author: Wyatt Kaiser
CREATE TABLE reservation_history (
	space_id INT NOT NULL,
	queue_name VARCHAR(64) NOT NULL,
	node_count INT NOT NULL,
	start_date DATE NOT NULL,
	end_date DATE NOT NULL,
	message TEXT NOT NULL,
	PRIMARY KEY (queue_name, start_date)
);

-- Includes temporary data when editing node_count information
-- Author: Wyatt Kaiser
CREATE TABLE temp_node_changes (
	space_id INT NOT NULL,
	queue_name VARCHAR(64) NOT NULL,
	node_count INT NOT NULL,
	reserve_date DATE NOT NULL,
	PRIMARY KEY (space_id, queue_name, reserve_date),
	CONSTRAINT temp_node_changes_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE
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

CREATE TABLE space_default_settings (
    space_id INT,
    post_processor INT,
    cpu_timeout INT DEFAULT 1,
	clock_timeout INT DEFAULT 1,
	dependencies_enabled BOOLEAN DEFAULT FALSE,
	default_benchmark INT DEFAULT NULL,
	maximum_memory BIGINT DEFAULT 1073741824,
	PRIMARY KEY (space_id),
	CONSTRAINT space_default_settings_space_id FOREIGN KEY (space_id) REFERENCES spaces(id) ON DELETE CASCADE,
	CONSTRAINT space_default_settings_post_processor FOREIGN KEY (post_processor) REFERENCES processors(id) ON DELETE SET NULL,
	CONSTRAINT space_default_settings_default_benchmark FOREIGN KEY (default_benchmark) REFERENCES benchmarks(id) ON DELETE SET NULL
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
    error_message VARCHAR(512) DEFAULT "no error",
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
	PRIMARY KEY (id)
);

-- Saves all job space information
-- Author: Eric Burns
CREATE TABLE job_spaces (
	id INT NOT NULL AUTO_INCREMENT,
	name VARCHAR(128),
	PRIMARY KEY (id)
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
-- as we only store complete jobs, so incomplete=failed
-- Author: Eric Burns
CREATE TABLE job_stats (
	job_space_id INT NOT NULL,
	config_id INT NOT NULL,
	complete INT NOT NULL,
	correct INT NOT NULL,
	incorrect INT NOT NULL,
	failed INT NOT NULL,
	wallclock DOUBLE,
	cpu DOUBLE,
	resource_out INT NOT NULL,
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
	PRIMARY KEY (integrity_keeper)
);

