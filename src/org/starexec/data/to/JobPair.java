package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.Properties;

import org.starexec.constants.R;

/**
 * Represents a job pair which is a single unit of execution consisting of a solver(config)/benchmark pair
 * @author Tyler Jensen
 */
public class JobPair extends Identifiable {	
	private int jobId = -1;
	private int gridEngineId = -1;
	private int cpuTimeout = -1;
	private int wallclockTimeout = -1;
	private int completionId=-1;
	private WorkerNode node = null;
	private Solver solver = null;
	private Benchmark bench = null;	
	private Status status = null;
	private Properties attributes = null;
	
	private Timestamp queueSubmitTime = null;
	private Timestamp startTime = null;
	private Timestamp endTime = null;	
	private int exitStatus;
	private double wallclockTime;
	private double cpuUsage;
	private double userTime;
	private double systemTime;
	private double ioDataUsage;
	private double ioDataWait;
	private double memoryUsage;
	private double maxVirtualMemory;
	private double maxResidenceSetSize;
	private double pageReclaims;
	private double pageFaults;
	private double blockInput;
	private double blockOutput;
	private double voluntaryContextSwitches;
	private double involuntaryContextSwitches;
	private Configuration configuration = null;
	private Space space = null;//the space that the benchmark is in, not where the job is initiated
	private String path=null; //A list of spaces seperated by '/' marks giving the path from the space
							  //the job is initiated to the space the benchmark is in
	public JobPair() {
		this.node = new WorkerNode();
		this.solver = new Solver();
		this.bench = new Benchmark();
		this.status = new Status();		
	}
	
	/**
	 * @return the database id of the starexec job this pair belongs to
	 */
	public int getJobId() {
		return jobId;
	}
	
