-- Benton McCune
-- Data to initialize StarExec

USE starexec;

-- initial user, password="password"
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('benton-mccune@uiowa.edu', 'Benton', 'McCune', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 52428800);
INSERT INTO user_roles VALUES('benton-mccune@uiowa.edu', 'user');

-- public user, password="public" (must configure starexec-config for public user to function properly - set "PUBLIC_USER_ID" value="2" and "PUBLIC_SPACE_ID" value="4" 

INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota)
	VALUES ('public', 'Public', 'User', 'The Great Unwashed', SYSDATE(), 'd32997e9747b65a3ecf65b82533a4c843c4e16dd30cf371e8c81ab60a341de00051da422d41ff29c55695f233a1e06fac8b79aeb0a4d91ae5d3d18c8e09b8c73', 52428800);
INSERT INTO user_roles VALUES('public', 'user');

-- leadership permission for initial user (starts at 2, root space permission in schema	
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
-- default space permissions	
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);	
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	
-- Starts at 2 (the root space is defined in the schema)
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('SMT', SYSDATE(), 'this is the SMT community space', 0, 3);	
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('TPTP', SYSDATE(), 'this is the TPTP community', 0, 4);
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('publicSpace', SYSDATE(), 'this is a space for public jobs', 1, 5);

-- giving spaces a tree structure
INSERT INTO set_assoc VALUES (1, 2);
INSERT INTO set_assoc VALUES (1, 3);	
INSERT INTO closure VALUES (2, 2);
INSERT INTO closure VALUES (3, 3);	
INSERT INTO closure VALUES (1, 2);
INSERT INTO closure VALUES (1, 3);

-- make initial user leader of both initial spaces
INSERT INTO user_assoc VALUES (1, 2, 2, 2);
INSERT INTO user_assoc VALUES (1, 3, 3, 2);