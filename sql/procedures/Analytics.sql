-- Retrieve the `id` of a given Event
DROP PROCEDURE IF EXISTS GetEventId //
CREATE PROCEDURE GetEventId( IN _name CHAR(32) )
	BEGIN
		SELECT event_id FROM analytics_events WHERE name=_name;
	END //

-- If there is not yet a record of this event happening today
--   create a record and set its count to `1`
-- otherwise
--   increment the count of the existing record
DROP PROCEDURE IF EXISTS RecordEvent //
CREATE PROCEDURE RecordEvent(
		IN _event_id INT,
		IN _date_recorded DATE,
		IN _count INT
	)
	BEGIN
		INSERT INTO analytics_historical (event_id, date_recorded, count)
			VALUES (_event_id, _date_recorded, _count)
		ON DUPLICATE KEY
			UPDATE
				count = count + _count;
	END //

-- Record an instance of
--   a particular user triggering
--   a particular event on
--   a particular day
-- If we have already recorded this user/event/day, we can just ignore the
-- DUPLICATE KEY warning
DROP PROCEDURE IF EXISTS RecordEventUser //
CREATE PROCEDURE RecordEventUser(
		IN _event_id INT,
		IN _date_recorded DATE,
		IN _user_id INT
	)
	BEGIN
		INSERT IGNORE
			INTO analytics_users (event_id, date_recorded, user_id)
			VALUES (_event_id, _date_recorded, _user_id)
		;
	END //

DROP PROCEDURE IF EXISTS GetAnalyticsForDateRange //
CREATE PROCEDURE GetAnalyticsForDateRange(
		IN _start DATE,
		IN _end DATE)
	BEGIN
		SELECT
			name as "event",
			SUM(count) as "count",
			COUNT(distinct user_id) as "users"
		FROM analytics_historical
		LEFT JOIN analytics_users  ON analytics_historical.event_id = analytics_users.event_id
		LEFT JOIN analytics_events ON analytics_historical.event_id = analytics_events.event_id
		WHERE analytics_historical.date_recorded >= _start AND analytics_historical.date_recorded <= _end
		GROUP BY analytics_historical.event_id
		;
	END //
