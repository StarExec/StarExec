package org.starexec.data.to;

import java.io.File;

import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a benchmark type, which is defined so users can specify how the benchmarks
 * are parsed for attributes.
 * @author Tyler Jensen 
 */
public class BenchmarkType extends Identifiable {
	@Expose private String name;
	@Expose private String description = "none";
	@Expose private String processorName;
	private String processorPath;	
	private long communityId;
	
	/**
	 * @return The user-defined name for the type
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name The name to set for the type
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return This type's user-defined description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * @param description The description to set for this type
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
	}
	
	/**
	 * @return The name of the processor (filename)
	 */
	public String getProcessorName() {
		return this.processorName;
	}
	
	/**
	 * @return The physical path to the processor for this type
	 */
	public String getProcessorPath() {
		return this.processorPath;
	}
	
	/**
	 * @param processorPath The physical path to set for the processor for this type
	 */
	public void setProcessorPath(String processorPath) {
		this.processorPath = processorPath;			
		this.processorName = this.processorPath.substring(this.processorPath.lastIndexOf(File.separator) + 1);		
	}
	
	/**
	 * @return The id of the community this type belongs to 
	 */
	public long getCommunityId() {
		return communityId;
	}
	
	/**
	 * @param communityId The id of the owning community to set for this type
	 */
	public void setCommunityId(long communityId) {
		this.communityId = communityId;
	}		
}
