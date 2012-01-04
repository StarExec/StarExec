package org.starexec.data.to;

import java.util.HashMap;

import com.google.gson.annotations.Expose;

/**
 * Represents a worker node in the database
 * @author Tyler Jensen
 */
public class WorkerNode extends Identifiable {
	@Expose private String name;
	@Expose private String status;
	@Expose private HashMap<String, String> attributes;
	
	public WorkerNode() {
		// Default constructor
		attributes = new HashMap<String, String>();
	}
	
	/**
	 * @param name The name of the node
	 */
	public WorkerNode(String name) {
		this.name = name;
		attributes = new HashMap<String, String>();
	}
	
	/**
	 * @return The node's canonical name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set for this node
	 */
	public void setName(String name) {
		this.name = name;
	}		

	/**
	 * @return the status of the node
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set for the node
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
	/**
	 * @return The attributes for this worker node
	 */
	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param key The key of the attribute to add
	 * @param val The value of the given attribute
	 */
	public void putAttribute(String key, String val) {
		this.attributes.put(key, val);
	}

	@Override
	public String toString() {
	 return this.name;
	}
}
