package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.util.Util;

import com.google.gson.annotations.Expose;

/**
 * Represents a job in the database
 * 
 * @author Tyler Jensen
 */
public class Job extends Identifiable implements Iterable<JobPair> {
	private int userId = -1;		
	@Expose private String name;
	@Expose private String description = "no description";
	private Queue queue = null;
	@Expose private Timestamp createTime;
	private List<JobPair> jobPairs;
	private HashMap<String, Integer> liteJobPairStats;
	private Processor preProcessor;
	private Processor postProcessor;	
	
	public Job() {
		jobPairs = new LinkedList<JobPair>();
		preProcessor = new Processor();
		postProcessor = new Processor();
		queue = new Queue();		
	}
	
	/**
	 * @return the user id of the user who created the job
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * @param userId the user id to set as the creator
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * @return the user defined name for the job
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set for the job
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the date the job was created
	 */
	public Timestamp getCreateTime() {
		return this.createTime;
	}

	/**
	 * @param created the creation date to set for the job
	 */
	public void setCreateTime(Timestamp created) {
		this.createTime = created;
	}

	/**
	 * @return the user defined description of the job
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description for the job
	 */
	public void setDescription(String description) {
		if(!Util.isNullOrEmpty(description)) {
			this.description = description;
		}
	}
	
	/**
	 * @return the list of job pairs belonging to this job
	 */
	public List<JobPair> getJobPairs() {
		return jobPairs;
	}
	
	/**
	 * @param jobPairs the list of job pairs belonging to this job
	 */
	public void setJobPairs(List<JobPair> jobPairs) {
		this.jobPairs = jobPairs;
	}

	/**
	 * @return the attribute names for the first completed job pair in the job,
	 * or null if there are no completed job pairs.
	 */
	public Set<String> attributeNames() {
	    Iterator<JobPair> itr = jobPairs.iterator();
	    while(itr.hasNext()) {
		JobPair pair = itr.next();
		Properties props = pair.getAttributes();
		if (pair.getStatus().getCode() == StatusCode.STATUS_COMPLETE) 
		    return props.stringPropertyNames(); 
	    }
	    return null;
	}

	/**
	 * @param jobPair the job pair to add to the job
	 */
	public void addJobPair(JobPair jobPair) {
		jobPairs.add(jobPair);
	}
	
	/**
	 * @return the queue this job is intended to run on
	 */
	public Queue getQueue() {
		return queue;
	}

	/**
	 * @param queue the queue to set for this job
	 */
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/**
	 * @return the pre processor this job uses
	 */
	public Processor getPreProcessor() {
		return preProcessor;
	}

	/**
	 * @param preProcessor the preProcessor to set for this job
	 */
	public void setPreProcessor(Processor preProcessor) {
		this.preProcessor = preProcessor;
	}

	/**
	 * @return the post processor this job uses
	 */
	public Processor getPostProcessor() {
		return postProcessor;
	}

	/**
	 * @param postProcessor the post processor to set for this job
	 */
	public void setPostProcessor(Processor postProcessor) {
		this.postProcessor = postProcessor;
	}

	@Override
	public Iterator<JobPair> iterator() {
		return this.jobPairs.iterator();
	}
	
	/**
	 * @param ljps the job pair statistics to store in this object
	 */
	public void setLiteJobPairStats(HashMap<String, Integer> ljps){
		this.liteJobPairStats = ljps;
	}
	
	/**
	 * @return the job pair statistics stored in this object
	 */
	public HashMap<String, Integer> getLiteJobPairStats(){
		return liteJobPairStats;
	}
}