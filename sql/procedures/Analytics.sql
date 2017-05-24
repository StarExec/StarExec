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
		IN _date_recorded DATE,
		IN _count INT,
		IN _users INT
	)
	BEGIN
		INSERT INTO analytics_historical (event_id, date_recorded, count, users)
			VALUES (_event_id, _date_recorded, _count, _users)
		ON DUPLICATE KEY
			UPDATE
				count = count + _count,
				users = GREATEST(users, _users);
	END //

DROP PROCEDURE IF EXISTS GetAnalyticsForDateRange;
CREATE PROCEDURE GetAnalyticsForDateRange(
		IN _start DATE,
		IN _end DATE)
	BEGIN
		SELECT
			name as "event",
			SUM(count) as count,
			SUM(users) as users
		FROM analytics_historical
		LEFT JOIN analytics_events ON analytics_historical.event_id = analytics_events.event_id
		WHERE date_recorded >= _start AND date_recorded <= _end
		GROUP BY analytics_historical.event_id;
	END //

DELIMITER ;
