-- Author: Tyler Jensen
-- Description: This inserts sample data into the starexec database --

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

INSERT INTO bench_attributes VALUES (1, 'set-logic', 'LRA');
INSERT INTO bench_attributes VALUES (1, 'status', 'SAT');
INSERT INTO bench_attributes VALUES (1, 'starexec-dependencies', '1');
INSERT INTO bench_attributes VALUES (1, 'starexec-dependency-1', 'SAT/Jagertest');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependencies', '2');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependency-1', 'TPTP/TPTPA/ALG438-1');
INSERT INTO bench_attributes VALUES (2, 'starexec-dependency-2', 'TPTP/TPTPB/ALG438-1');

INSERT INTO solvers (user_id, name, uploaded, path, description, downloadable, disk_size) VALUES
	(1, 'Z3', SYSDATE(), '/home/starexec/solvers/z3', 'This is a downloadable solver that exists on the cluster', 1, 10240);

INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "default", "This is a default configuration for Z3");
INSERT INTO configurations(solver_id, name, description) VALUES
	(1, "alternate_config", "This is a secondary configuration for Z3");

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

INSERT INTO job_assoc VALUES (3, 1);
INSERT INTO job_assoc VALUES (3, 3);
INSERT INTO job_assoc VALUES (3, 2);
INSERT INTO job_assoc VALUES (3, 4);
INSERT INTO job_assoc VALUES (3, 5);
INSERT INTO job_assoc VALUES (3, 6);

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

INSERT INTO solver_assoc VALUES (2, 1);
INSERT INTO solver_assoc VALUES (3, 1);

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
