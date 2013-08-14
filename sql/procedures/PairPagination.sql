-- This file contains a highly repetitive procedure used to sort datatables on different columns
-- our old method was to use a order by (CASE) statement to order on different columns, but doing this
-- prevents SQL from using indexes for sorting for some reason, and as such is very slow. 

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpace;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpace(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT, IN _sortColumn INT)
	BEGIN
		IF (_sortColumn = 0) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				-- Order results depending on what column is being sorted on
				ORDER BY bench_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')

				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY bench_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=1) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver._name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY solver_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY solver_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=2) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY config_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY config_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=3) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs


				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY status_code ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY status_code DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=4) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY wallclock ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY wallclock DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=5) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	


				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY result ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs
				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY result DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //

	
-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets jobs across all spaces for one user.  
-- Author: Ben and Ruoyu + Eric Burns
DROP PROCEDURE IF EXISTS GetNextPageOfUserJobs;
CREATE PROCEDURE GetNextPageOfUserJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT)
	BEGIN
		IF (_sortColumn = 0) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				-- Order results depending on what column is being sorted on
				ORDER BY bench_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	

				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')

				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY bench_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=1) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver._name,
						bench_id,
						bench_name,
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY solver_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY solver_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=2) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY config_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY config_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=3) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs


				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY status_code ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY status_code DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=4) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	

				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY wallclock ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY wallclock DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF (_sortColumn=5) THEN
			IF (_sortASC = TRUE) THEN
				SELECT 	id, 
						config_id,
						config_name,
						
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	


				WHERE 	user_id=_userId AND deleted=false
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY result ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						config_id,
						config_name,
						status_code,
						solver_id,
						solver_name,
						
						bench_id,
						bench_name,
						
						GetJobPairResult(id) AS result,
						cpu,
						wallclock
				FROM	job_pairs
				WHERE 	user_id=_userId AND deleted=false
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status_code 	LIKE 	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY result DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //

	

DELIMITER ; -- this should always be at the end of the file