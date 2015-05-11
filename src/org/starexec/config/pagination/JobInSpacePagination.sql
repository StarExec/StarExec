-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.  
-- This services the DataTable object by supporting filtering by a query, 
-- ordering results by a column, and sorting results in ASC or DESC order.  
-- Author: Todd Elvers + Eric Burns

-- vars 
-- spaceId The ID of the space to get jobs in, or -1 for all jobs
-- query The query to filter jobs by
				SELECT 	jobs.id, 
						name, 
						user_id, 
						created, 
						completed,
						description, 
						deleted,
						user_id,
						COUNT(job_pairs.id) AS totalPairs,
						COUNT(CASE WHEN job_pairs.status_code=7 THEN 1 END) 	AS completePairs,	
						COUNT(CASE WHEN job_pairs.status_code BETWEEN 1 AND 6 THEN 1 END) AS pendingPairs,
						COUNT(CASE WHEN job_pairs.status_code BETWEEN 8 AND 17 OR job_pairs.status_code=0 THEN 1 END) AS errorPairs
				
				FROM	jobs
				JOIN job_pairs ON job_pairs.job_id = jobs.id
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id
	
					
				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', :query, '%')
				OR		GetJobStatus(jobs.id)	LIKE	CONCAT('%', :query, '%'))
											
				-- Exclude Jobs that aren't in the specified space
				AND (assoc.space_id=:spaceId)
				GROUP BY jobs.id
	
