-- Author: Tyler N Jensen (tylernjensen@gmail.comid) --
-- Description: This inserts sample data into the starexec database --

-- TODO: Update this to match the new schema

USE starexec;

INSERT INTO users (email, first_name, last_name, institution, created, password)
	VALUES ('tyler-jensen@uiowa.edu', 'Tyler', 'Jensen', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86');
INSERT INTO users (email, first_name, last_name, institution, created, password)
	VALUES ('clifton-palmer@uiowa.edu', 'CJ', 'Palmer', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86');
INSERT INTO users (email, first_name, last_name, institution, created, password)
	VALUES ('aaron.stump.test@uiowa.edu', 'Aaron', 'Stump', 'University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86');
INSERT INTO users (email, first_name, last_name, institution, created, password)
	VALUES ('test.tinelli@uiowa.edu', 'Cesare', 'Tinelli', 'University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86');
INSERT INTO users (email, first_name, last_name, institution, created, password)
	VALUES ('skylar-stark@uiowa.edu', 'Skylar', 'Stark', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86');

INSERT INTO user_roles VALUES('tyler-jensen@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('clifton-palmer@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('aaron.stump.test@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('test.tinelli@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('skylar-stark@uiowa.edu', 'user');
	
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable) VALUES
	(1, 'Napalm', SYSDATE(), 'C:\Benchmark.smt2', 'This is a sample benchmark that is downloadable', 1);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable) VALUES
	(2, 'Roadrunner', SYSDATE(), 'C:\CJBenchmark.smt2', 'This is another sample benchmark that cant be downloaded', 0);
	
INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable) VALUES
	(1, 'CVC3', SYSDATE(), 'C:\\z3\\', 'This is a downloadable solver', 1);
INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable) VALUES
	(2, 'Vampire', SYSDATE(), 'C:\\vamp\\', 'This is a non-downloadable solver', 0);

INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "Default", "This is a default configuration for CVC3");
INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "Secondary", "This is a secondary configuration for CVC3");

INSERT INTO configurations(solver_id, name, description) VALUES
	(2, "ClockTime", "This is the first configuration for vampire");
INSERT INTO configurations(solver_id, name, description) VALUES
	(2, "NoTime", "This is a second configuration for vampire");
	
INSERT INTO node_class (name) VALUES ("dev cluster");
INSERT INTO nodes(class_id, name) VALUES (1, "starexec1.cs.uiowa.edu");
INSERT INTO nodes(class_id, name) VALUES (1, "starexec2.cs.uiowa.edu");
INSERT INTO nodes(class_id, name) VALUES (1, "starexec3.cs.uiowa.edu");
INSERT INTO nodes(class_id, name) VALUES (1, "starexec4.cs.uiowa.edu");
INSERT INTO nodes(class_id, name) VALUES (1, "starexec5.cs.uiowa.edu");
	
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, remove_solver, remove_bench, remove_user, remove_space, is_leader) VALUES
	(0, 0, 0, 0, 0, 0, 0, 0, 0);
	
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('root', SYSDATE(), 'this is the mother of all spaces', 1, 1);
	
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SMT', SYSDATE(), 'this is the SMT space, the child of the root space', 1, 1);	

INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SAT', SYSDATE(), 'this is the SAT space, the child of the root space', 0, 1);

INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('3SAT', SYSDATE(), 'this is the 3SAT space, the child of SAT space', 0, 1);
	
INSERT INTO jobs (user_id, name, status, description, submitted, finished) VALUES
	(1, 'Sweet Job', 'Finished', 'This is an example job description', SYSDATE(), DATE_ADD(SYSDATE(), INTERVAL 2 HOUR));
INSERT INTO jobs (user_id, name, status, description, submitted) VALUES
	(2, 'SMT Job', 'Running', 'This is another example job description', SYSDATE());

INSERT INTO job_pairs(config_id, bench_id) VALUES (1, 1);
INSERT INTO job_pairs(config_id, bench_id) VALUES (2, 2);

INSERT INTO job_pair_attr(job_id, pair_id, node_id, start, stop, result, status) values
	(1, 1, 2, SYSDATE(), SYSDATE(), "SAT", "Success");

INSERT INTO job_pair_attr(job_id, pair_id, node_id, start, stop, result, status) values
	(1, 2, 3, SYSDATE(), SYSDATE(), "Error", "Error");
	
INSERT INTO set_assoc VALUES (1, 2, 1);
INSERT INTO set_assoc VALUES (1, 3, 1);
INSERT INTO set_assoc VALUES (3, 4, 1);

INSERT INTO closure VALUES (1, 2);
INSERT INTO closure VALUES (1, 3);
INSERT INTO closure VALUES (1, 4);
INSERT INTO closure VALUES (3, 4);

INSERT INTO user_assoc VALUES (3, 2, 2, 1);
INSERT INTO user_assoc VALUES (4, 2, 2, 1);
INSERT INTO user_assoc VALUES (1, 3, 3, 1);
INSERT INTO user_assoc VALUES (1, 4, 3, 1);
INSERT INTO user_assoc VALUES (2, 3, 3, 1);

INSERT INTO bench_assoc VALUES (2, 1);
INSERT INTO bench_assoc VALUES (2, 2);
INSERT INTO bench_assoc VALUES (3, 2);

INSERT INTO solver_assoc VALUES (2, 1);
INSERT INTO solver_assoc VALUES (2, 2);
INSERT INTO solver_assoc VALUES (3, 1);

INSERT INTO job_assoc VALUES (2, 1);
INSERT INTO job_assoc VALUES (2, 2);

INSERT INTO website (user_id, name, url) VALUES (1, 'Personal', 'http://www.tylernjensen.com');
INSERT INTO website (user_id, name, url) VALUES (1, 'University', 'http://www.cs.uiowa.edu');
INSERT INTO website (solver_id, name, url) VALUES (1, 'Documentation', 'http://www.cs.uiowa.edu');

INSERT INTO website (space_id, user_id, solver_id, url, name) 
	VALUES (1, 5, 1, 'http://www.google.com/', 'google');
INSERT INTO website (space_id, user_id, solver_id, url, name) 
	VALUES (1, 5, 1, 'http://www.bing.com/', 'bing');
INSERT INTO website (space_id, user_id, solver_id, url, name) 
	VALUES (1, 5, 1, 'http://www.uiowa.edu/', 'uiowa');

-- More sample data to come!