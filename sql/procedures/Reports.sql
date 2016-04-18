-- Description: This file contains all weekly-report-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement



-- Set the value of an event's occurrences not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS SetEventOccurrencesNotRelatedToQueue;
CREATE PROCEDURE SetEventOccurrencesNotRelatedToQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT)
	BEGIN
		UPDATE report_data
		SET occurrences = _eventOccurrences
		WHERE event_name = _eventName AND queue_name IS NULL;
	END //

-- Set the value of an event's occurrences not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS SetEventOccurrencesForQueue;
CREATE PROCEDURE SetEventOccurrencesForQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT, IN _queueName VARCHAR(64))
	BEGIN
		-- check if the event already exists for this queue and set it if it does 
		IF EXISTS (SELECT 1 FROM report_data WHERE queue_name=_queueName) AND EXISTS (SELECT 1 FROM report_data WHERE event_name=_eventName) THEN
			UPDATE report_data
			SET occurrences = _eventOccurrences
			WHERE event_name = _eventName AND queue_name = _queueName;
		-- otherwise create the event with the given number of occurrences
		ELSE 
			INSERT INTO report_data (event_name, queue_name, occurrences)
			VALUES (_eventName, _queueName, _eventOccurrences);
		END IF;
	END //


-- Adds to the value of an event's occurrences not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddToEventOccurrencesNotRelatedToQueue; 
CREATE PROCEDURE AddToEventOccurrencesNotRelatedToQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT)
	BEGIN
		UPDATE report_data
		SET occurrences = occurrences + _eventOccurrences
		WHERE event_name = _eventName AND queue_name IS NULL;
	END //

-- Add to the value of an event's occurrences for a specific queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddToEventOccurrencesForQueue;
CREATE PROCEDURE AddToEventOccurrencesForQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT, IN _queueName VARCHAR(64))
	BEGIN
			
		INSERT IGNORE INTO report_data (event_name, queue_name, occurrences) VALUES (_eventName, _queueName, 0);

		UPDATE report_data
		SET occurrences = occurrences + _eventOccurrences
		WHERE event_name = _eventName AND queue_name = _queueName;
	END //

-- Add to the value of an event's occurrences for a specific queue related to a specific job pair.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddToEventOccurrencesForJobPairsQueue;
CREATE PROCEDURE AddToEventOccurrencesForJobPairsQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT, IN _pairId INT)
	BEGIN
		 SET @queueId := (SELECT queue_id
			 			  FROM job_pairs
			 			  INNER JOIN jobs
			 			  ON job_pairs.job_id=jobs.id
			 			  WHERE job_pairs.id=_pairId);

		IF @queueId IS NOT NULL THEN
			SET @queueName := (SELECT name FROM queues WHERE id=@queueId);

			INSERT IGNORE INTO report_data (event_name, occurrences, queue_name) VALUES (_eventName, 0, @queueName);


			CALL AddToEventOccurrencesForQueue(_eventName, _eventOccurrences, @queueName);	
		END IF;
	END //

-- Gets all event names and occurrences for all events not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetAllEventsAndOccurrencesNotRelatedToQueues;
CREATE PROCEDURE GetAllEventsAndOccurrencesNotRelatedToQueues()
	BEGIN
		SELECT event_name, occurrences 
		FROM report_data
		WHERE queue_name IS NULL;
	END // 

-- Gets all event names and occurrences for every queue
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetAllEventsAndOccurrencesForAllQueues;
CREATE PROCEDURE GetAllEventsAndOccurrencesForAllQueues()
	BEGIN
		SELECT event_name, occurrences, queue_name FROM report_data WHERE queue_name IS NOT NULL;
	END //

-- Gets the number of occurrences for an event not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetEventOccurrencesNotRelatedToQueues;
CREATE PROCEDURE GetEventOccurrencesNotRelatedToQueues(IN _eventName VARCHAR(64))
	BEGIN
		SELECT occurrences 
		FROM report_data
		WHERE event_name = _eventName AND queue_name IS NULL;
	END //


-- Gets the number of an event's occurrences for a specific queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetEventOccurrencesForQueue;
CREATE PROCEDURE GetEventOccurrencesForQueue(IN _eventName VARCHAR(64), _queueName VARCHAR(64))
	BEGIN
		SELECT occurrences
		FROM report_data
		WHERE event_name = _eventName AND queue_name=_queueName;
	END //


-- Resets all report data by setting all occurrences to 0 and deleting queue related rows
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS ResetReports;
CREATE PROCEDURE ResetReports()
	BEGIN
		UPDATE report_data
		SET occurrences = 0
		WHERE queue_name IS NULL;
		DELETE FROM report_data
		WHERE queue_name IS NOT NULL;
	END //


-- Gets the number of unique user logins in the logins table.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetNumberOfUniqueLogins;
CREATE PROCEDURE GetNumberOfUniqueLogins()
	BEGIN
		SELECT COUNT(*) FROM (SELECT DISTINCT user_id FROM logins) AS T;
	END //

-- Delete all information in the logins table.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS ResetLogins;
CREATE PROCEDURE ResetLogins()
	BEGIN
		DELETE FROM logins;
	END //

DELIMITER ; -- this should always be at the end of the file
