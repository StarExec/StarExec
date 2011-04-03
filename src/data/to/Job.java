package data.to;

import java.util.Date;

public class Job {
	private long jobid;
	private Date submitted, completed;
	// How should we associated the solvers and benchmarks???
		
	public void setJobID(long jobid) {
		this.jobid = jobid;
	}
	
	public long getJobID() {
		return this.jobid;
	}
	
	public void setDateSubmitted(Date submitted) {
		this.submitted = submitted;
	}
	
	public Date getDateSubmitted() {
		return this.submitted;
	}
	
	public void setDateCompleted(Date completed) {
		this.completed = completed;
	}
	
	public Date getDateCompleted() {
		return this.completed;
	}
}
