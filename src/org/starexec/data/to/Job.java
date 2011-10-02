package org.starexec.data.to;

import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.List;

/**
 * @deprecated This class is out of date and needs to be updated
 */
public class Job {
	private int jobid;
	private int communityId;
	private int userid;
	private Timestamp submitted, completed;
	private String description, status, node;	// TODO: Create a node TO later on with hardware info?
	private long timeout;	
	private String runTime = "";
	private List<JobPair> jobPairs;
	
	public Job(){
		jobPairs = new ArrayList<JobPair>();
	}
	
	public Job(int id){
		this();
		setJobId(id);
	}
	
	public int getJobId() {
		return jobid;
	}
	
	public void setJobId(int jobid) {
		this.jobid = jobid;
	}
	
	public int getUserId() {
		return userid;
	}
	
	public void setUserId(int userid) {
		this.userid = userid;
	}
	
	public Timestamp getSubmitted() {
		return submitted;
	}
	
	public void setSubmitted(Timestamp submitted) {
		this.submitted = submitted;
	}
	
	public Timestamp getCompleted() {
		return completed;
	}
	
	public void setCompleted(Timestamp completed) {
		this.completed = completed;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getNode() {
		return node;
	}
	
	public void setNode(String node) {
		this.node = node;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public List<JobPair> getJobPairs() {
		return jobPairs;
	}	
	
	public boolean addJobPair(JobPair jp){
		return jobPairs.add(jp);
	}
		
	public String getRunTime() {
		return runTime;
	}

	/**
	 * @param runTime The run time in total seconds (which is converted to a time string)
	 */
	public void setRunTime(int runTime) {
		runTime = Math.abs(runTime);
		int minutes = runTime / 60;
		int hours = runTime / (3600);
		int days = runTime / (86400);
		int seconds = runTime - (minutes * 60) - (hours * 3600) - (days * 86400);
		
		
		this.runTime = String.format("%sd %sh %sm %ss", days, hours, minutes, seconds);
	}

	public int getCommunityId() {
		return communityId;
	}

	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}	
}