package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a benchmark in the database
 * 
 * @author Tyler Jensen
 */
public class Benchmark extends Identifiable implements Iterable<Entry<Object, Object>>{
	private int userId = -1;	
	@Expose private String name;	
	@Expose private String description = "no description";	
	@Expose private Processor type;
	private Timestamp uploadDate;	
	private Properties attributes;
	private String path;
	private boolean isDownloadable;
	private long diskSize;	
	private boolean deleted;
	private boolean recycled;
	
	
	public Benchmark() {
		attributes=new Properties();
	}
	
	/**
	 * @return the user id of the user who uploaded the benchmark
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
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
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

	/**
	 * @return the type the benchmark is
	 */
	public Processor getType() {
		return type;
	}

	/**
	 * @param type the benchmark type to set for this benchmark
	 */
	public void setType(Processor type) {
		this.type = type;
	}
	
	/**
	 * @param diskSize the number of bytes this benchmark consumes on disk
	 */
	public void setDiskSize(long diskSize){
		this.diskSize = diskSize;
	}
	
	/**
	 * @return the number of bytes this benchmark consumes on disk
	 */
	public long getDiskSize(){
		return diskSize;
	}
		
	/**
	 * @return the attributes
	 */
	public Properties getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Properties attributes) {
		this.attributes = attributes;
	}	

	@Override
	public Iterator<Entry<Object, Object>> iterator() {
		return this.attributes.entrySet().iterator();
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setRecycled(boolean recycled) {
		this.recycled = recycled;
	}

	public boolean isRecycled() {
		return recycled;
	}
}
