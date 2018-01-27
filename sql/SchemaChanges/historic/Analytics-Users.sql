-- Description: Create table to keep track of how many Users trigger an Event

CREATE TABLE analytics_users (
	event_id INT NOT NULL,
	date_recorded DATE NOT NULL,
	user_id INT NOT NULL,
	PRIMARY KEY (event_id, date_recorded, user_id),
	CONSTRAINT analytics_users_to_event FOREIGN KEY (event_id) REFERENCES analytics_events(event_id),
	CONSTRAINT analytics_users_to_users FOREIGN KEY (user_id)  REFERENCES users(id)
);
