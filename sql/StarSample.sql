-- Author: Tyler N Jensen (tylernjensen@gmail.comid) --
-- Description: This inserts sample data into the starexec database --

-- TODO: Update this to match the new schema

USE dev_tj_starexec;

INSERT INTO communities (name) VALUES ('SAT');
INSERT INTO communities (name) VALUES ('SMT');
INSERT INTO communities (name) VALUES ('ATP');

INSERT INTO users (fname, lname, affiliation, created, email, password)
	VALUES ('Tyler', 'Jensen', 'The University of Iowa', SYSDATE(), 'tyler-jensen@uiowa.edu', '5f4dcc3b5aa765d61d8327deb882cf99');
INSERT INTO users (fname, lname, affiliation, created, email, password)
	VALUES ('Clifton', 'Palmer', 'The University of Iowa', SYSDATE(), 'clifton-palmer@uiowa.edu', '5f4dcc3b5aa765d61d8327deb882cf99');
INSERT INTO users (fname, lname, created, email, password) 
	VALUES ('The', 'Admin', SYSDATE(), 'starexec@cs.uiowa.edu', '5f4dcc3b5aa765d61d8327deb882cf99');

INSERT INTO user_roles VALUES('tyler-jensen@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('clifton-palmer@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('starexec@cs.uiowa.edu', 'admin');
	
INSERT INTO user_community VALUES (1, 1, 1);
INSERT INTO user_community VALUES (2, 2, 1);
	
INSERT INTO levels (name, lft, rgt, gid, usr, dep, comid) VALUES
	('SMT', 1, 2, 0, 1, 0, 2);
	
INSERT INTO benchmarks (uploaded, physical_path, usr, lvl, comid) VALUES
	(NOW(), '/home/starexec/Benchmarks/model_6_66.smt2', 1, 1, 2);

INSERT INTO solvers (uploaded, name, path, usr, notes, comid) VALUES
 	(NOW(), 'Z3', '/home/starexec/Solvers/z3', 1, 'Test solver', 2);
	
-- INSERT INTO jobs VALUES (1, NOW(), NOW(), 'This is a sample job',  'Completed', 'Starexec-1', '9000', 1);
INSERT INTO configurations (sid, name, notes) VALUES (1, "run", "This is a drill.");
-- INSERT INTO job_pairs VALUES (1, 1, 1, 1, 'sat', DATE_SUB(NOW(), INTERVAL 14 MINUTE), NOW(), 'Starexec-1', 'Completed');


INSERT INTO benchmarks (uploaded, physical_path, usr, lvl, comid) VALUES
 	(NOW(), '/home/starexec/Benchmarks/int_incompleteness1.smt2', 1, 1, 2);
INSERT INTO benchmarks (uploaded, physical_path, usr, lvl, comid) VALUES
	(NOW(), '/home/starexec/Benchmarks/model_6_49.smt2', 1, 1, 2);
INSERT INTO benchmarks (uploaded, physical_path, usr, lvl, comid) VALUES
	(NOW(), '/home/starexec/Benchmarks/model_5_8.smt2', 1, 1, 2);
