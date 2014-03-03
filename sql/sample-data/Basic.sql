-- Author: Tyler Jensen
-- Description: This inserts sample data into the starexec database --

USE starexec;
/*
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('tyler-jensen@uiowa.edu', 'Tyler', 'Jensen', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('aaron.stump.test@uiowa.edu', 'Aaron', 'Stump', 'University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('tinelli.test@uiowa.edu', 'Cesare', 'Tinelli', 'University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('skylar-stark@uiowa.edu', 'Skylar', 'Stark', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('geoff@cs.miami.edu', 'Geoff', 'Sutcliffe', 'The University of Miami', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);		
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('todd.elvers@gmail.com', 'Todd', 'Elvers', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('john.doe@gmail.com', 'John', 'Doe', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);


INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota) VALUES ('public', 'Jane ', 'Doe', 'The Great Unwashed', SYSDATE(), 'd32997e9747b65a3ecf65b82533a4c843c4e16dd30cf371e8c81ab60a341de00051da422d41ff29c55695f233a1e06fac8b79aeb0a4d91ae5d3d18c8e09b8c73', 52428800);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota) VALUES ('admin@uiowa.edu', 'Admin', '', 'StarExec', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);

INSERT INTO user_roles VALUES('tyler-jensen@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('aaron.stump.test@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('tinelli.test@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('skylar-stark@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('geoff@cs.miami.edu', 'user');
INSERT INTO user_roles VALUES('todd.elvers@gmail.com', 'user');
INSERT INTO user_roles VALUES('public', 'user');
INSERT INTO user_roles VALUES('admin@uiowa.edu', 'admin');
*/
-- INSERT INTO nodes(name, status) VALUES ("starexec1x.cs.uiowa.edu", "ACTIVE");
INSERT INTO nodes(name, status) VALUES ("starexec2x.cs.uiowa.edu", "ACTIVE");
INSERT INTO nodes(name, status) VALUES ("starexec3x.cs.uiowa.edu", "ACTIVE");
INSERT INTO nodes(name, status) VALUES ("starexec4x.cs.uiowa.edu", "ACTIVE");
INSERT INTO nodes(name, status) VALUES ("starexec5x.cs.uiowa.edu", "ACTIVE");
	
-- Starts at 2 (the root default permission is defined in the schema)
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
	
-- Starts at 2 (the root space is defined in the schema)
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SMT', SYSDATE(), 'this is the SMT space, the child of the root space', 1, 2);	
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SAT', SYSDATE(), 'this is the SAT space, the child of the root space', 0, 3);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('3SAT', SYSDATE(), 'this is the 3SAT space, the child of SAT space', 0, 4);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('4SAT', SYSDATE(), 'this is the 3SAT space, the child of SAT space', 0, 4);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('TPTP', SYSDATE(), 'this is Geoffs example space', 0, 5);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('fakeSpace', SYSDATE(), 'this is the unviewable space for benchmarks and jobs from public', 0, 5);

INSERT INTO job_spaces(name) VALUES('SAT');	

INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('no_type', 'This is a sample SAT benchmark type', 'C:\\SATPROCESSOR', 1, 3, 1024);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('bench_1', 'This is a sample SAT benchmark type', 'C:\\SATPROCESSOR', 3, 3, 1024);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('bench_2', 'This is a sample SAT benchmark type', 'C:\\SATPROCESSOR', 3, 3, 1024);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_PRE', 'This is a sample SAT pre processor', 'C:\\SATPPREROCESSOR', 3, 1, 10240);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_POST1', 'This is a sample SAT post processor', 'C:\\SATPOSTPROCESSOR', 3, 2, 102400);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_POST2', 'This is a sample SAT post processor', 'C:\\SATPOSTPROCESSOR', 3, 2, 102400);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_POST3', 'This is a sample SAT post processor', 'C:\\SATPOSTPROCESSOR', 3, 2, 102400);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_POST4', 'This is a sample SAT post processor', 'C:\\SATPOSTPROCESSOR', 3, 2, 102400);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SAT_LIB_POST5', 'This is a sample SAT post processor', 'C:\\SATPOSTPROCESSOR', 3, 2, 102400);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('SMT_LIB_V1', 'This is a sample SMT benchmark type', 'C:\\SMTPROCESSOR', 2, 3, 1024000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V1', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V2', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V3', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V4', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V5', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
INSERT INTO processors (name, description, path, community, processor_type, disk_size) VALUES
	('TPTP_LIB_V6', 'This is a sample TPTP benchmark type', 'C:\\TPTPPROCESSOR', 6, 2, 10240000);
	
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(1, 'Gasburner', SYSDATE(), '/home/starexec/benchmarks/gasburner-prop3-2.smt2', 'This is a sample benchmark that is downloadable', 1, 1, 1024);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(2, 'Jagertest', SYSDATE(), '/home/starexec/benchmarks/jagertest2.smt2', 'This is another sample benchmark that cant be downloaded', 0, 1, 1024);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(1, 'Model 5 8', SYSDATE(), '/home/starexec/benchmarks/model_5_8.smt2', 'This is a sample benchmark that is downloadable', 1, 1, 1024);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(1, 'Model 6 10', SYSDATE(), '/home/starexec/benchmarks/model_6_10.smt2', 'This is a sample benchmark that is downloadable', 1, 1, 1024);	
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(1, 'Model 6 9', SYSDATE(), '/home/starexec/benchmarks/model_6_9.smt2', 'This is a sample benchmark that is downloadable', 1, 1, 1024);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(1, 'Problem 004', SYSDATE(), '/home/starexec/benchmarks/problem__004.smt2', 'This is a sample benchmark that is downloadable', 1, 1, 1024);
/*
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(6, 'ALG436-1', SYSDATE(), '/home/starexec/benchmarks/ALG436-1.p', 'This is a sample benchmark that is downloadable', 1, 1, 1024);
INSERT INTO benchmarks (user_id, name, uploaded, path, description, downloadable, bench_type, disk_size) VALUES
	(6, 'ALG438-1', SYSDATE(), '/home/starexec/benchmarks/ALG438-1.p', 'This is another sample benchmark that cant be downloaded', 0, 1, 1024);
*/
INSERT INTO bench_attributes VALUES (1, 'set-logic', 'LRA');
INSERT INTO bench_attributes VALUES (1, 'status', 'SAT');
INSERT INTO bench_attributes VALUES (1, 'starexec-dependencies', '1');
INSERT INTO bench_attributes VALUES (1, 'starexec-dependency-1', 'SAT/Jagertest');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependencies', '2');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependency-1', 'TPTP/TPTPA/ALG438-1');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependency-2', 'TPTP/TPTPB/ALG438-1');

INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size) VALUES
	(1, 'Z3', SYSDATE(), '/home/starexec/solvers/z3', 'This is a downloadable solver that exists on the cluster', 1, 10240);
/*
INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size) VALUES
	(5, 'Vampire', SYSDATE(), '/home/starexec/solvers/vampire', 'This is a non-downloadable solver that exists on the cluster', 0, 10240);
*/
INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "default", "This is a default configuration for Z3");
INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "alternate_config", "This is a secondary configuration for Z3");
/*
INSERT INTO configurations(solver_id, name, description) VALUES
	(2, "default", "This is a default configuration for Vampire");
*/

INSERT INTO jobs (user_id, name, description, primary_space) VALUES
	(1, 'Sweet Job1','This is an example job description',1);
INSERT INTO jobs (user_id, name, description, primary_space) VALUES
	(1, 'Sweet Job2','This is an example job description',1);
INSERT INTO jobs (user_id, name, description, primary_space) VALUES
	(1, 'Sweet Job3','This is an example job description',1);
INSERT INTO jobs (user_id, name, description,primary_space) VALUES
	(1, 'Sweet Job4','This is an example job description',1);
INSERT INTO jobs (user_id, name, description,primary_space) VALUES
	(1, 'Sweet Job5','This is an example job description',1);
INSERT INTO jobs (user_id, name, description,primary_space) VALUES
	(2, 'SMT Job', 'This is another example job description',1);
/*
INSERT INTO jobs (user_id, name, description,primary_space) VALUES
	(6, 'TPTP Job 1', 'This is an example job for the TPTP community',1);
INSERT INTO jobs (user_id, name, description,primary_space) VALUES
	(6, 'TPTP Job 2', 'This is another example job for the TPTP community',1);
*/
INSERT INTO job_assoc VALUES (3, 1);
INSERT INTO job_assoc VALUES (3, 3);
INSERT INTO job_assoc VALUES (3, 2);
INSERT INTO job_assoc VALUES (3, 4);
INSERT INTO job_assoc VALUES (3, 5);
INSERT INTO job_assoc VALUES (3, 6);


/*
INSERT INTO job_attributes (pair_id, attr_key, attr_value) VALUES (1, 'starexec-result', 'sat');
INSERT INTO job_pairs(job_id, config_id, bench_id, node_id, start_time, end_time, status_code)
	VALUES (1, 3, 1, 3, SYSDATE(), SYSDATE(), 7);
INSERT INTO job_pairs(job_id, config_id, bench_id, node_id, start_time, end_time, status_code)
	VALUES (2, 2, 1, 2, SYSDATE(), SYSDATE(), 12);
INSERT INTO job_pairs(job_id, config_id, bench_id, node_id, start_time, end_time, status_code)
	VALUES (3, 3, 4, 4, SYSDATE(), SYSDATE(), 7);
INSERT INTO job_pairs(job_id, config_id, bench_id, node_id, start_time, end_time, status_code)
	VALUES (4, 3, 4, 3, SYSDATE(), SYSDATE(), 7);
*/


	
-- INSERT INTO set_assoc VALUES (1, 2);
INSERT INTO set_assoc VALUES (1, 3);
INSERT INTO set_assoc VALUES (1, 6);
INSERT INTO set_assoc VALUES (3, 4);
INSERT INTO set_assoc VALUES (4, 5);

