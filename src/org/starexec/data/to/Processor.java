package org.starexec.data.to;

import java.io.File;

import org.starexec.constants.R;
import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a processor, which is an arbitrary user specified filed that takes
 * input and produces output. This is used at various stages in the job pipeline
 * @author Tyler Jensen 
 */
public class Processor extends Identifiable {
	/**
	 * Represents the type of the processor (along with it's SQL storage values)
	 */
	public static enum ProcessorType {
		DEFAULT(0), 
		PRE(1), 
		POST(2), 
		BENCH(3);
		
		private int val;
		
		private ProcessorType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static ProcessorType valueOf(int val) {
			switch(val) {			
				case 1:
					return PRE;
				case 2:
					return POST;
				case 3:
					return BENCH;
				default:
					return DEFAULT;				
			}
		}
	}
	
	@Expose private String name = "none";
	@Expose private String description = "no description";
	@Expose private String fileName;
	@Expose private ProcessorType type = Processor.ProcessorType.DEFAULT;	
	private String filePath;	
	private long diskSize;
	private int communityId;
	
	/**
	 * @return the type of the processor (bench, pre or post proessor)
	 */
	public ProcessorType getType() {
		return type;
	}

	/**
	 * @param type the type to set for the processor (pre, post or bench)
	 */
	public void setType(ProcessorType type) {
		this.type = type;
	}

	/**
	 * @return The user-defined name for the processor
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name The name to set for the processor
	 */
	public void setName(String name) {
		if(!Util.isNullOrEmpty(name)) {
			this.name = name;
		}
	}
	
	/**
	 * @return This processors's user-defined description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description The description to set for this processor
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
	}
	
	/**
	 * @return The file name of the processor
	 */
	public String getFileName() {
		return this.fileName;
	}
	
	/**
	 * @return The physical path to the directory containing this processor
	 */
	public String getFilePath() {
		return this.filePath;
	}
	
	/**
	 * Gets the physical path to the executable script for this processor. Requires filePath to be set.
	 * @return The path to the process script for this processor on disk
	 */
	public String getExecutablePath() {
		return new File(this.getFilePath(),R.PROCSSESSOR_RUN_SCRIPT).getAbsolutePath();
	}
	
	/**
	 * @param processorPath The physical path to set for the processor
	 */
	public void setFilePath(String processorPath) {
		if (!Util.isNullOrEmpty(processorPath)) {
			this.filePath = processorPath;
			this.fileName = this.filePath.substring(this.filePath.lastIndexOf(File.separator) + 1);
		}
	}
	/**
	 * @return The id of the community this processor belongs to 
	 */
	public int getCommunityId() {
		return communityId;
	}
	
	/**
	 * @param communityId The id of the owning community to set for this processor
	 */
	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}		
	
	/**
	 * @return the number of bytes this processor consumes on disk
	 */
	public long getDiskSize(){
		return diskSize;
	}
	
	/**
	 * @param diskSize the number of bytes this processor consumes on disk
	 */
	public void setDiskSize(long diskSize){
		this.diskSize = diskSize;
	}
}