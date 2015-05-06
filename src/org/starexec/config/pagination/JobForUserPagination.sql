
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
				WHERE 	(name				LIKE	CONCAT('%', :query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', :query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND jobs.user_id= :userId AND deleted=false
				
										
			