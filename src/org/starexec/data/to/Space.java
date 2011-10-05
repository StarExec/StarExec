package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Represents a space in the database
 * 
 * @author Tyler Jensen
 */
public class Space extends Identifiable {
	private long permissionId = -1;
	private String name;
	private String description;
	private boolean locked;
	private Timestamp created;
	
	/**
	 * @return the id of the default permission entry that represents the permission newly added users have
	 */
	public long getPermissionId() {
		return permissionId;
	}
	
	/**
	 * @param permissionId the default permission id to set for this space
	 */
	public void setPermissionId(long permissionId) {
		this.permissionId = permissionId;
	}
	
	/**
	 * @return the user defined name for the space
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set for the space
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the user defined description of the space
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set for the space
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return true if the space is 'locked' false otherwise (locked indicates no links can be made to the space)
	 */
	public boolean isLocked() {
		return locked;
	}
	
	/**
	 * @param locked the locked property to set for the space
	 */
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	/**
	 * @return the time the space was created
	 */
	public Timestamp getCreated() {
		return created;
	}
	
	/**
	 * @param created the creation time to set for the space
	 */
	public void setCreated(Timestamp created) {
		this.created = created;
	}		
}