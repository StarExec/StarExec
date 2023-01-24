package org.starexec.data.to.pipelines;

import org.starexec.constants.R;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status;
import org.starexec.logger.StarLogger;

import java.util.List;
import java.util.Properties;

/**
 * This class represents the set of results for a single stage of a jobline
 *
 * @author Eric
 */
public class JoblineStage {
	private static final StarLogger log = StarLogger.getLogger(JoblineStage.class);

	private Solver solver = null;
	private Integer stageId = null; // This is the ID of the PipelineStage that this JoblineStage refers to
	private Integer jobpairId = null;
	private double wallclockTime;
	private double cpuTime;
	private double userTime;
	private double systemTime;
	private Status status = null;
	private double maxVirtualMemory;
	private double maxResidenceSetSize;
	private Configuration configuration = null;
	private boolean noOp = false;
	private Properties attributes = null;
	private Integer stageNumber = null; //which stage is this? 1,2... etc.

	private List<PipelineDependency> dependencies;

	// the standard output of this pair. This is only populated for a short time in pair.jsp
	private String output = null;

	public JoblineStage() {
		this.setSolver(new Solver());
		this.setConfiguration(new Configuration());
		status = new Status();
		this.attributes = new Properties();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;

		// print status
		log.debug( "in JoblineStage.setConfiguration(): current configId = " + configuration.getId() +
				"; current deleted status = " + configuration.getDeleted() );
	}

	public double getMaxResidenceSetSize() {
		return maxResidenceSetSize;
	}

	public void setMaxResidenceSetSize(double maxResidenceSetSize) {
		this.maxResidenceSetSize = maxResidenceSetSize;
	}

	public double getMaxVirtualMemory() {
		return maxVirtualMemory;
	}

	public void setMaxVirtualMemory(double maxVirtualMemory) {
		this.maxVirtualMemory = maxVirtualMemory;
	}

	public double getSystemTime() {
		return systemTime;
	}

	public void setSystemTime(double systemTime) {
		this.systemTime = systemTime;
	}

	public double getUserTime() {
		return userTime;
	}

	public void setUserTime(double userTime) {
		this.userTime = userTime;
	}

	public double getCpuTime() {
		return cpuTime;
	}

	public void setCpuUsage(double cpuTime) {
		this.cpuTime = cpuTime;
	}

	public double getWallclockTime() {
		return wallclockTime;
	}

	public void setWallclockTime(double wallclockTime) {
		this.wallclockTime = wallclockTime;
	}

	public Solver getSolver() {
		return solver;
	}

	public void setSolver(Solver solver) {
		this.solver = solver;
	}

	public Integer getStageId() {
		return stageId;
	}

	public void setStageId(Integer stageId) {
		this.stageId = stageId;
	}

	public Integer getJobpairId() {
		return jobpairId;
	}

	public void setJobpairId(Integer jobpairId) {
		this.jobpairId = jobpairId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public boolean isNoOp() {
		return noOp;
	}

	public void setNoOp(boolean noOp) {
		this.noOp = noOp;
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
	 * @return the starexec-result value from attributes list
	 */
	public String getStarexecResult() {
		Properties prop = this.getAttributes();
		return (prop != null && prop.containsKey(R.STAREXEC_RESULT) && prop.get(R.STAREXEC_RESULT) != null) ?
		       prop.getProperty(R.STAREXEC_RESULT) : "--";
	}

	public Integer getStageNumber() {
		return stageNumber;
	}

	public void setStageNumber(Integer stageNumber) {
		this.stageNumber = stageNumber;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public List<PipelineDependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<PipelineDependency> dependencies) {
		this.dependencies = dependencies;
	}
}
