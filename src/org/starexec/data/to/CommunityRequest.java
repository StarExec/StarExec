package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Represents an request to join a community in the database
 * 
 * @author Todd Elvers & Tyler Jensen
 */
public class CommunityRequest {
	private int communityId;
	private String message;
	private String code;
	private Timestamp createDate;
	private int userId;
	
	/**
	 * @return the user_id of the user who created the request
	 */
	public int getUserId(){
		return this.userId;
	}
	
	/**
	 * @param userId the userId of the user who created this invite
	 */
	public void setUserId(int userId){
		this.userId = userId;
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
	public String getCode(){
		return this.code;
	}
	
	/**
	 * @param code the unique code to set to this request (used for hyperlinking)
	 */
	public void setCode(String code){
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
	public void setMessage(String message){
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

}