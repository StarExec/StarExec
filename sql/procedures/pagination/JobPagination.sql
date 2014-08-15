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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
				ORDER BY totalPairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
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
				ORDER BY totalPairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn=4 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
						completed,
						description, 
						deleted,
						user_id,
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
	
DROP PROCEDURE IF EXISTS GetNextPageOfUserJobs;
CREATE PROCEDURE GetNextPageOfUSerJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT)
	BEGIN
		IF _colSortedOn = 0 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
						
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY name ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false
				ORDER BY name DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 1 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
						
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY status ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false 
				ORDER BY status DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 2 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY completePairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false
				ORDER BY completePairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn = 3 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY pendingPairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false 
				ORDER BY pendingPairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSEIF _colSortedOn=4 THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY errorPairs ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false
				ORDER BY errorPairs DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id=_userId AND deleted=false
											
				-- Order results depending on what column is being sorted on
				ORDER BY created ASC
						 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
	
				WHERE 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
				AND 	jobs.user_id=_userId AND deleted=false
				ORDER BY created DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Wyatt Kaiser
DROP PROCEDURE IF EXISTS GetNextPageOfAllJobs;
CREATE PROCEDURE GetNextPageOfAllJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Jobs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT DISTINCT
						jobs.id, 
						jobs.name, 
						jobs.user_id, 
						jobs.created, 
						jobs.completed,
						jobs.description, 
						jobs.deleted,
						jobs.primary_space,
						GetJobStatus(jobs.id)		AS status,
						GetTotalPairs(jobs.id) 		AS totalPairs,
						GetCompletePairs(jobs.id) 	AS completePairs,
						GetPendingPairs(jobs.id) 	AS pendingPairs,
						GetErrorPairs(jobs.id) 		AS errorPairs
				
				FROM	jobs
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	DISTINCT
						jobs.id, 
						jobs.name, 
						jobs.user_id, 
						jobs.created, 
						jobs.completed,
						jobs.description, 
						jobs.deleted,
						jobs.primary_space,
						GetJobStatus(jobs.id)		AS status,
						GetTotalPairs(jobs.id) 		AS totalPairs,
						GetCompletePairs(jobs.id) 	AS completePairs,
						GetPendingPairs(jobs.id) 	AS pendingPairs,
						GetErrorPairs(jobs.id) 		AS errorPairs
				
				FROM	jobs			
				
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	DISTINCT
						jobs.id, 
						jobs.name, 
						jobs.user_id, 
						jobs.created,
						jobs.completed,
						jobs.description, 
						jobs.deleted,
						jobs.primary_space,
						GetJobStatus(jobs.id)		AS status,
						GetTotalPairs(jobs.id) 		AS totalPairs,
						GetCompletePairs(jobs.id) 	AS completePairs,
						GetPendingPairs(jobs.id) 	AS pendingPairs,
						GetErrorPairs(jobs.id) 		AS errorPairs
				
				FROM	jobs
				WHERE
				
				-- Exclude Jobs whose name and status don't contain the query string
						(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
										
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT DISTINCT
						jobs.id, 
						jobs.name, 
						jobs.user_id, 
						jobs.created, 
						jobs.completed,
						jobs.description, 
						jobs.deleted,
						jobs.primary_space,
						GetJobStatus(jobs.id)		AS status,
						GetTotalPairs(jobs.id) 		AS totalPairs,
						GetCompletePairs(jobs.id) 	AS completePairs,
						GetPendingPairs(jobs.id) 	AS pendingPairs,
						GetErrorPairs(jobs.id) 		AS errorPairs
				FROM	jobs

				WHERE
					 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //

	
-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers
DROP PROCEDURE IF EXISTS GetNextPageOfJobs;
CREATE PROCEDURE GetNextPageOfJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _spaceId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Jobs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				JOIN job_assoc ON job_assoc.job_id = jobs.id
				WHERE job_assoc.space_id=_spaceId
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				JOIN job_assoc ON job_assoc.job_id = jobs.id
				WHERE job_assoc.space_id=_spaceId
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs
				JOIN job_assoc ON job_assoc.job_id = jobs.id
				WHERE job_assoc.space_id=_spaceId AND
				-- Exclude Jobs whose name and status don't contain the query string
				(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))

										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
					 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs
				JOIN job_assoc ON job_assoc.job_id = jobs.id
				WHERE job_assoc.space_id=_spaceId AND
				(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //


	
	
	-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Gets jobs across all spaces for one user.  
-- Author: Ben and Ruoyu
DROP PROCEDURE IF EXISTS GetNextPageOfUserJobs;
CREATE PROCEDURE GetNextPageOfUserJobs(IN _startingRecord INT, IN _recordsPerPage INT, IN _colSortedOn INT, IN _sortASC BOOLEAN, IN _userId INT, IN _query TEXT)
	BEGIN
		-- If _query is empty, get next page of Jobs without filtering for _query
		IF (_query = '' OR _query = NULL) THEN
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false
				
				
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC
			 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
			
		-- Otherwise, ensure the target Jobs contain _query
		ELSE
			IF _sortASC = TRUE THEN
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				
				FROM	jobs 
				
				WHERE user_id = _userId
				
				AND deleted=false
				
				-- Exclude Jobs whose name and status don't contain the query string
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))
										
										
				-- Order results depending on what column is being sorted on
				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) ASC	 
				-- Shrink the results to only those required for the next page of Jobs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
				SELECT 	id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						primary_space,
						GetJobStatus(id)		AS status,
						GetTotalPairs(id) 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs
				FROM	jobs 
				WHERE user_id = _userId
				
				AND deleted=false
				
				AND 	(name				LIKE	CONCAT('%', _query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', _query, '%'))

				ORDER BY 
					 (CASE _colSortedOn
					 	WHEN 0 THEN name
					 	WHEN 1 THEN status
					 	WHEN 2 THEN completePairs
					 	WHEN 3 THEN pendingPairs
					 	WHEN 4 THEN errorPairs
						ELSE created
					 END) DESC
				
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
		END IF;
	END //
	
DELIMITER ; -- This should always go at the end of the file