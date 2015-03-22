-- This file contains a highly repetitive procedure used to sort datatables on different columns
-- our old method was to use a order by (CASE) statement to order on different columns, but doing this
-- prevents SQL from using indexes for sorting for some reason, and as such is very slow. 

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Eric Burns
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceHierarchy;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceHierarchy(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _jobSpaceId INT, IN _sortColumn INT, IN _stageNumber INT, IN _configId INT, IN _type VARCHAR(16))
	BEGIN
		IF (_sortColumn = 0) THEN
			IF (_sortASC = TRUE) THEN
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

				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
				
				-- this large block handles filtering the pairs according to their status code

				AND
				
				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				-- Order results depending on what column is being sorted on
				ORDER BY bench_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)

				AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')

				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY bench_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=1) THEN
			IF (_sortASC = TRUE) THEN
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)

					AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY jobpair_stage_data.solver_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)

					AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY jobpair_stage_data.solver_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=2) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						
						job_pairs.bench_id,
						bench_name,
						bench_attributes.attr_value AS expected,
						job_attributes.attr_value AS result,
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
						
				FROM	job_pairs	
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY jobpair_stage_data.config_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						
						job_pairs.bench_id,
						bench_name,
						bench_attributes.attr_value AS expected,
						job_attributes.attr_value AS result,
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
				FROM	job_pairs	
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY jobpair_stage_data.config_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=3) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						
						job_pairs.bench_id,
						bench_name,
						bench_attributes.attr_value AS expected,
						job_attributes.attr_value AS result,
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
						
				FROM	job_pairs
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY jobpair_stage_data.status_code ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)

					AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY jobpair_stage_data.status_code DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=4) THEN
			IF (_sortASC = TRUE) THEN
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR     job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY wallclock ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						bench_attributes.attr_value AS expected,
						job_pairs.bench_id,
						bench_name,
						
						job_attributes.attr_value AS result,
						
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
				FROM	job_pairs	
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY wallclock DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=5) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						
						job_pairs.bench_id,
						bench_name,
						bench_attributes.attr_value AS expected,
						job_attributes.attr_value AS result,
						
						jobpair_stage_data.wallclock AS wallclock,
						jobpair_stage_data.cpu AS cpu
						
				FROM	job_pairs	
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))

				-- Order results depending on what column is being sorted on
				ORDER BY result ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 				
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value			LIKE 	CONCAT('%', _query, '%'))

				ORDER BY result DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			ELSEIF (_sortColumn=6) THEN
			IF (_sortASC = TRUE) THEN
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 	
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY job_pairs.id ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				 
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				ORDER BY job_pairs.id DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			ELSEIF (_sortColumn=7) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						jobpair_stage_data.config_id,
						jobpair_stage_data.config_name,
						jobpair_stage_data.status_code,
						jobpair_stage_data.solver_id,
						jobpair_stage_data.solver_name, jobpair_stage_data.stage_number,
						job_pairs.bench_id,
						bench_name,
						job_attributes.attr_value AS result,
						jobpair_stage_data.wallclock AS wallclock,
						bench_attributes.attr_value AS expected,
						jobpair_stage_data.cpu AS cpu
						
				FROM	job_pairs
				JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
				LEFT JOIN job_pair_completion ON job_pair_completion.pair_id=job_pairs.id
				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and wallclock
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value			LIKE 	CONCAT('%', _query, '%'))
				
				-- Doing a negative DESC is the same as ASC but with null values last
				ORDER BY -completion_id DESC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
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
				LEFT JOIN job_pair_completion ON job_pair_completion.pair_id=job_pairs.id

				LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
				JOIN job_space_closure ON descendant=job_pairs.job_space_id
				LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
				WHERE 	ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_stageNumber = 0 AND jobpair_stage_data.stage_number=job_pairs.primary_jobpair_data) OR jobpair_stage_data.stage_number=_stageNumber)
	AND

				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'))
				
				-- Doing a negative ASC is the same as DESC but with null values last

				ORDER BY -completion_id ASC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
	
-- Counts the total number of job pairs that satisfy GetNextPageOfJobPairsInJobSpaceHierarchy
DROP PROCEDURE IF EXISTS CountJobPairsInJobSpaceHierarchyByType;
CREATE PROCEDURE CountJobPairsInJobSpaceHierarchyByType(IN _jobSpaceId INT,IN _configId INT, IN _type VARCHAR(16), IN _query TEXT)

	BEGIN
		SELECT COUNT(*) as count FROM job_pairs

		JOIN jobpair_stage_data ON jobpair_stage_data.jobpair_id = job_pairs.id
		LEFT JOIN job_pair_completion ON job_pair_completion.pair_id=job_pairs.id

		LEFT JOIN job_attributes on (job_attributes.pair_id=job_pairs.id and job_attributes.stage_number=jobpair_stage_data.stage_number AND job_attributes.attr_key="starexec-result")
		JOIN job_space_closure ON descendant=job_pairs.job_space_id
		LEFT JOIN bench_attributes ON (job_pairs.bench_id=bench_attributes.bench_id AND bench_attributes.attr_key = "starexec-expected-result")
		
		WHERE ancestor=_jobSpaceId AND jobpair_stage_data.config_id=_configId AND 
				((_type = "all") OR
				(_type="resource" AND job_pairs.status_code>=14 AND job_pairs.status_code<=17) OR
				(_type = "incomplete" AND job_pairs.status_code!=7 AND !(job_pairs.status_code>=14 AND job_pairs.status_code<=17)) OR
				(_type="failed" AND ((job_pairs.status_code>=8 AND job_pairs.status_code<=13) OR job_pairs.status_code=18)) OR
				(_type ="complete" AND (job_pairs.status_code=7 OR (job_pairs.status_code<=14 ANd job_pairs.status_code<=17))) OR
				(_type= "unknown" AND job_pairs.status_code=7 AND job_attributes.attr_value="starexec-unknown") OR
				(_type = "solved" AND job_pairs.status_code=7 AND (job_attributes.attr_value=bench_attributes.attr_value OR bench_attributes.attr_value is null)) OR
				(_type = "wrong" AND job_pairs.status_code=7 AND (bench_attributes.attr_value is not null) and (job_attributes.attr_value!=bench_attributes.attr_value)))
				
				AND
				
				(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.config_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.solver_name		LIKE	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		jobpair_stage_data.wallclock				LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%')
				OR      job_attributes.attr_value 			LIKE 	CONCAT('%', _query, '%'));
	END //
	
DELIMITER ; -- This should always go at the end of the file