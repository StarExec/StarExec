-- Gets the fewest necessary Jobs in order to service a client's
-- request for the next page of Jobs in their DataTable object.
-- This services the DataTable object by supporting filtering by a query,
-- ordering results by a column, and sorting results in ASC or DESC order.
-- Author: Todd Elvers + Eric Burns

-- vars
-- query The query to filter jobs by
				SELECT 	id,
						name,
						user_id,
						queue_id,
						created,
						completed,
						description,
						deleted,
						user_id,
						disk_size,
						jobs.total_pairs 		AS totalPairs,
						GetCompletePairs(id) 	AS completePairs,
						GetPendingPairs(id) 	AS pendingPairs,
						GetErrorPairs(id) 		AS errorPairs

				FROM	jobs
				INNER JOIN job_assoc AS assoc ON assoc.job_id=jobs.id

				-- Exclude Jobs whose name and status don't contain the query string
				WHERE 	(name				LIKE	CONCAT('%', :query, '%')
				OR		GetJobStatus(id)	LIKE	CONCAT('%', :query, '%'))

				-- Exclude Jobs that aren't in the specified space
				AND (GetJobStatus(id)="incomplete")
