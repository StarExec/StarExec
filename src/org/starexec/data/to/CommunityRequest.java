package org.starexec.data.to;

import org.starexec.data.database.Communities;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.util.Mail;
import org.starexec.logger.StarLogger;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Represents an request to join a community in the database
 *
 * @author Todd Elvers & Tyler Jensen
 */
public class CommunityRequest {
	private static final StarLogger log = StarLogger.getLogger(CommunityRequest.class);

	private int communityId;
	private String message;
	private String code;
	private Timestamp createDate;
	private int userId;
	private User user;

	/**
	 * @return the user_id of the user who created the request
	 */
	public int getUserId() {
		return this.userId;
	}

	/**
	 * @param userId the userId of the user who created this invite
	 */
	public void setUserId(int userId) {
		this.userId = userId;
		user = null;
	}

	public User getUser() {
		if (user == null) {
			user = Users.get(userId);
		}
		return user;
	}

	/**
	 * @return the id of the community this request is for
	 */
	public int getCommunityId() {
		return this.communityId;
	}

	/**
	 * @param communityId the id of the community to set for this request
	 */
	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}

	/**
	 * @return the unique code for this request (used for hyperlinking)
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * @param code the unique code to set to this request (used for hyperlinking)
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the message the user attached to the request
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * @param message the message to set for this request
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the date the request was added to the system
	 */
	public Timestamp getCreateDate() {
		return this.createDate;
	}

	/**
	 * @param createDate the date to set when the request was added to the system
	 */
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public void decline() {
		final String communityName = Spaces.getName(getCommunityId());
		final boolean isRegistered = Users.isUnauthorized(userId);

		// Remove their entry from INVITES
		Requests.declineCommunityRequest(userId, getCommunityId());

		// Notify user they've been declined
		try {
			Mail.sendRequestResults(getUser(), communityName, false, !isRegistered);
		} catch (IOException e) {
			log.error("decline", "Could not email user " + getUser().getFullName(), e);
		}

		log.info("User [" + getUser().getFullName() + "]'s request to join the " +
		         communityName + " community was declined."
		);
	}

	public void approve() {
		// Add them to the community & remove the request from the database
		boolean successfullyApproved =
				Requests.approveCommunityRequest(getUserId(), getCommunityId());
		if (!successfullyApproved) {
			log.error("Did not successfully approve user community request for user with id=" +
			          getUserId() + " even though an admin or community leader approved them.");
			return;
		}

		final String communityName = Spaces.getName(getCommunityId());

		// Notify user they've been approved
		try {
			Mail.sendRequestResults(getUser(), communityName, successfullyApproved, false);
		} catch (IOException e) {
			log.error("approve", "Could not email user " + getUser().getFullName(), e);
		}

		// Create a personal subspace for the user in the space they were admitted to
		Communities.createPersonalSubspace(getCommunityId(), getUser());

		log.info("User [" + user.getFullName() +
		         "] has finished the approval process and now apart of the " +
		         communityName + " community."
		);
	}
}
