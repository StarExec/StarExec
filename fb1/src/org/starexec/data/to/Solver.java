package org.starexec.data.to;

import java.io.File;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 * Represents a solver in the database
 * 
 * @author Tyler Jensen
 */
public class Solver extends Identifiable {
	private long userId = -1;
	@Expose	private String name;
	@Expose	private String description;	
	private Timestamp uploadDate;	
	private transient String path;
	private boolean isDownloadable;	
	private List<Configuration> configurations;
	
	public Solver() {
		this.configurations = new LinkedList<Configuration>();
	}
	
	/**
	 * @return the user id of the user who uploaded the solver
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * @param userId the user id to set as the uploader
	 */
	public void setUserId(long userId) {
		this.userId = userId;
	}

	/**
	 * @return the canonical name of the solver
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set for the solver
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the user defined description of the solver
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set for the solver
	 */
	public void setDescription(String description) {
		this.description = description;
	}	
	
	/**
	 * @return the date the solver was added to the system
	 */
	public Timestamp getUploadDate() {
		return uploadDate;
	}
	
	/**
	 * @param uploadDate the upload date to set for the solver
	 */
	public void setUploadDate(Timestamp uploadDate) {
		this.uploadDate = uploadDate;
	}
	
	/**
	 * @return the absolute file path to the solver on disk
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * @param path the absolute path to set for the solver
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * @return true if this solver can be downloaded, false otherwise
	 */
	public boolean isDownloadable() {
		return isDownloadable;
	}
	
	/**
	 * @param isDownloadable sets whether or not this solver down be downloaded
	 */
	public void setDownloadable(boolean isDownloadable) {
		this.isDownloadable = isDownloadable;
	}

	/**
	 * @return the configurations that belong to the job
	 */
	public List<Configuration> getConfigurations() {
		return configurations;
	}

	/**
	 * @param configuration the configuration to add to the job
	 */
	public void addConfiguration(Configuration configuration) {
		this.configurations.add(configuration);
	}	
}
