-- Users can subscribe to be notified when a Job status changes

USE starexec;

-- Here we record the Job, User, and the last seen status
-- A periodic task will check the Job current status against the status recorded
-- here, and alert the User if status has changed
CREATE TABLE notifications_jobs_users (
	job_id  INT NOT NULL,
	user_id INT NOT NULL,
	last_seen_status
		ENUM("RUNNING", "PROCESSING", "COMPLETE", "DELETED", "KILLED", "PAUSED", "GLOBAL_PAUSE")
		NOT NULL,
	PRIMARY KEY (job_id, user_id),
	CONSTRAINT notifications_jobs_users_to_jobs  FOREIGN KEY (job_id)   REFERENCES jobs(id)  ON DELETE CASCADE,
	CONSTRAINT notifications_jobs_users_to_users FOREIGN KEY (user_id)  REFERENCES users(id) ON DELETE CASCADE
);
