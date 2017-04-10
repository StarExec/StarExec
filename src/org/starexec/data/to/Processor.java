package org.starexec.data.to;

import java.io.File;

import org.starexec.constants.R;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.data.to.tuples.Locatable;
import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a processor, which is an arbitrary user specified filed that takes
 * input and produces output. This is used at various stages in the job pipeline
 * @author Tyler Jensen 
 */
public class Processor extends Identifiable implements Nameable, Locatable {

	
	@Expose private String name = "none";
	@Expose private String description = "no description";
	@Expose private String fileName;
	@Expose private ProcessorType type = ProcessorType.DEFAULT;
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
	 * Alias for getFilePath, needed for Locatable interface.
	 * @return
	 */
	@Override
	public String getPath() {
		return this.getFilePath();
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
		return new File(this.getFilePath(),R.PROCESSOR_RUN_SCRIPT).getAbsolutePath();
	}

	/**
	 * Alias for setFilePath, needed for Locatable interface.
	 * @param processorPath The physical path to set for the processor
	 */
	@Override
	public void setPath(String processorPath) {
		setFilePath(processorPath);
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
