package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a solver in the database
 * 
 * @author Tyler Jensen
 */
public class Solver extends Identifiable implements Iterable<Configuration>, Nameable{
	
	/**
	 * Represents the type of the processor (along with it's SQL storage values)
	 */
	public static enum ExecutableType {
		
		SOLVER(1), 
		TRANSFORMER(2), 
		RESULTCHECKER(3),
		OTHER(4);
		
		private int val;
		
		private ExecutableType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static ExecutableType valueOf(int val) {
			switch(val) {			
				case 1:
					return SOLVER;
				case 2:
					return TRANSFORMER;
				case 3:
					return RESULTCHECKER;
			    case 4:
				   return OTHER;
				default:
					return null;				
			}
		}
	}
	
	
	private int userId = -1;
	@Expose	private String name;
	@Expose private String description = "no description";
	private Timestamp uploadDate;	
	private String mostRecentUpdateString;
	private transient String path;
	private boolean isDownloadable;	
	private List<Configuration> configurations;
	private long diskSize;
	private boolean recycled;
	private boolean deleted;
	private ExecutableType type;
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
	 * @param description the description to set for the solver
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
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

	public void setMostRecentUpdate(String mostRecentUpdate) {
		this.mostRecentUpdateString = mostRecentUpdate;
	}

	public String getMostRecentUpdate() {
		return mostRecentUpdateString;
	}

	public void setRecycled(boolean recycled) {
		this.recycled = recycled;
	}

	public boolean isRecycled() {
		return recycled;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public ExecutableType getType() {
		return type;
	}

	public void setType(ExecutableType type) {
		this.type = type;
	}
}