	/**
	 * @param jobId the starexec job id to set for this pair
	 */
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}
	
	public int getCompletionId() {
		return completionId;
	}
	
	public void setCompletionId(int completionId) {
		this.completionId=completionId;
	}
	
	/**
	 * @return the attributes for this job pair
	 */
	public Properties getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes the attributes to set for this job pair
	 */
	public void setAttributes(Properties attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the actual job id of this pair in the grid engine
	 */
	public int getGridEngineId() {
		return gridEngineId;
	}
	
	/**
	 * @param gridEngineId the grid engine id to set for this pair
	 */
	public void setGridEngineId(int gridEngineId) {
		this.gridEngineId = gridEngineId;
	}
		
	/**
	 * @return the maximum amount of cpu time (in seconds) a job pair can run
	 */
	public int getCpuTimeout() {
		return this.cpuTimeout;
	}

	/**
	 * @param timeout the cpu timeout for the job pair
	 */
	public void setCpuTimeout(int timeout) {
		this.cpuTimeout = timeout;
	}
	
	/**
	 * @return the maximum amount of wallclock time (in seconds) a job pair can run
	 */
	public int getWallclockTimeout() {
		return this.wallclockTimeout;
	}

	/**
	 * @param timeout the wallclock timeout for the job pair
	 */
	public void setWallclockTimeout(int timeout) {
		this.wallclockTimeout = timeout;
	}
	
	/**
	 * @return the node this pair ran on
	 */
	public WorkerNode getNode() {
		return node;
	}
	
	/**
	 * @param node the node to set for this pair
	 */
	public void setNode(WorkerNode node) {
		this.node = node;
	}
	
	/**
	 * @return the solver used in this pair
	 */
	public Solver getSolver() {
		return solver;
	}
	
	/**
	 * @param solver the solver to set for this pair
	 */
	public void setSolver(Solver solver) {
		this.solver = solver;
	}
	
	/**
	 * @return the starexec-result value from attributes list
	 */
	public String getStarexecResult() {
		Properties prop = this.getAttributes();
		return (prop != null && prop.containsKey(R.STAREXEC_RESULT)) 
			? prop.getProperty(R.STAREXEC_RESULT) : "--";
	}
	
	/**
	 * @return the benchmark used in this pair
	 */
	public Benchmark getBench() {
		return bench;
	}
	
	/**
	 * @param bench the benchmark to set for this pair
	 */
	public void setBench(Benchmark bench) {
		this.bench = bench;
	}
	
	/**
	 * @return the time this pair started executing
	 */
	public Timestamp getStartTime() {
		return startTime;
	}
	
	/**
	 * @param startTime the start time to set for this pair
	 */
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}
	
	/**
	 * @return the time this pair stopped executing
	 */
	public Timestamp getEndTime() {
		return endTime;
	}
	
	/**
	 * @param endTime the end time to set for this pair
	 */
	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}
	
	/**
	 * @return the status of the pair's execution
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * @param status the status to set for this pair
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the time the pair was submitted to the sge queue
	 */
	public Timestamp getQueueSubmitTime() {
		return queueSubmitTime;
	}

	/**
	 * @param queueSubmitTime queue submit time to set for this pair
	 */
	public void setQueueSubmitTime(Timestamp queueSubmitTime) {
		this.queueSubmitTime = queueSubmitTime;
	}

	/**
	 * @return the exit status of the job on the grid engine
	 */
	public int getExitStatus() {
		return exitStatus;
	}

	/**
	 * @param exitStatus the exit status to set for this pair
	 */
	public void setExitStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}

	/**
	 * @return the wallclock time it took for this pair to execute in seconds
	 */
	public double getWallclockTime() {
		return wallclockTime;
	}

	/**
	 * @param wallclockTime the wallclock time to set for this pair
	 */
	public void setWallclockTime(double wallclockTime) {
		this.wallclockTime = wallclockTime;
	}

	/**
	 * @return the cpu time usage in seconds
	 */
	public double getCpuUsage() {
		return cpuUsage;
	}

	/**
	 * @param cpuUsage the cpu usage to set for this pair
	 */
	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	/**
	 * @return the the total amount of time spent executing in user mode in seconds + microseconds
	 */
	public double getUserTime() {
		return userTime;
	}

	/**
	 * @param userTime the user time to set for this pair
	 */
	public void setUserTime(double userTime) {
		this.userTime = userTime;
	}

	/**
	 * @return the total amount of time spent executing in kernel mode
	 */
	public double getSystemTime() {
		return systemTime;
	}

	/**
	 * @param systemTime the system time to set for this pair
	 */
	public void setSystemTime(double systemTime) {
		this.systemTime = systemTime;
	}

	/**
	 * @return the amount of data transferred in input/output operations
	 */
	public double getIoDataUsage() {
		return ioDataUsage;
	}

	/**
	 * @param ioDataUsage the io data usage to set for this pair
	 */
	public void setIoDataUsage(double ioDataUsage) {
		this.ioDataUsage = ioDataUsage;
	}

	/**
	 * @return the io wait time in seconds
	 */
	public double getIoDataWait() {
		return ioDataWait;
	}

	/**
	 * @param ioDataWait the io wait time to set for this job
	 */
	public void setIoDataWait(double ioDataWait) {
		this.ioDataWait = ioDataWait;
	}

	/**
	 * @return the integral memory usage in Gbytes seconds
	 */
	public double getMemoryUsage() {
		return memoryUsage;
	}

	/**
	 * @param memoryUsage the memory usage to set for this pair
	 */
	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage = memoryUsage;
	}

	/**
	 * @return the maximum vmem size in bytes
	 */
	public double getMaxVirtualMemory() {
		return maxVirtualMemory;
	}

	/**
	 * @param maxVirtualMemory the maximum virtual memory size to set for this pair
	 */
	public void setMaxVirtualMemory(double maxVirtualMemory) {
		this.maxVirtualMemory = maxVirtualMemory;
	}

	/**
	 * @return the maximum resident set size used (in kilobytes)
	 */
	public double getMaxResidenceSetSize() {
		return maxResidenceSetSize;
	}

	/**
	 * @param d the maxResidenceSetSize to set for this pair
	 */
	public void setMaxResidenceSetSize(double d) {
		this.maxResidenceSetSize = d;
	}

	/**
	 * @return the number of page faults serviced without any I/O activity
	 */
	public double getPageReclaims() {
		return pageReclaims;
	}

	/**
	 * @param pageReclaims the page reclaims to set for this pair
	 */
	public void setPageReclaims(double pageReclaims) {
		this.pageReclaims = pageReclaims;
	}

	/**
	 * @return the number of page faults serviced that required I/O activity
	 */
	public double getPageFaults() {
		return pageFaults;
	}

	/**
	 * @param pageFaults the page faults to set for this pair
	 */
	public void setPageFaults(double pageFaults) {
		this.pageFaults = pageFaults;
	}

	/**
	 * @return the number of times the file system had to perform input
	 */
	public double getBlockInput() {
		return blockInput;
	}

	/**
	 * @param blockInput the block input to set for this pair
	 */
	public void setBlockInput(double blockInput) {
		this.blockInput = blockInput;
	}

	/**
	 * @return the number of times the file system had to perform output
	 */
	public double getBlockOutput() {
		return blockOutput;
	}

	/**
	 * @param blockOutput the block output to set for this pair
	 */
	public void setBlockOutput(double blockOutput) {
		this.blockOutput = blockOutput;
	}

	/**
	 * @return the number of times a context switch resulted due to a process voluntarily giving up the processor before its time slice was completed
	 */
	public double getVoluntaryContextSwitches() {
		return voluntaryContextSwitches;
	}

	/**
	 * @param voluntaryContextSwitches the voluntary context switches to set for this pair
	 */
	public void setVoluntaryContextSwitches(double voluntaryContextSwitches) {
		this.voluntaryContextSwitches = voluntaryContextSwitches;
	}

	/**
	 * @return the number of times a context switch resulted due to a higher priority process becoming runnable or because the current process exceeded its time slice
	 */
	public double getInvoluntaryContextSwitches() {
		return involuntaryContextSwitches;
	}

	/**
	 * @param involuntaryContextSwitches the involuntary context switches to set for this pair
	 */
	public void setInvoluntaryContextSwitches(double involuntaryContextSwitches) {
		this.involuntaryContextSwitches = involuntaryContextSwitches;
	}

	/**
	 * @return the configuration
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * @return the space
	 */
	public Space getSpace() {
		return space;
	}

	/**
	 * @param space the space to set
	 */
	public void setSpace(Space space) {
		this.space = space;
	}
	/**
	 * @param path The path of this job_pair
	 */
	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}