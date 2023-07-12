package org.starexec.data.to;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.starexec.data.database.Queues;

/**
 * Represents an SGE queue which has a collection of worker nodes that belong to it
 *
 * @author Tyler Jensen
 */
public class Queue extends Identifiable implements Iterable<WorkerNode>, Nameable {
	@Expose private String name;
	@Expose private String status;
	@Expose private boolean global_access;
	@Expose private final List<WorkerNode> nodes;
	@Expose private final HashMap<String, String> attributes;
	@Expose private final HashMap<Integer, String[]> jobPairs;
	@Expose private int cpuTimeout;
	@Expose private int wallTimeout;

	public Queue() {
		this.nodes = new LinkedList<>();
		this.attributes = new HashMap<>();
		this.jobPairs = new HashMap<>();
	}

	/**
	 * @return the name of the queue
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set for the queue
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the status of the queue
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set for the queue
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return true if the queue is global, false otherwise
	 */
	public boolean getGlobalAccess() {
		return global_access;
	}

	/**
	 * @param global_access true if queue is global, false otherwise
	 */
	public void setGlobalAccess(boolean global_access) {
		this.global_access = global_access;
	}

	/**
	 * @return the worker nodes that belong to the queue
	 */
	public List<WorkerNode> getNodes() {
		return nodes;
	}

	/**
	 * @param node the worker node to add to the queue
	 */
	public void addNode(WorkerNode node) {
		this.nodes.add(node);
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

	/**
	 * @return The job pairs
	 */
	public HashMap<Integer, String[]> getJobPair() {
		return jobPairs;
	}

	public void putJobPair(int key, String[] values) {
		this.jobPairs.put(key, values);
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public Iterator<WorkerNode> iterator() {
		return this.nodes.iterator();
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

	public String getDesc() {
		//get the id of this queue;
		int id = Queues.getIdByName(this.name);
		return Queues.getDescForQueue(id);
	}
}
