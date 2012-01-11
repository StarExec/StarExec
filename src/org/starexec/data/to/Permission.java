package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Represents a permission between a user and a space, or a default permission
 * on a space. All fields are false by default.
 * @author Tyler Jensen
 *
 */
public class Permission extends Identifiable {
	@Expose private boolean addSolver = false;
	@Expose private boolean addBenchmark = false;
	@Expose private boolean addUser = false;
	@Expose private boolean addSpace = false;
	@Expose private boolean addJob = false;
	@Expose private boolean removeSolver = false;
	@Expose private boolean removeBench = false;
	@Expose private boolean removeUser = false;
	@Expose private boolean removeSpace = false;
	@Expose private boolean removeJob = false;
	@Expose private boolean isLeader = false;
	
	public Permission() {
		// Default constructor, do nothing
	}
	
	/**
	 * Constructor which sets all permissions to the given default
	 * @param defaultPerm The value to set all permissions to
	 */
	public Permission(boolean defaultPerm) {
		this.addSolver = defaultPerm;
		this.addBenchmark = defaultPerm;
		this.addUser = defaultPerm;
		this.addSpace = defaultPerm;
		this.addJob = defaultPerm;
		this.removeSolver = defaultPerm;
		this.removeBench = defaultPerm;
		this.removeUser = defaultPerm;
		this.removeSpace = defaultPerm;
		this.removeJob = defaultPerm;
		this.isLeader = defaultPerm;
	}
	
	/**
	 * @return if the user has the the add solver permission
	 */
	public boolean canAddSolver() {
		return addSolver;
	}
	/**
	 * @param addSolver the add solver permission to set
	 */
	public void setAddSolver(boolean addSolver) {
		this.addSolver = addSolver;
	}
	/**
	 * @return if the user has the the add benchmark permission
	 */
	public boolean canAddBenchmark() {
		return addBenchmark;
	}
	/**
	 * @param addBenchmark the add benchmark permission to set
	 */
	public void setAddBenchmark(boolean addBenchmark) {
		this.addBenchmark = addBenchmark;
	}
	/**
	 * @return if the user has the the add user permission
	 */
	public boolean canAddUser() {
		return addUser;
	}
	/**
	 * @param addUser the add user permission to set
	 */
	public void setAddUser(boolean addUser) {
		this.addUser = addUser;
	}
	/**
	 * @return if the user has the the add space permission
	 */
	public boolean canAddSpace() {
		return addSpace;
	}
	/**
	 * @param addSpace the add space permission to set
	 */
	public void setAddSpace(boolean addSpace) {
		this.addSpace = addSpace;
	}
	/**
	 * @return if the user has the the remove solver permission
	 */
	public boolean canRemoveSolver() {
		return removeSolver;
	}
	/**
	 * @param removeSolver the remove solver permission to set
	 */
	public void setRemoveSolver(boolean removeSolver) {
		this.removeSolver = removeSolver;
	}
	/**
	 * @return if the user has the the remove benchmark permission
	 */
	public boolean canRemoveBench() {
		return removeBench;
	}
	/**
	 * @param removeBench the remove benchmark permission to set
	 */
	public void setRemoveBench(boolean removeBench) {
		this.removeBench = removeBench;
	}
	/**
	 * @return if the user has the the remove user permission
	 */
	public boolean canRemoveUser() {
		return removeUser;
	}
	/**
	 * @param removeUser the remove user permission to set
	 */
	public void setRemoveUser(boolean removeUser) {
		this.removeUser = removeUser;
	}
	/**
	 * @return if the user has the the remove space permission
	 */
	public boolean canRemoveSpace() {
		return removeSpace;
	}
	/**
	 * @param removeSpace the remove space permission to set
	 */
	public void setRemoveSpace(boolean removeSpace) {
		this.removeSpace = removeSpace;
	}	

	/**
	 * @return if the user has the the add job permission
	 */
	public boolean canAddJob() {
		return addJob;
	}

	/**
	 * @param addJob the add job permission to set
	 */
	public void setAddJob(boolean addJob) {
		this.addJob = addJob;
	}

	/**
	 * @return if the user has the the remove job permission
	 */
	public boolean canRemoveJob() {
		return removeJob;
	}

	/**
	 * @param removeJob the remove job permission to set
	 */
	public void setRemoveJob(boolean removeJob) {
		this.removeJob = removeJob;
	}	
	
	/**
	 * @return if the user is a space leader
	 */
	public boolean isLeader() {
		return isLeader;
	}
	
	/**
	 * @param isLeader indicates if the user is a space leader
	 */
	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}
}
