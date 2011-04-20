package com.starexec.data.to;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Job {
	private int jobid, userid;
	private Date submitted, completed;
	private String description, status, node;	// TODO: Create a node TO later on with hardware info?
	private long timeout;	
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
	
	public Date getSubmitted() {
		return submitted;
	}
	
	public void setSubmitted(Date submitted) {
		this.submitted = submitted;
	}
	
	public Date getCompleted() {
		return completed;
	}
	
	public void setCompleted(Date completed) {
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
}