-- INSERT INTO closure VALUES (2, 2);
INSERT INTO closure VALUES (3, 3);
INSERT INTO closure VALUES (4, 4);
INSERT INTO closure VALUES (5, 5);
INSERT INTO closure VALUES (6, 6);

-- INSERT INTO closure VALUES (1, 2);
INSERT INTO closure VALUES (1, 3);
INSERT INTO closure VALUES (1, 4);
INSERT INTO closure VALUES (1, 5);
INSERT INTO closure VALUES (1, 6);
INSERT INTO closure VALUES (3, 4);
INSERT INTO closure VALUES (3, 5);
INSERT INTO closure VALUES (4, 5);

/*
INSERT INTO user_assoc VALUES (1, 3, 6);
INSERT INTO user_assoc VALUES (1, 5, 6);
INSERT INTO user_assoc VALUES (2, 2, 8);
INSERT INTO user_assoc VALUES (3, 2, 9);
INSERT INTO user_assoc VALUES (4, 3, 10);
INSERT INTO user_assoc VALUES (5, 6, 11);
INSERT INTO user_assoc VALUES (1, 6, 6);
INSERT INTO user_assoc VALUES (6, 3, 12);
*/
/*
INSERT INTO bench_assoc VALUES (2, 1);
INSERT INTO bench_assoc VALUES (2, 2);
INSERT INTO bench_assoc VALUES (2, 3);
INSERT INTO bench_assoc VALUES (2, 4);
INSERT INTO bench_assoc VALUES (2, 5);
INSERT INTO bench_assoc VALUES (2, 6);
INSERT INTO bench_assoc VALUES (3, 1);
INSERT INTO bench_assoc VALUES (3, 2);
INSERT INTO bench_assoc VALUES (3, 3);
INSERT INTO bench_assoc VALUES (3, 4);
INSERT INTO bench_assoc VALUES (3, 5);
INSERT INTO bench_assoc VALUES (3, 6);
INSERT INTO bench_assoc VALUES (6, 7);
INSERT INTO bench_assoc VALUES (6, 8);
*/


INSERT INTO solver_assoc VALUES (2, 1);
INSERT INTO solver_assoc VALUES (3, 1);
-- INSERT INTO solver_assoc VALUES (3, 2);
-- INSERT INTO solver_assoc VALUES (6, 2);

/*
INSERT INTO job_assoc VALUES (3, 2);
INSERT INTO job_assoc VALUES (2, 1);
INSERT INTO job_assoc VALUES (2, 2);
INSERT INTO job_assoc VALUES (3, 1);
INSERT INTO job_assoc VALUES (3, 2);
INSERT INTO job_assoc VALUES (5, 3);
INSERT INTO job_assoc VALUES (5, 4);
*/

/*
INSERT INTO website (user_id, name, url) VALUES (1, 'Personal', 'http://www.tylernjensen.com');
INSERT INTO website (user_id, name, url) VALUES (1, 'University', 'http://www.cs.uiowa.edu');
INSERT INTO website (solver_id, name, url) VALUES (1, 'Documentation', 'http://www.cs.uiowa.edu');
INSERT INTO website (user_id, url, name) VALUES (5, 'http://www.google.com/', 'google');
INSERT INTO website (user_id, url, name) VALUES (5, 'http://www.bing.com/', 'bing');
INSERT INTO website (user_id, url, name) VALUES (5, 'http://www.uiowa.edu/', 'uiowa');
*/

INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (1, 2, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (2, 3, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (4, 1, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (1, 5, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (3, 4, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (5, 6, "C://Whatever");
INSERT INTO bench_dependency (primary_bench_id, secondary_bench_id, include_path) VALUES (3, 2, "C://Whatever");

INSERT INTO queues(name, status) VALUES ("all.q", "ACTIVE");
INSERT INTO queues(name, status) VALUES ("queue1.q", "ACTIVE");

INSERT INTO queue_assoc (queue_id, node_id) VALUES (1, 1);
INSERT INTO queue_assoc (queue_id, node_id) VALUES (1, 2);
INSERT INTO queue_assoc (queue_id, node_id) VALUES (1, 3);
INSERT INTO queue_assoc (queue_id, node_id) VALUES (1, 4);




