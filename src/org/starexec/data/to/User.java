package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;


import com.google.gson.annotations.Expose;

/**
 * Represents a user in the database
 * 
 * @author Tyler Jensen
 */
public class User extends Identifiable {
	@Expose	private String email;
	@Expose	private String firstName;
	@Expose	private String lastName;
	@Expose	private String institution;	
	private String role;
	private Timestamp createDate;	
	private transient String password;		
	private List<Website> websites;
	private long diskQuota;
	private long diskUsage;
	private boolean subscribedToReports;
	private boolean subscribedToErrorLogs;
	private int pairQuota;
	
	
	/**
	 * @return the user's disk quota (in bytes)
	 */
	public long getDiskQuota(){
		return diskQuota;
	}
	
	/**
	 * @param diskQuota the disk quota to set
	 */
	public void setDiskQuota(long diskQuota){
		this.diskQuota = diskQuota;
	}
	
	/**
	 * @return the user's registered email address
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email address to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the user's first name
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the first name to set for the user
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the user's last name
	 */
	public String getLastName() {
		return lastName;
	}
	
	

	/**
	 * @param lastName the last name to set for the user
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the institution the user belongs to
	 */
	public String getInstitution() {
		return institution;
	}

	/**
	 * @param institution the institution to set for the user
	 */
	public void setInstitution(String institution) {
		this.institution = institution;
	}
	
	/**
	 * @return the role of the user
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set for the user
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * @return the date the user joined the system
	 */
	public Timestamp getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the date to set when the user joined the system
	 */
	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the user's password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password to set for the user
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the user's first and last name with a space in between
	 */
	public String getFullName() {
		return firstName + " " + lastName;
	}
	
	/**
	 * @return A list of websites associated with the user
	 */
	public List<Website> getWebsites() {
		if(websites == null) {
			this.websites = new LinkedList<Website>();
		}
		
		return websites;
	}

	/**
	 * @param website A website to associate with the user
	 */
	public void addWebsite(Website website) {
		if(this.websites == null) {
			websites = new LinkedList<Website>();
		}
		
		this.websites.add(website);
	}

	/**
	 * @return true if the user is subscribed to reports. False otherwise.
	 * @author Albert Giegerich
	 */
	public boolean isSubscribedToReports() {
		return subscribedToReports;
	}

	/**
	 * Subscribe/unsubscribe user to reports.
	 * @param subscribe Whether or not to subscribe the user.
	 * @author Albert Giegerich
	 */
	public void setSubscribedToReports(boolean subscribe) {
		subscribedToReports = subscribe;
	}

	public boolean isSubscribedToErrorLogs() {
		return subscribedToErrorLogs;
	}

	public void setSubscribedToErrorLogs(boolean subscribedToErrorLogs) {
		this.subscribedToErrorLogs = subscribedToErrorLogs;
	}
	
	@Override
	public String toString() {
		return this.getFullName();
	}

	/**
	 * @return the pairQuota
	 */
	public int getPairQuota() {
		return pairQuota;
	}

	/**
	 * @param pairQuota the pairQuota to set
	 */
	public void setPairQuota(int pairQuota) {
		this.pairQuota = pairQuota;
	}

	/**
	 * @return the diskUsage
	 */
	public long getDiskUsage() {
		return diskUsage;
	}

	/**
	 * @param diskUsage the diskUsage to set
	 */
	public void setDiskUsage(long diskUsage) {
		this.diskUsage = diskUsage;
	}
}
