-- Description: This file contains all weekly-report-related stored procedures for the starexec database
-- The procedures are stored by which table they're related to and roughly alphabetic order. Please try to keep this organized!

DELIMITER // -- Tell MySQL how we will denote the end of each prepared statement

-- Adds to the value of an event's occurrences not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddToEventOccurrencesNotRelatedToQueue; 
CREATE PROCEDURE AddToEventOccurrences(IN _eventName VARCHAR(64), IN _eventOccurrences INT)
	BEGIN
		UPDATE report_data
		SET occurrences = occurrences + _eventOccurrences
		WHERE event_name = _eventName AND queue_id IS NULL;
	END //

-- Add to the value of an event's occurrences for a specific queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS AddToEventOccurrencesForQueue;
CREATE PROCEDURE AddToEventOccurrencesForQueue(IN _eventName VARCHAR(64), IN _eventOccurrences INT, IN _queueName VARCHAR(64))
	BEGIN
		SET @queueId := (SELECT id FROM queues WHERE name=_queueName);
		UPDATE report_data
		SET occurrences = occurrences + _eventOccurrences
		WHERE event_name = _eventName AND queue_id = @queueId;
	END //

-- Gets all event names and occurrences for all events not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetAllEventsAndOccurrencesNotRelatedToQueues;
CREATE PROCEDURE GetAllEventsAndOccurrencesNotRelatedToQueues()
	BEGIN
		SELECT event_name, occurrences 
		FROM report_data
		WHERE queue_id IS NULL;
	END // 

-- Gets all event names and occurrences for every queue along with the associated queue_id.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetAllEventsAndOccurrencesForAllQueues;
CREATE PROCEDURE GetAllEventsAndOccurrencesForAllQueues()
	BEGIN
		SELECT report_data.event_name, report_data.occurrences, report_data.queue_id, queues.name
		FROM report_data
		INNER JOIN queues
		ON report_data.queue_id = queues.id; 
	END //

-- Gets the number of occurrences for an event not related to a queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetEventOccurrencesNotRelatedToQueues;
CREATE PROCEDURE GetEventOccurrences(IN _eventName VARCHAR(64))
	BEGIN
		SELECT occurrences 
		FROM report_data
		WHERE event_name = _eventName AND queue_id IS NULL;
	END //


-- Gets the number of an event's occurrences for a specific queue.
-- Author: Albert Giegerich
DROP PROCEDURE IF EXISTS GetEventOccurrencesForQueue;
CREATE PROCEDURE GetEventOccurrencesForQueue(IN _eventName VARCHAR(64), _queueName VARCHAR(64))
	BEGIN
		SET @queueId := (SELECT id FROM queues WHERE name=_queueName);
		SELECT occurrences
		FROM report_data
		WHERE event_name = _eventName AND queue_id = @queueId;
	END //

DELIMITER ; -- this should always be at the end of the file
