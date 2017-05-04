package org.starexec.data.to;

import com.google.gson.annotations.Expose;
import org.starexec.util.Util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a space in the database.
 * 
 * @author Tyler Jensen
 */
public class Space extends Identifiable implements Iterable<Space>, Nameable {	
	@Expose	private String name;
	@Expose private String description = "no description";
	private boolean locked;
	private boolean isPublic;
	private Timestamp created;
	private Permission defaultPermission;
	@Expose	private List<Solver> solvers;
	@Expose	private List<Benchmark> benchmarks;
	@Expose	private List<Job> jobs;
	@Expose	private List<User> users;
	@Expose	private List<Space> subspaces;
	@Expose private Integer parentSpace;
	@Expose private boolean stickyLeaders;
	public void setParentSpace(Integer space) {
		this.parentSpace = space;
	}
	
	public Integer getParentSpace() {
		return this.parentSpace;
	}
	
	public Space() {
		this.solvers = new LinkedList<>();
		this.benchmarks = new LinkedList<>();
		this.jobs = new LinkedList<>();
		this.users = new LinkedList<>();
		this.subspaces = new LinkedList<>();
		this.defaultPermission = new Permission();
		
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
	 * @return all benchmarks in this space and all subspaces recursively
	 */
	public List<Benchmark> getBenchmarksRecursively() {
		List<Benchmark> benchs = new ArrayList<>();
		benchs.addAll(this.benchmarks);
		for (Space s : this.getSubspaces()) {
			benchs.addAll(s.getBenchmarksRecursively());
		}
		return benchs;
	}

	/**
	 * @param benchmark the benchmark to add to this space
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
	
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public void setPublic(boolean pbc) {
		isPublic = pbc;
	}
	
	@Override
	public Iterator<Space> iterator() {
		return this.subspaces.iterator();
	}

	public void setStickyLeaders(boolean stickyLeaders) {
		this.stickyLeaders = stickyLeaders;
	}

	public boolean isStickyLeaders() {
		return stickyLeaders;
	}			
}
