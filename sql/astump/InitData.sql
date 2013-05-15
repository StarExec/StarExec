-- Author: Aaron Stump
-- Description: Sample data for starexec_astump on StarDev --

USE starexec;



INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('astump@acm.org', 'Aaron', 'Stump', 'University of Iowa', SYSDATE(), 'bff6d6df6acabf9a56693d263a608487d7858ea54cd1f5f99b3022dd7c7febb3a7127dbe1a4eb235a7a853f759b0c0de998207143a70588399b84aca747932e9', 1073741824);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('aaron-stump@uiowa.edu', 'Aaron', 'Stump', 'University of Iowa', SYSDATE(), 'bff6d6df6acabf9a56693d263a608487d7858ea54cd1f5f99b3022dd7c7febb3a7127dbe1a4eb235a7a853f759b0c0de998207143a70588399b84aca747932e9', 1073741824);
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('public', 'Jane ', 'Doe', 'Public', SYSDATE(), 'd32997e9747b65a3ecf65b82533a4c843c4e16dd30cf371e8c81ab60a341de00051da422d41ff29c55695f233a1e06fac8b79aeb0a4d91ae5d3d18c8e09b8c73', 104857600);


INSERT INTO user_roles VALUES('astump@acm.org', 'user');
INSERT INTO user_roles VALUES('aaron-stump@uiowa.edu', 'user');
INSERT INTO user_roles VALUES('public', 'user');
	

-- Starts at 2 (the root default permission is defined in the schema)
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0);


-- Starts at 2 (the root space is defined in the schema)
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SMT', SYSDATE(), 'this is the SMT space, the child of the root space', 0, 3);	
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('TPTP', SYSDATE(), 'this is Geoffs example space', 0, 3);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('fakeSpace', SYSDATE(), 'this is the unviewable space for benchmarks and jobs from public', 0, 3);

INSERT INTO set_assoc VALUES (1, 2);
INSERT INTO set_assoc VALUES (1, 3);

INSERT INTO closure VALUES (1, 2);
INSERT INTO closure VALUES (1, 3);
INSERT INTO closure VALUES (2, 2);
INSERT INTO closure VALUES (3, 3);

INSERT INTO user_assoc VALUES (1,2,2,2);
INSERT INTO user_assoc VALUES (1,3,3,2);