package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Base class for all transfer objects that have a unique
 * identifier in the database.
 * 
 * @author Tyler Jensen
 */
public class Identifiable {
	@Expose private int id = -1;

	/**
	 * @return the unique ID of the object in the database
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * DO NOT set the ID of an identifiable outside of the database layer!
	 * @param id the unique ID of the object in the database.
	 */
	public void setId(int id) {
		this.id = id;
	}	
}
