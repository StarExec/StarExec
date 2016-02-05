package org.starexec.command;

public class PollJobData {

	public int since=0;
	public long lastModified=0;
	
	public PollJobData() {
		
	}
	public PollJobData(int since, long lastModified) {
		this.since=since;
		this.lastModified=lastModified;
	}
}
