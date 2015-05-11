-- gets the next page of running pairs to populate the datatable on the cluster status page
-- vars
-- id The ID of the node to get the running pairs on
	SELECT job_pairs.id,
		   job_pairs.path,
		   job_pairs.primary_jobpair_data,
		   job_pairs.job_id,
		   job_pairs.bench_id,
		   job_pairs.bench_name,
		   jobpair_stage_data.solver_id,
		   jobpair_stage_data.solver_name,
		   jobpair_stage_data.config_id,
		   jobpair_stage_data.config_name,
		   jobs.id,
		   jobs.name,
		   users.id,
		   users.first_name,
		   users.last_name
	FROM job_pairs
	JOIN jobs ON jobs.id = job_pairs.job_id
	JOIN users ON users.id = jobs.user_id
	JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id

	WHERE node_id = :id AND (job_pairs.status_code = 4 OR job_pairs.status_code = 3) AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data
