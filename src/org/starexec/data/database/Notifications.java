package org.starexec.data.database;

import org.starexec.data.to.JobStatus;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;

import java.sql.SQLException;

public class Notifications {
	private Notifications() {
	} // Class cannot be instantiated

	private static final StarLogger log = StarLogger.getLogger(Notifications.class);

	/**
	 * @param user ID of User
	 * @param job ID of Job
	 * @return True if user is subscribed to job, false otherwise
	 */
	public static boolean isUserSubscribedToJob(int user, int job) throws SQLException {
		return Common.query("{CALL UserSubscribedToJob(?,?)}", procedure -> {
			procedure.setInt(1, user);
			procedure.setInt(2, job);
		}, results -> {
			results.next();
			return results.getBoolean(1);
		});
	}

	/**
	 * Subscribe User u to Job j
	 *
	 * @param user ID of User
	 * @param job ID of Job
	 */
	public static void subscribeUserToJob(int user, int job) throws SQLException {
		Common.update("{CALL SubscribeUserToJob(?,?)}", procedure -> {
			procedure.setInt(1, user);
			procedure.setInt(2, job);
		});
	}

	/**
	 * Unsubscribe user from job
	 *
	 * @param user ID of User
	 * @param job ID of Job
	 */
	public static void unsubscribeUserToJob(int user, int job) throws SQLException {
		Common.update("{CALL UnsubscribeUserFromJob(?,?)}", procedure -> {
			procedure.setInt(1, user);
			procedure.setInt(2, job);
		});
	}

	/**
	 * Update the last seen status of a Job to status. After we have sent a notification, we need to record the current
	 * status of the Job so that we can be notified when it changes again.
	 *
	 * @param userId
	 * @param job
	 * @param status
	 */
	private static void updateNotificationJobStatus(int userId, int job, JobStatus status) throws SQLException {
		log.debug(
				"updateNotificationJobStatus",
				"user: " + userId + "    job: " + job + "   status " + status.toString()
		);
		Common.update("{CALL UpdateNotificationJobStatus(?,?,?)}", procedure -> {
			procedure.setInt(1, userId);
			procedure.setInt(2, job);
			procedure.setString(3, status.name());
		});
	}

	/**
	 * Send email notifications for Jobs with changed status. Query the DB for Jobs whose status has changed since last
	 * we checked. Send email notifications to users subscribed to those jobs, then record the new status so we will
	 * know if it changes again in the future.
	 */
	public static void sendEmailNotifications() {
		final String method = "sendEmailNotifications";
		try {
			Common.query("CALL NotifyUsersOfJobs();", procedure -> {}, results -> {
				User user = new User();
				JobStatus status;
				int job;
				int userId;
				while (results.next()) {
					userId = results.getInt("user");
					user.setId(userId);
					user.setEmail(results.getString("email"));
					user.setFirstName(results.getString("first_name"));
					user.setLastName(results.getString("last_name"));
					job = results.getInt("job");
					status = JobStatus.valueOf(results.getString("status"));
					Mail.notifyUserOfJobStatus(user, job, status);
					updateNotificationJobStatus(userId, job, status);
				}
				return null;
			});
		} catch (SQLException e) {
			log.error(method, e);
		}
	}
}
