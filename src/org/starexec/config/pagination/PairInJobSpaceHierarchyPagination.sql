-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Eric Burns
-- vars
-- query The query to filter pairs by
-- stageNumber The stage number to get data for
-- jobSpaceId The root space of the job space hierarchy to get pairs for
-- configId The ID of the configuration used by the given stage of the pairs
-- pairType The "type" of the pairs, where type is defined by the columns of the solver stats table

				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						job_pairs.bench_id,
						bench_name,
						job_attributes.attr_value AS result,
						bench_attributes.attr_value AS expected,
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
						
				FROM	job_pairs
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")

				WHERE 	ancestor= :jobSpaceId AND jobpair_stage_data.config_id= :configId AND 
				(( :stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number= :stageNumber)
				
				-- this large block handles filtering the pairs according to their status code

				AND
				
				(( :pairType = "all") OR
				( :pairType="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				( :pairType = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				( :pairType="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				( :pairType ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				( :pairType= "unknown" AND job_pairs.status_code=7 AND (job_attributes.attr_value="starexec-unknown"OR bench_attributes.attr_value is null)) OR
				( :pairType = "solved" AND job_pairs.status_code=7 AND job_attributes.attr_value=bench_attributes.attr_value) OR
				( :pairType = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', :query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', :query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', :query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', :query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', :query, '%')
				OR		cpu				LIKE	CONCAT('%', :query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', :query, '%'))

	

