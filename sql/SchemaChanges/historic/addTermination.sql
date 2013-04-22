USE starexec;
--Give me permission to add communities
INSERT INTO permissions(add_solver, add_bench, add_user, add_space, add_job, remove_solver, remove_bench, remove_user, remove_space, remove_job, is_leader) VALUES
	(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0);

INSERT INTO user_assoc values (1,1,1, (select LAST_INSERT_ID()) );