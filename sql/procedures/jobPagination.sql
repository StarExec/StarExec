USE starexec;

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers + Eric Burns
DROP PROCEDURE IF EXISTS GetNextPageOfJobs;
CREATE PROCEDURE GetNextPageOfJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		IF _colSortedOn = 0 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY name ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 1 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY status ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY status DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 2 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY completePairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY completePairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 3 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY pendingPairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY pendingPairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn=4 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY errorPairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY errorPairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND assoc.space_id=_spaceId 
											
				-- Order results depending on what column is being sorted on
				ORDER BY created ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						description, 
						deleted,
						
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	assoc.space_id=_spaceId 
				ORDER BY created DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
DELIMITER ; -- This should always go at the end of the file