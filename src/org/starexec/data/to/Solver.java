package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a solver in the database
 * 
 * @author Tyler Jensen
 */
public class Solver extends Identifiable implements Iterable<Configuration> {
	private int userId = -1;
	@Expose	private String name;
	@Expose private String description = "no description";
	@Expose private String description_File = "no description";
	@Expose private String zip_description_File = "no description";
	private Timestamp uploadDate;	
	private transient String path;
	private boolean isDownloadable;	
	private List<Configuration> configurations;
	private long diskSize;
	
	public Solver() {
		this.configurations = new LinkedList<Configuration>();
	}
	
	/**
	 * @return the user id of the user who uploaded the solver
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * @param userId the user id to set as the uploader
	 */
	public void setUserId(int userId) {
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
	 * @return the user defined description of the solver
	 */
	public String getFileDescription() {
		return description_File;
	}
	
	/**
	 * @return the user defined description of the solver
	 */
	public String getZipFileDescription() {
		return zip_description_File;
	}
	
	/**
	 * @param description the description to set for the solver
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
	}	
	
	/**
	 * @param description the description to set for the solver
	 */
	public void setFileDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description_File = description;
		}
	}
	
	/**
	 * @param description the description to set for the solver
	 */
	public void setZipFileDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.zip_description_File = description;
		}
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

	/**
	 * Removes all configurations from the solver
	 */
	public void removeConfigurations() {
		this.configurations.clear();
	}
	
	@Override
	public Iterator<Configuration> iterator() {
		return this.configurations.iterator();
	}	
	
	/**
	 * @param diskSize the number of bytes this solver consumes on disk
	 */
	public void setDiskSize(long diskSize){
		this.diskSize = diskSize;
	}
	
	/**
	 * @return the number of bytes this solver consumes on disk
	 */
	public long getDiskSize(){
		return diskSize;
	}
}
