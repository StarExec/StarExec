package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a space in the database
 * 
 * @author Tyler Jensen
 */
public class Space extends Identifiable {	
	@Expose	private String name;
	@Expose private String description = "none";;
	private boolean locked;
	private Timestamp created;
	private Permission defaultPermission;
	@Expose	private List<Solver> solvers;
	@Expose	private List<Benchmark> benchmarks;
	@Expose	private List<Job> jobs;
	@Expose	private List<User> users;
	@Expose	private List<Space> subspaces;
	
	public Space() {
		this.solvers = new LinkedList<Solver>();
		this.benchmarks = new LinkedList<Benchmark>();
		this.jobs = new LinkedList<Job>();
		this.users = new LinkedList<User>();
		this.subspaces = new LinkedList<Space>();
	}
	
	/**
	 * @return the default permission entry that represents the permission newly added users have
	 */
	public Permission getPermission() {
		return defaultPermission;
	}
	
	/**
	 * @param permission the default permission to set for this space
	 */
	public void setPermission(Permission permission) {
		this.defaultPermission = permission;
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
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
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

	/**
	 * @return all solvers belonging directly to this space
	 */
	public List<Solver> getSolvers() {
		return this.solvers;
	}

	/**
	 * @param solver the solver to add to this space
	 */
	public void addSolver(Solver solver) {
		this.solvers.add(solver);
	}

	/**
	 * @return all benchmarks belonging directly to this space
	 */
	public List<Benchmark> getBenchmarks() {
		return this.benchmarks;
	}

	/**
	 * @param benchmarks the benchmarks to set
	 */
	public void addBenchmark(Benchmark benchmark) {
		this.benchmarks.add(benchmark);
	}

	/**
	 * @return all jobs belonging directly to this space
	 */
	public List<Job> getJobs() {
		return this.jobs;
	}

	/**
	 * @param job the job to add to the space
	 */
	public void addJob(Job job) {
		this.jobs.add(job);
	}

	/**
	 * @return all users belonging to the space
	 */
	public List<User> getUsers() {
		return this.users;
	}

	/**
	 * @param user the user to add to the space
	 */
	public void addUser(User user) {
		this.users.add(user);
	}

	/**
	 * @param solvers The list of solvers belonging to this space
	 */
	public void setSolvers(List<Solver> solvers) {
		this.solvers = solvers;
	}

	/**
	 * @param benchmarks The list of benchmarks belonging to this space
	 */
	public void setBenchmarks(List<Benchmark> benchmarks) {
		this.benchmarks = benchmarks;
	}

	/**
	 * @param jobs The list of jobs belonging to this space
	 */
	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}

	/**
	 * @param users The list of users belonging to this space
	 */
	public void setUsers(List<User> users) {
		this.users = users;
	}

	/**
	 * @return the list of subspaces belonging to this space
	 */
	public List<Space> getSubspaces() {
		return subspaces;
	}

	/**
	 * @param subspaces the subspaces to set for this space
	 */
	public void setSubspaces(List<Space> subspaces) {
		this.subspaces = subspaces;
	}			
}