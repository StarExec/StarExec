package org.starexec.command;

import java.util.ArrayList;
import java.util.List;


//TODO: This class should not be duplicated from Starexec-- we need to just include it in the distribution
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
	 * @param addSolver1 the add solver permission to set
	 */
	public void setAddSolver(boolean addSolver1) {
		this.addSolver = addSolver1;
	}
	/**
	 * @return if the user has the the add benchmark permission
	 */
	public boolean canAddBenchmark() {
		return addBenchmark;
	}
	/**
	 * @param addBenchmark1 the add benchmark permission to set
	 */
	public void setAddBenchmark(boolean addBenchmark1) {
		this.addBenchmark = addBenchmark1;
	}
	/**
	 * @return if the user has the the add user permission
	 */
	public boolean canAddUser() {
		return addUser;
	}
	/**
	 * @param addUser1 the add user permission to set
	 */
	public void setAddUser(boolean addUser1) {
		this.addUser = addUser1;
	}
	/**
	 * @return if the user has the the add space permission
	 */
	public boolean canAddSpace() {
		return addSpace;
	}
	/**
	 * @param addSpace1 the add space permission to set
	 */
	public void setAddSpace(boolean addSpace1) {
		this.addSpace = addSpace1;
	}
	/**
	 * @return if the user has the the remove solver permission
	 */
	public boolean canRemoveSolver() {
		return removeSolver;
	}
	/**
	 * @param removeSolver1 the remove solver permission to set
	 */
	public void setRemoveSolver(boolean removeSolver1) {
		this.removeSolver = removeSolver1;
	}
	/**
	 * @return if the user has the the remove benchmark permission
	 */
	public boolean canRemoveBench() {
		return removeBench;
	}
	/**
	 * @param removeBench1 the remove benchmark permission to set
	 */
	public void setRemoveBench(boolean removeBench1) {
		this.removeBench = removeBench1;
	}
	/**
	 * @return if the user has the the remove user permission
	 */
	public boolean canRemoveUser() {
		return removeUser;
	}
	/**
	 * @param removeUser1 the remove user permission to set
	 */
	public void setRemoveUser(boolean removeUser1) {
		this.removeUser = removeUser1;
	}
	/**
	 * @return if the user has the the remove space permission
	 */
	public boolean canRemoveSpace() {
		return removeSpace;
	}
	/**
	 * @param removeSpace1 the remove space permission to set
	 */
	public void setRemoveSpace(boolean removeSpace1) {
		this.removeSpace = removeSpace1;
	}	

	/**
	 * @return if the user has the the add job permission
	 */
	public boolean canAddJob() {
		return addJob;
	}

	/**
	 * @param addJob1 the add job permission to set
	 */
	public void setAddJob(boolean addJob1) {
		this.addJob = addJob1;
	}

	/**
	 * @return if the user has the the remove job permission
	 */
	public boolean canRemoveJob() {
		return removeJob;
	}

	/**
	 * @param removeJob1 the remove job permission to set
	 */
	public void setRemoveJob(boolean removeJob1) {
		this.removeJob = removeJob1;
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
