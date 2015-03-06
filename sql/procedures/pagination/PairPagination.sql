-- This file contains a highly repetitive procedure used to sort datatables on different columns
-- our old method was to use a order by (CASE) statement to order on different columns, but doing this
-- prevents SQL from using indexes for sorting for some reason, and as such is very slow. 

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Gets the next page of job pairs enqueued in a given queue for a datatable
DROP PROCEDURE IF EXISTS GetNextPageOfEnqueuedJobPairs;
CREATE PROCEDURE GetNextPageOfEnqueuedJobPairs(IN _startingRecord INT, IN _recordsPerPage INT, IN _sortASC BOOLEAN, IN _id INT)
	BEGIN
		
			IF (_sortASC = TRUE) THEN
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND job_pairs.status_code = 2)
					ORDER BY job_pairs.sge_id ASC
				-- Shrink the results to only those required for the next page of JobPairs
				LIMIT _startingRecord, _recordsPerPage;
			ELSE
					SELECT *
					FROM job_pairs
					-- Where the job_pair is running on the input Queue
						INNER JOIN jobs AS enqueued ON job_pairs.job_id = enqueued.id
					WHERE (enqueued.queue_id = _id AND job_pairs.status_code = 2)
					ORDER BY job_pairs.sge_id DESC
				LIMIT _startingRecord, _recordsPerPage;
			END IF;
	END //
DELIMITER ; -- this should always be at the end of the file