package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Represents an invite in the database
 * 
 * @author Todd Elvers
 */
public class Invite {
	private Long communityId;
	private String message;
	private String code;
	private Timestamp createDate;
	private long userId;
	
	/**
	 * @return the user_id of the user who created this invite (returns same as getId())
	 */
	public long getUserId(){
		return userId;
	}
	
	/**
	 * @param userId the userId of the user who created this invite
	 */
	public void setUserId(long userId){
		this.userId = userId;
	}
	
	/**
	 * @return the id of the community this invite is for
	 */
	public long getCommunityId() {
		return communityId;
	}

	/**
	 * @param communityId the id of the community to set for this invite
	 */
	public void setCommunityId(long communityId) {
		this.communityId = communityId;
	}

	/**
	 * @return the unique code for this invite (used for hyperlinking)
	 */
	public String getCode(){
		return code;
	}
	
	/**
	 * @param code the unique code to set to this invite (used for hyperlinking)
	 */
	public void setCode(String code){
		this.code = code;
	}
	
	
	/**
	 * @return the message for this invite
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * @param message the message to set for this invite
	 */
	public void setMessage(String message){
		this.message = message;
	}

	/**
	 * @return the date the invite was added to the system
	 */
	public Timestamp getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the date to set when the invite was added to the system
	 */
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

}