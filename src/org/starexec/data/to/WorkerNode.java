package org.starexec.data.to;

import com.google.gson.annotations.Expose;

import java.util.HashMap;

/**
 * Represents a worker node in the database
 * @author Tyler Jensen
 */
public class WorkerNode extends Identifiable implements Nameable{
	@Expose private String name;
	@Expose private String status;
	@Expose private HashMap<String, String> attributes;
	@Expose private HashMap<Integer, String[]> jobPairs;
	@Expose private Queue queue;
	public WorkerNode() {
		// Default constructor
		attributes = new HashMap<>();
		jobPairs = new HashMap<>();
		setQueue(new Queue());
	}
	
	/**
	 * @param name The name of the node
	 */
	public WorkerNode(String name) {
		this.name = name;
		attributes = new HashMap<>();
		jobPairs = new HashMap<>();
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
	
	/**
	 * @return The job pairs
	 */
	public HashMap<Integer, String[]> getJobPair() {
		return jobPairs;
	}
	
	public void putJobPair(int key, String[] values) {
		this.jobPairs.put(key, values);
	}

	/**
	 * @param queue the queue to set
	 */
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/**
	 * @return the queue
	 */
	public Queue getQueue() {
		return queue;
	}
}
