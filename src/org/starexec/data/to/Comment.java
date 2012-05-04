package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a comment in the database
 * 
 * @author Vivek Sardeshmukh
 */
public class Comment extends Identifiable {
	@Expose private long userId = -1;
	@Expose	private String firstName;
	@Expose	private String lastName;
	@Expose private String description = "none";
	@Expose private Timestamp uploadDate;	
	
	
	/**
	 * @return the user id of the user who uploaded this comment
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * @param userId the user id to set as the uploader
	 */
	public void setUserId(long userId) {
		this.userId = userId;
	}
	
	/**
	 * @return the user comment description 
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the comment to set 
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
	}	
	
	/**
	 * @return the date the comment was added to the system
	 */
	public Timestamp getUploadDate() {
		return uploadDate;
	}
	
	/**
	 * @param uploadDate the upload date to set for the comment
	 */
	public void setUploadDate(Timestamp uploadDate) {
		this.uploadDate = uploadDate;
	}
	
	/**
	 * @return the user's first and last name with a space in between
	 */
	public String getFullName() {
		return firstName + " " + lastName;
	}
	
	/**
	 * @param firstName of the user who uploaded this comment
	 */
	public void setFirstName(String fname) {	
		this.firstName = fname;
	}
	/**
	 * @param lastName of the user who uploaded this comment
	 */
	public void setLastName(String lname) {	
		this.lastName = lname;
	}
	
	@Override
	public String toString() {
		return this.getFullName();
	}
}