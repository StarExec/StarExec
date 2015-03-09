package org.starexec.data.to;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.starexec.constants.R;
import org.starexec.data.to.compare.JoblineStageComparator;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineStage;

/**
 * Represents a job pair which is a single unit of execution consisting of a solver(config)/benchmark pair
 * @author Tyler Jensen
 */
public class JobPair extends Identifiable {	
	private int jobId = -1;
	private int gridEngineId = -1;
	


	private int completionId=-1;
	private int jobSpaceId=-1;
	private String jobSpaceName="";
	private WorkerNode node = null;
	private Benchmark bench = null;	//this is the input benchmark to the jobline
	private Status status = null;
	private Timestamp queueSubmitTime = null;
	private Timestamp startTime = null;
	private Timestamp endTime = null;	
	private int exitStatus;
	private List<JoblineStage> stages=null; // this is an ordered list of all the stages in this jobline
	
	// this field says what the primary stage is by stage number. It is used during job construction,
	// before the job is loaded into the database, as before thta there are no ids. This is not necessary
	// and will not be set for jobs after creation.
	private Integer primaryStageNumber=null;
	
	private int sandboxNum;
	private Space space = null;//the space that the benchmark is in, not where the job is initiated
	private String path=null; //A list of spaces seperated by '/' marks giving the path from the space
							  //the job is initiated to the space the benchmark is in

	
	//the inputs to this job pair, excluding the primary benchmark (in other words, the dependencies stored in the
	//jobpair_inputs table
	private List<Integer> benchInputs;
	
	public JobPair() {
		this.node = new WorkerNode();
		this.bench = new Benchmark();
		this.status = new Status();		
		this.space=new Space();
		setStages(new ArrayList<JoblineStage>());
		setBenchInputs(new ArrayList<Integer>());
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

	public void setJobSpaceId(int jobSpaceId) {
		this.jobSpaceId = jobSpaceId;
	}

	public int getJobSpaceId() {
		return jobSpaceId;
	}

	public void setJobSpaceName(String jobSpaceName) {
		this.jobSpaceName = jobSpaceName;
	}

	public String getJobSpaceName() {
		return jobSpaceName;
	}


	public List<JoblineStage> getStages() {
		return stages;
	}

	public void setStages(List<JoblineStage> stages) {
		this.stages = stages;
	}
	
	/**
	 * Adds a stage to the END of this job pairs stage list.
	 * @param stage
	 */
	public void addStage(JoblineStage stage) {
		this.stages.add(stage);
	}
	
	/**
	 * Arranges all the stages in this job pair in order of their stage_number.
	 * In other words, after calling this function, calling getStages().get(0)
	 * will return the stage with the lowest stage number, then the second lowest,
	 * and so on
	 */
	
	public void sortStages() {
		JoblineStageComparator comp= new JoblineStageComparator();
		Collections.sort(stages, comp);
	}
	
	/**
	 * Returns the primary stage of this job pair, as determined by the 
	 * primaryStageNumber field. If that field is not set, returns the first stage.
	 * If no stages are set, returns null
	 * @return
	 */
	public JoblineStage getPrimaryStage() {
		
		if (primaryStageNumber!=null) {
			return stages.get(primaryStageNumber-1);
		}
		
		// if the primary stage isn't set for some reason, we simply return the first stage.
		if (stages.size()>0){
			
			return stages.get(0);

		}
		
		
		// just return the first stage if there is none marked as primary
		return null;
		
	}
	
	/**
	 * Returns the configuration of the "priamry" stage of this jobline. Returns
	 * null  if there is no such stage.
	 * @return
	 */
	public Configuration getPrimaryConfiguration() {
		JoblineStage s= getPrimaryStage();
		if (s==null) {
			return null;
		}
		
		return s.getConfiguration();
	}
	
	/**
	 * Returns the solver of the "priamry" stage of this jobline. Returns
	 * null  if there is no such stage.
	 * @return
	 */
	public Solver getPrimarySolver() {
		JoblineStage s= getPrimaryStage();
		if (s==null) {
			return null;
		}
		
		return s.getSolver();
	}
	
	/**
	 * Returns the solver of the "priamry" stage of this jobline. Returns
	 * null  if there is no such stage.
	 * @return
	 */
	public Double getPrimaryCpuTime() {
		JoblineStage s= getPrimaryStage();
		if (s==null) {
			return null;
		}
		
		return s.getCpuTime();
	}
	
	/**
	 * Returns the solver of the "priamry" stage of this jobline. Returns
	 * null  if there is no such stage.
	 * @return
	 */
	public Double getPrimaryWallclockTime() {
		JoblineStage s= getPrimaryStage();
		if (s==null) {
			return null;
		}
		
		return s.getWallclockTime();
	}
	
	/**
	 * Returns the solver of the "priamry" stage of this jobline. Returns
	 * null  if there is no such stage.
	 * @return
	 */
	public Double getPrimaryMaxVirtualMemory() {
		JoblineStage s= getPrimaryStage();
		if (s==null) {
			return null;
		}
		
		return s.getMaxVirtualMemory();
	}
	
	/**
	 * Returns a string that uniquely identifies the stages of this jobline
	 * using the following format. A colon will terminate the string. The
	 * empty string is returned if there are no stages
	 * <stage1id>:<stage2id>:...
	 * @return
	 */
	public String getStageString() {
		StringBuilder sb=new StringBuilder();
		for (JoblineStage s : stages) {
			sb.append(s.getConfiguration().getId());
			sb.append(":");
		}
		return sb.toString();
	}

	
	public int getSandboxNum() {
		return sandboxNum;
	}

	public void setSandboxNum(int sandboxNum) {
		this.sandboxNum = sandboxNum;
	}

	

	public List<Integer> getBenchInputs() {
		return benchInputs;
	}

	public void setBenchInputs(List<Integer> benchInputs) {
		this.benchInputs = benchInputs;
	}
	public void addBenchInput(Integer input) {
		this.benchInputs.add(input);
	}

	public Integer getPrimaryStageNumber() {
		return primaryStageNumber;
	}

	public void setPrimaryStageNumber(Integer primaryStageNumber) {
		this.primaryStageNumber = primaryStageNumber;
	}
	/**
	 * Returns a stage based on the number. 
	 * If given <=0, returns the primary stage
	 * if given 1...n where there are n stages, returns the stage
	 * if given >n returns null;
	 * @param stageNumber
	 */
	public JoblineStage getStageFromNumber(int stageNumber) {
		if (stageNumber<=0) {
			return this.getPrimaryStage();
		} else if (stageNumber<=stages.size()) {
			return stages.get(stageNumber-1);
		}
		return null;
		
	}
	
	/**
	 * @return the starexec-result value from attributes list
	 */
	public String getPrimaryStarexecResult() {
		Properties prop = this.getPrimaryStage().getAttributes();
		return (prop != null && prop.containsKey(R.STAREXEC_RESULT) && prop.get(R.STAREXEC_RESULT)!=null) 
			? prop.getProperty(R.STAREXEC_RESULT) : "--";
	}
}