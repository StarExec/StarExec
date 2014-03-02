package org.starexec.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a permission between a user and a space, or a default permission
 * on a space. All fields are false by default.
 * @author Tyler Jensen
 *
 */
public class Permission {
	private boolean addSolver = false;
	private boolean addBenchmark = false;
	private boolean addUser = false;
	private boolean addSpace = false;
	private boolean addJob = false;
	private boolean removeSolver = false;
	private boolean removeBench = false;
	private boolean removeUser = false;
	private boolean removeSpace = false;
	private boolean removeJob = false;
	
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
	
	protected void setPermissionOn(String perm) {
		if (perm.equalsIgnoreCase("addSolver")) {
			this.addSolver=true;
		}
		if (perm.equalsIgnoreCase("addBench")) {
			this.addBenchmark=true;
		}
		if (perm.equalsIgnoreCase("addSpace")) {
			this.addSpace=true;
		}
		if (perm.equalsIgnoreCase("addJob")) {
			this.addJob=true;
		}
		if (perm.equalsIgnoreCase("addUser")) {
			this.addUser=true;
		}
		if (perm.equalsIgnoreCase("removeSolver")) {
			this.removeSolver=true;
		}
		if (perm.equalsIgnoreCase("removeSpace")) {
			this.removeSpace=true;
		}
		if (perm.equalsIgnoreCase("removeUser")) {
			this.removeUser=true;
		}
		if (perm.equalsIgnoreCase("removeJob")) {
			this.removeJob=true;
		}
		if (perm.equalsIgnoreCase("removeBench")) {
			this.removeBench=true;
		}
	}
	/**
	 * Gets all the permissions that are turned on, as strings that are
	 * recognized by the server
	 * @return
	 */
	protected List<String> getOnPermissions() {
		List<String> perms=new ArrayList<String>();
		if (canAddSolver()) {
			perms.add("addSolver");
		}
		if (canAddUser()) {
			perms.add("addUser");
		}
		if (canAddSpace()) {
			perms.add("addSpace");
		}
		if (canAddBenchmark()) {
			perms.add("addBench");
		}
		if (canAddJob()) {
			perms.add("addJob");
		}
		if (canRemoveSolver()) {
			perms.add("removeSolver");
		}
		if (canRemoveUser()) {
			perms.add("removeUser");
		}
		if (canRemoveBench()) {
			perms.add("removeBench");
		}
		if (canRemoveJob()) {
			perms.add("removeJob");
		}
		if (canRemoveSpace()) {
			perms.add("removeSpace");
		}
		
		return perms;
	}
	
	/**
	 * Gets all the permissions that are turned on, as strings that are
	 * recognized by the server
	 * @return
	 */
	protected List<String> getOffPermissions() {
		List<String> perms=new ArrayList<String>();
		if (!canAddSolver()) {
			perms.add("addSolver");
		}
		if (!canAddUser()) {
			perms.add("addUser");
		}
		if (!canAddSpace()) {
			perms.add("addSpace");
		}
		if (!canAddBenchmark()) {
			perms.add("addBench");
		}
		if (!canAddJob()) {
			perms.add("addJob");
		}
		if (!canRemoveSolver()) {
			perms.add("removeSolver");
		}
		if (!canRemoveUser()) {
			perms.add("removeUser");
		}
		if (!canRemoveBench()) {
			perms.add("removeBench");
		}
		if (!canRemoveJob()) {
			perms.add("removeJob");
		}
		if (!canRemoveSpace()) {
			perms.add("removeSpace");
		}
		
		return perms;
	}
	
	
}
