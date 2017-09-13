-- Description: This file contains stored procedures for sending notifications

DELIMITER //

-- Subscribe a User to status updates from a Job
-- Record the current status of Job so we can see when it changes
DROP PROCEDURE IF EXISTS SubscribeUserToJob;
CREATE PROCEDURE SubscribeUserToJob(IN _userId INT, IN _jobId INT)
	BEGIN
		INSERT
			INTO notifications_jobs_users (user_id, job_id, last_seen_status)
			VALUES (_userId, _jobId, GetJobStatusDetail(_jobId))
		;
	END //

-- Unsubscribe a User from status updates to a Job
DROP PROCEDURE IF EXISTS UnsubscribeUserFromJob;
CREATE PROCEDURE UnsubscribeUserFromJob(IN _userId INT, IN _jobId INT)
	BEGIN
		DELETE
			FROM notifications_jobs_users
			WHERE user_id=_userId
			  AND job_id=_jobId
		;
	END //

-- Find all Jobs that Users have subscribed to whose current status is different
--   from last recorded status. Does NOT modify any data. Status must be updated
--   if a notification is sent.
-- Returns a list of Job ID and User emails for sending notifications
DROP PROCEDURE IF EXISTS NotifyUsersOfJobs;
CREATE PROCEDURE NotifyUsersOfJobs()
	BEGIN
		SELECT job_id AS "job", user_id as "user", first_name, last_name, users.email AS "email", GetJobStatusDetail(job_id) AS "status"
			FROM notifications_jobs_users
			LEFT JOIN users ON users.id=user_id
			WHERE last_seen_status <> GetJobStatusDetail(job_id)
		;
	END //

-- Update the last_seen_status of a Job-User notification to the current status
--   of Job, then clean up the table deleting any notifications for Jobs with an
--   immutable status
DROP PROCEDURE IF EXISTS UpdateNotificationJobStatus;
CREATE PROCEDURE UpdateNotificationJobStatus(IN _userId INT, IN _jobId INT, IN _status CHAR(16))
	BEGIN
		UPDATE notifications_jobs_users
			SET last_seen_status=_status
			WHERE user_id=_userId
			  AND job_id=_jobId
		;
		DELETE
			FROM notifications_jobs_users
			WHERE last_seen_status="COMPLETE"
			   OR last_seen_status="DELETED"
		;
	END //

DROP PROCEDURE IF EXISTS UserSubscribedToJob;
CREATE PROCEDURE UserSubscribedToJob(IN _userId INT, IN _jobId INT)
	BEGIN
		SELECT _jobId IN (
			SELECT job_id
			FROM notifications_jobs_users
			WHERE user_id=_userId
		);
	END //

DELIMITER ; -- this should always be at the end of the file
