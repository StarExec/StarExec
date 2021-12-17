package org.starexec.data.to;

import com.google.gson.annotations.Expose;
import org.jfree.util.Log;
import org.starexec.constants.R;
import org.starexec.data.database.Jobs;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.logger.StarLogger;
import org.starexec.util.Util;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Represents a job in the database
 *
 * @author Tyler Jensen
 */
public class Job extends Identifiable implements Iterable<JobPair>, Nameable {
	private static final StarLogger log = StarLogger.getLogger(Jobs.class);

	private int userId = -1;
	private User user = null; // this is populated for the JobManager
	@Expose private String name;
	@Expose private String description = "no description";
	private Queue queue = null;
	private long seed = 0;

	private int cpuTimeout = -1;
	private int wallclockTimeout = -1;
	private int killDelay = 0;
	private int softTimeLimit = 0;
	private long maxMemory;        //maximum memory the pair can use, in bytes
	private BenchmarkingFramework benchmarkingFramework;

	@Expose private Timestamp createTime;
	@Expose private Timestamp completeTime;
	// this is the root JOB SPACE for this job. It is NOT a space from the spaces table.
	//Exception: Before a job is created, this field is used to store the space the job was created in
	@Expose private int primarySpace;
	private List<JobPair> jobPairs;
	private HashMap<String, Integer> liteJobPairStats;

	private boolean deleted;
			// if true, this job has been deleted on disk and exists only in the database so we can see space
			// associations
	private boolean paused; // if true, this job is currently paused
	private Boolean readOnly;

	private boolean buildJob;
	//a list of all the stage attributes for this job, in no particular order
	private List<StageAttributes> stageAttributes;

	// Whether to suppress the timestamp produced by runsolver for this job.
	private boolean suppressTimestamp;

	private boolean usingDependencies = false;
	private boolean isHighPriority = false;

	private int totalPairs; // number of pairs this job owns
	private long diskSize; // in bytes
	private String outputBenchmarksPath;

