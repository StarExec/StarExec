-- Author: Tyler N Jensen (tylernjensen@gmail.comid) --
-- Description: This inserts sample data into the starexec database --

-- TODO: Update this to match the new schema

USE starexec;

INSERT INTO users (email, first_name, last_name, institution, created, password, verified)
	VALUES ('tyler-jensen@uiowa.edu', 'Tyler', 'Jensen', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 1);
INSERT INTO users (email, first_name, last_name, institution, created, password, verified)
	VALUES ('clifton-palmer@uiowa.edu', 'CJ', 'Palmer', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 1);
	INSERT INTO users (email, first_name, last_name, institution, created, password, verified)
	VALUES ('skylar-stark@uiowa.edu', 'Skylar', 'Stark', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 1);

INSERT INTO user_roles VALUES('tyler-jensen@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('clifton-palmer@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('skylar-stark@uiowa.edu', 'user');
	
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable) VALUES
	(1, 'AmazingBench', SYSDATE(), 'C:\Benchmark.smt2', 'This is a sample benchmark', 1);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable) VALUES
	(2, 'CJBench', SYSDATE(), 'C:\CJBenchmark.smt2', 'This is another sample benchmark that cant be downloaded', 0);
	
INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable) VALUES
	(1, 'Z3', SYSDATE(), 'C:\\z3\\', 'This is a downloadable solver', 1);
INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable) VALUES
	(2, 'Vampire', SYSDATE(), 'C:\\vamp\\', 'This is a non-downloadable solver', 0);
	
-- More sample data to come!