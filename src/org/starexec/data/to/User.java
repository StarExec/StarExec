package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.starexec.constants.R;

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
	@Expose private boolean isPublic=false;
	private String role;
	private Timestamp createDate;	
	private transient String password;		
	private List<Website> websites;
	private long diskQuota;
	
	/**
	 * @return the user's disk quota (in bytes)
	 */
	public long getDiskQuota(){
		return diskQuota;
	}
	public void setId(int ID) {
		
		if (ID==R.PUBLIC_USER_ID){
			this.isPublic=true;
		}
		super.setId(ID);
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
	 * @return whether this is the public user
	 */
	
	public boolean isPublic() {
		return isPublic;
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
	
	@Override
	public String toString() {
		return this.getFullName();
	}
}