	public Job() {
		jobPairs = new LinkedList<>();

		queue = new Queue();
		setStageAttributes(new ArrayList<>());
		setSuppressTimestamp(false); // false is default
		setBuildJob(false); //false is default
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
	 * @return The root job space for this job
	 */
	public int getPrimarySpace() {
		return primarySpace;
	}

	public boolean isHighPriority() {
		return isHighPriority;
	}

	public void setHighPriority() {
		isHighPriority = true;
	}

	public void setLowPriority() {
		isHighPriority = false;
	}

	public BenchmarkingFramework getBenchmarkingFramework() {
		return benchmarkingFramework;
	}

	public void setBenchmarkingFramework(BenchmarkingFramework benchmarkingFramework) {
		this.benchmarkingFramework = benchmarkingFramework;
	}

	public String getOutputBenchmarksPath() {
		return outputBenchmarksPath;
	}

	public void setOutputBenchmarksPath(String path) {
		outputBenchmarksPath = path;
	}

	/**
	 * Sets the root job space for this job
	 *
	 * @param space The ID of the space
	 */

	public void setPrimarySpace(int space) {
		this.primarySpace = space;
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
		if (!Util.isNullOrEmpty(description)) {
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
	 * @return all the attribute names for every completed job pair in this job
	 */
	public Set<String> attributeNames() {
		if (jobPairs == null || jobPairs.isEmpty()) {
			return null;
		}
		Set<String> attrs = new HashSet<>();
		for (JobPair pair : jobPairs) {
			for (JoblineStage stage : pair.getStages()) {
				Properties props = stage.getAttributes();

				if (pair.getStatus().getCode() == StatusCode.STATUS_COMPLETE) {
					attrs.addAll(props.stringPropertyNames());
				}
			}
		}
		Log.debug("Returning " + attrs.size() + " unique attr names");
		return attrs;
	}

	public void addJobPairs(Collection<JobPair> pairs) {
		jobPairs.addAll(pairs);
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

	@Override
	public Iterator<JobPair> iterator() {
		return this.jobPairs.iterator();
	}

	/**
	 * @param ljps the job pair statistics to store in this object
	 */
	public void setLiteJobPairStats(HashMap<String, Integer> ljps) {
		this.liteJobPairStats = ljps;
	}

	/**
	 * @return the job pair statistics stored in this object
	 */
	public HashMap<String, Integer> getLiteJobPairStats() {
		return liteJobPairStats;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public boolean isReadOnly() {
		if (readOnly == null) readOnly = Jobs.isReadOnly(this.getId());
		log.info("isReadOnly", "Job: "+this.getId()+"     isReadOnly: "+readOnly);
		return readOnly;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public boolean isPaused() {
		return paused;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public long getSeed() {
		return seed;
	}

	/**
	 * @param completeTime the completeTime to set
	 */
	public void setCompleteTime(Timestamp completeTime) {
		this.completeTime = completeTime;
	}

	/**
	 * @return the completeTime
	 */
	public Timestamp getCompleteTime() {
		return completeTime;
	}

	public int getCpuTimeout() {
		return cpuTimeout;
	}

	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}

	public int getWallclockTimeout() {
		return wallclockTimeout;
	}

	public void setWallclockTimeout(int wallclockTimeout) {
		this.wallclockTimeout = wallclockTimeout;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}

	public List<StageAttributes> getStageAttributes() {
		return stageAttributes;
	}

	public void setStageAttributes(List<StageAttributes> stageAttributes) {
		this.stageAttributes = stageAttributes;
	}

	public void addStageAttributes(StageAttributes attrs) {
		this.stageAttributes.add(attrs);
	}

	public boolean containsStageOneAttributes() {
		for (StageAttributes attrs : this.stageAttributes) {
			if (attrs.getStageNumber() == 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the StageAttributes object for the given stage number. If there is no such object, generates one using
	 * the defaults from this job object.
	 *
	 * @param stageNumber
	 * @return
	 */
	public StageAttributes getStageAttributesByStageNumber(int stageNumber) {
		for (StageAttributes attrs : stageAttributes) {
			if (attrs.getStageNumber() == stageNumber) {
				return attrs;
			}
		}

		StageAttributes attrs = new StageAttributes();
		attrs.setStageNumber(stageNumber);
		attrs.setCpuTimeout(cpuTimeout);
		attrs.setWallclockTimeout(wallclockTimeout);
		attrs.setJobId(this.getId());
		attrs.setMaxMemory(maxMemory);
		return attrs;
	}

	/**
	 * Sets the suppress timestamps boolean for this job.
	 *
	 * @param suppressTimestamp whether to suppress timestamps produced by runsolver for this job.
	 * @author Albert Giegerich
	 */
	public void setSuppressTimestamp(boolean suppressTimestamp) {
		this.suppressTimestamp = suppressTimestamp;
	}

	/**
	 * Gets whether or not the timestamp is suppressed for this job.
	 *
	 * @return Whether or not the timestamp is suppressed for this job.
	 * @author Albert Giegerich
	 */
	public boolean timestampIsSuppressed() {
		return suppressTimestamp;
	}

	public boolean isUsingDependencies() {
		return usingDependencies;
	}

	public void setUsingDependencies(boolean usingDependencies) {
		this.usingDependencies = usingDependencies;
	}

	/**
	 * Gets whether or not this is a buildjob
	 *
	 * @return Whether or not the job is a build job
	 * @author Andrew Lubinus
	 */

	public boolean isBuildJob() {
		return buildJob;
	}

	/**
	 * Sets whether or not this is a build job
	 *
	 * @param buildJob boolean representing if this a build job or not
	 * @author Andrew Lubinus
	 */

	public void setBuildJob(boolean buildJob) {
		this.buildJob = buildJob;
	}

	/**
	 * Gets the name of the root space for this job. Doing this requires that at least one job pair is populated and
	 * that it has the correct path info set.
	 *
	 * @return the root space name, or null if it cannot be found
	 */
	public String getRootSpaceName() {
		if (getJobPairs().isEmpty()) {
			return null;
		}
		String rootName = getJobPairs().get(0).getPath();
		if (rootName.contains(R.JOB_PAIR_PATH_DELIMITER)) {
			rootName = rootName.substring(0, rootName.indexOf(R.JOB_PAIR_PATH_DELIMITER));
		}
		return rootName;
	}

	/**
	 * @return the totalPairs
	 */
	public int getTotalPairs() {
		return totalPairs;
	}

	/**
	 * @param totalPairs the totalPairs to set
	 */
	public void setTotalPairs(int totalPairs) {
		this.totalPairs = totalPairs;
	}

	/**
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * @return the diskSize
	 */
	public long getDiskSize() {
		return diskSize;
	}

	/**
	 * @param diskSize the diskSize to set
	 */
	public void setDiskSize(long diskSize) {
		this.diskSize = diskSize;
	}

	/**
	 * @return Human readable description of this job's status
	 */
	public String getStatus() {
		final int id = getId();
		try {
			return Jobs.getJobStatus(id).toString();
		} catch (SQLException e) {
			return "";
		}
	}

	public void setKillDelay(int killDelay) {
		this.killDelay = killDelay;
	}

	/**
	 * If non-zero, process will receive SIGTERM, and then SIGKILL after the delay specified. Note that this option
	 * only
	 * applies to RunSolver
	 *
	 * @return delay in seconds
	 */
	public int getKillDelay() {
		return killDelay;
	}

	public void setSoftTimeLimit(int softTimeLimit) {
		this.softTimeLimit = softTimeLimit;
	}

	/**
	 * If non-zero, SIGTERM will be sent this many seconds after Job starts. SIGKILL will be sent after other timeouts.
	 *
	 * @return softTimeLimit in seconds
	 */
	public int getSoftTimeLimit() {
		return softTimeLimit;
	}
}
