package org.starexec.data.database;

import java.sql.SQLException;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobStatus;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;

public class Notifications {
	private Notifications() {} // Class cannot be instantiated
	private static StarLogger log = StarLogger.getLogger(Notifications.class);

	/**
	 * @param user ID of User
	 * @param job ID of Job
	 * @return True if user is subscribed to job, false otherwise
	 */
	public static boolean isUserSubscribedToJob(int user, int job) throws SQLException {
		return Common.query(
			"{CALL UserSubscribedToJob(?,?)}",
			procedure -> {
				procedure.setInt(1, user);
				procedure.setInt(2, job);
			},
			results -> {
				results.next();
				return results.getBoolean(1);
			}
		);
	}

	/**
	 * Subscribe User u to Job j
	 *
	 * @param user ID of User
	 * @param job ID of Job
	 */
	public static void subscribeUserToJob(int user, int job) throws SQLException {
		Common.update(
			"{CALL SubscribeUserToJob(?,?)}",
			procedure -> {
				procedure.setInt(1, user);
				procedure.setInt(2, job);
			}
		);
	}

	/**
	 * Unsubscribe user from job
	 *
	 * @param user ID of User
	 * @param job ID of Job
	 */
	public static void unsubscribeUserToJob(int user, int job) throws SQLException {
		Common.update(
			"{CALL UnsubscribeUserFromJob(?,?)}",
			procedure -> {
				procedure.setInt(1, user);
				procedure.setInt(2, job);
			}
		);
	}

	private static void updateNotificationJobStatus(int userId, int job, JobStatus status) throws SQLException {
		log.warn("updateNotificationJobStatus", "user: " + userId + "    job: " + job + "   status " + status.toString());
		Common.update(
			"{CALL UpdateNotificationJobStatus(?,?,?)}",
			procedure -> {
				procedure.setInt(1, userId);
				procedure.setInt(2, job);
				procedure.setString(3, status.name());
			}
		);
	}

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
