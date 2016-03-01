-- gets jobs pairs for the next page of a datatables table on the job space explorer
-- vars
-- stageNumber The stage number to get data for
-- query The query to filter pairs by
-- jobSpaceId The ID of the job space to get pairs in

				SELECT  job_pairs.id,
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						completion_id,
						bench_id,
						bench_name,
						job_attributes.attr_value AS result,
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu,
						anonymous_bench_names.anonymous_name AS anon_bench_name,
						anonymous_solver_names.anonymous_name AS anon_solver_name,
						anonymous_config_names.anonymous_name AS anon_config_name

						
						
				FROM	job_pairs	
				LEFT JOIN job_pair_completion ON job_pairs.id=job_pair_completion.pair_id
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id

				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")

				LEFT JOIN anonymous_primitive_names AS anonymous_solver_names ON 
					jobpair_stage_data.solver_id=anonymous_solver_names.primitive_id AND anonymous_solver_names.primitive_type="solver"
				LEFT JOIN anonymous_primitive_names AS anonymous_bench_names ON
					bench_id=anonymous_bench_names.primitive_id AND anonymous_bench_names.primitive_type="bench"
				LEFT JOIN anonymous_primitive_names AS anonymous_config_names ON
					jobpair_stage_data.config_id=anonymous_config_names.primitive_id AND anonymous_config_names.primitive_type="config"

				WHERE 	jobpair_stage_data.job_space_id= :jobSpaceId AND 
				(( :stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number= :stageNumber)
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', :query , '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', :query , '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', :query , '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', :query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', :query, '%')
				OR		cpu				LIKE	CONCAT('%', :query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', :query, '%'))

