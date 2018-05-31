-- Populate the StarExec DB with a minimal set of data to get started.

-- the password is Starexec4ever
INSERT INTO users (email, first_name, last_name, institution, created, password, disk_quota) VALUES
	('admin@uiowa.edu', 'Admin', 'User', 'The University of Iowa', SYSDATE(), 'b109f3bbbc244eb82441917ed06d618b9008dd09b3befd1b5e07394c706a8bb980b1d7785e5976ec049b46df5f1326af5a2ea6d103fd07c95385ffab0cacbc86', 107374182400),
	('public', 'Public', 'User', 'None', SYSDATE(), 'd32997e9747b65a3ecf65b82533a4c843c4e16dd30cf371e8c81ab60a341de00051da422d41ff29c55695f233a1e06fac8b79aeb0a4d91ae5d3d18c8e09b8c73', 52428800);

INSERT INTO user_roles VALUES
	('admin@uiowa.edu', 'admin'),
	('public', 'user');

-- Starts at 2 (the root default permission is defined in the schema)
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
	(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0),
	(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0);

-- Starts at 2 (the root space is defined in the schema)
INSERT INTO spaces(name, created, description, locked, default_permission) VALUES
	('Test', SYSDATE(), 'The Test community', 0, 3);

INSERT INTO set_assoc VALUES
	(1, 2);

INSERT INTO closure VALUES
	(1, 2),
	(2, 2);

INSERT INTO user_assoc VALUES
	(1, 1, 2);

INSERT INTO queues(name, status, global_access) VALUES
	("all.q", "ACTIVE", true);

INSERT INTO system_flags (paused, test_queue, major_version, minor_version) VALUES
	(false, 1, 1, 1);
