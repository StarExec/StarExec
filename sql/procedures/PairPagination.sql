-- This file contains several highly repetitive procedures used to sort datatables on different columns
-- our old method was to use a order by (CASE) statement to order on different columns, but doing this
-- prevents SQL from using indexes for sorting for some reason, and as such is very slow. 

USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement


-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortBench;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortBench(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY bench_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY bench_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
	
	-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortSolver;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortSolver(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY solver_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY solver_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
	
	-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortConfig;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortConfig(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY config_name ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY config_name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
	
	-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortTime;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortTime(IN _startingRecord INT, IN _recordsPerPage INT,  IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY cpu ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY cpu DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortResult;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortResult(IN _startingRecord INT, IN _recordsPerPage INT,  IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY result ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY result DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
	
-- Gets the fewest necessary JobPairs in order to service a client's
-- request for the next page of JobPairs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers	
DROP PROCEDURE IF EXISTS GetNextPageOfJobPairsInJobSpaceSortStatus;
CREATE PROCEDURE GetNextPageOfJobPairsInJobSpaceSortStatus(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _query TEXT, IN _spaceId INT)
	BEGIN
			IF (_sortASC = TRUE) THEN
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
						
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				-- Exclude JobPairs whose benchmark name, configuration name, solver name, status and cpu
				-- don't include the query
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				
				-- Order results depending on what column is being sorted on
				ORDER BY status.status ASC
			 
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	job_pairs.id, 
						config.id,
						config.name,
						config.description,
						status.status,
						status.description,
						solver.id,
						solver.name,
						solver.description,
						bench.id,
						bench.name,
						bench.description,
						GetJobPairResult(job_pairs.id) AS result,
						cpu,
						wallclock
				FROM	job_pairs	JOIN	status_codes 	AS 	status 	ON	job_pairs.status_code = status.code
									JOIN	configurations	AS	config	ON	job_pairs.config_id = config.id 
									JOIN	benchmarks		AS	bench	ON	job_pairs.bench_id = bench.id
									JOIN	solvers			AS	solver	ON	config.solver_id = solver.id

				WHERE 	job_space_id=_spaceId
				
				AND		(bench_name 		LIKE 	CONCAT('%', _query, '%')
				OR		config_name		LIKE	CONCAT('%', _query, '%')
				OR		solver_name		LIKE	CONCAT('%', _query, '%')
				OR		status.status	LIKE	CONCAT('%', _query, '%')
				OR		cpu				LIKE	CONCAT('%', _query, '%'))
				ORDER BY status.status DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
		
	
DELIMITER ; -- this should always be at the end of the file