-- gets the next page of enqueued pairs to populate the datatable on the cluster status page
-- vars
-- id The ID of the queue to get the running pairs on

SELECT 	   job_pairs.id,
		   job_pairs.path,
		   job_pairs.primary_jobpair_data,
		   job_pairs.job_id,
		   job_pairs.bench_id,
		   job_pairs.bench_name,
		   job_pairs.queuesub_time,
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
-- Where the job_pair is running on the input Queue
	JOIN jobs ON jobs.id = job_pairs.job_id
	JOIN users ON users.id = jobs.user_id
	JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
WHERE (jobs.queue_id = :id AND job_pairs.status_code = 2 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data)
