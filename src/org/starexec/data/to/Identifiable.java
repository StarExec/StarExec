package org.starexec.data.to;

/**
 * Base class for all transfer objects that have a unique
 * identifier in the database.
 * 
 * @author Tyler Jensen
 */
public class Identifiable {
	private long id = -1;

	/**
	 * @return the unique ID of the object in the database
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * DO NOT set the ID of an identifiable outside of the database layer!
	 * @param the unique ID of the object in the database.
	 */
	public void setId(long id) {
		this.id = id;
	}	
}
