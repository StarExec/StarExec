package org.starexec.data.to;

import java.io.File;
import java.sql.Timestamp;

import com.google.gson.annotations.Expose;

/**
 * Represents a benchmark in the database
 * 
 * @author Tyler Jensen
 */
public class Benchmark extends Identifiable {
	private long userId = -1;	
	@Expose private String name;	
	@Expose private String description;	
	private Timestamp uploadDate;	
	private transient String path;
	private boolean isDownloadable;
	
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
	 * @return the canonical name of the benchmark
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set for the benchmark
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return the user defined description of the benchmark
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description the description to set for the benchmark
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return the date the benchmark was added to the system
	 */
	public Timestamp getUploadDate() {
		return uploadDate;
	}
	
	/**
	 * @param uploadDate the upload date to set for the benchmark
	 */
	public void setUploadDate(Timestamp uploadDate) {
		this.uploadDate = uploadDate;
	}
	
	/**
	 * @return the absolute file path to the benchmark on disk
	 */
	public String getPath() {
		return path;
	}
	
	/**
	 * @param path the absolute path to set for the benchmark
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 * @return true if this benchmark can be downloaded, false otherwise
	 */
	public boolean isDownloadable() {
		return isDownloadable;
	}
	
	/**
	 * @param isDownloadable sets whether or not this benchmark down be downloaded
	 */
	public void setDownloadable(boolean isDownloadable) {
		this.isDownloadable = isDownloadable;
	}	
}
