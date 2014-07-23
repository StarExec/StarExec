package org.starexec.data.to;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Represents an request to join a community in the database
 * 
 * @author Todd Elvers & Tyler Jensen
 */
public class QueueRequest extends Identifiable {

	private int userId;
	private int spaceId;
	private String queueName;
	private int node_count;
	private Date start_date;
	private Date end_date;
	private String message;
	private Timestamp createDate;
	private int wallTimeout;
	private int cpuTimeout;
	
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
	 * @return the user_id of the user who created the request
	 */
	public int getSpaceId(){
		return this.spaceId;
	}
	
	/**
	 * @param userId the userId of the user who created this invite
	 */
	public void setSpaceId(int spaceId){
		this.spaceId = spaceId;
	}
	
	/**
	 * @return the user_id of the user who created the request
	 */
	public String getQueueName(){
		return this.queueName;
	}
	
	/**
	 * @param userId the userId of the user who created this invite
	 */
	public void setQueueName(String queueName){
		this.queueName = queueName;
	}

	/**
	 * @return the node_count of the request
	 */
	public int getNodeCount(){
		return this.node_count;
	}
	
	/**
	 * @param node_count the count of nodes the user requested
	 */
	public void setNodeCount(int nodeCount){
		this.node_count = nodeCount;
	}
	
	/**
	 * @return the start date of the request
	 */
	public Date getStartDate(){
		return this.start_date;
	}
	
	/**
	 * @param start_date the date to begin the reservation
	 */
	public void setStartDate(Date start_date){
		this.start_date = start_date;
	}
	
	/**
	 * @return the end date of the request
	 */
	public Date getEndDate(){
		return this.end_date;
	}
	
	/**
	 * @param node_count the date the end the reservation
	 */
	public void setEndDate(Date end_date){
		this.end_date = end_date;
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

	/**
	 * @param wallTimeout the wallTimeout to set
	 */
	public void setWallTimeout(int wallTimeout) {
		this.wallTimeout = wallTimeout;
	}

	/**
	 * @return the wallTimeout
	 */
	public int getWallTimeout() {
		return wallTimeout;
	}

	/**
	 * @param cpuTimeout the cpuTimeout to set
	 */
	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}

	/**
	 * @return the cpuTimeout
	 */
	public int getCpuTimeout() {
		return cpuTimeout;
	}

}