DELIMITER //

-- Retrieve the `id` of a given Event
DROP PROCEDURE IF EXISTS GetEventId;
CREATE PROCEDURE GetEventId( IN _name CHAR(32) )
	BEGIN
		SELECT event_id FROM analytics_events WHERE name=_name;
	END //

-- If there is not yet a record of this event happening today
--   create a record and set its count to `1`
-- otherwise
--   increment the count of the existing record
DROP PROCEDURE IF EXISTS RecordEvent;
CREATE PROCEDURE RecordEvent(
		IN _event_id INT,
		IN _date_recorded DATE)
	BEGIN
		INSERT INTO analytics_historical (event_id, date_recorded, count)
			VALUES (_event_id, _date_recorded, 1)
		ON DUPLICATE KEY
			UPDATE count = count + 1;
	END //

DROP PROCEDURE IF EXISTS GetAnalyticsForDateRange;
CREATE PROCEDURE GetAnalyticsForDateRange(
		IN _start DATE,
		IN _end DATE)
	BEGIN
		SELECT
			name as "event",
			SUM(count) as count
		FROM analytics_historical
		LEFT JOIN analytics_events ON analytics_historical.event_id = analytics_events.event_id
		WHERE date_recorded >= _start AND date_recorded <= _end
		GROUP BY analytics_historical.event_id;
	END //

DELIMITER ;
