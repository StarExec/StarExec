package org.starexec.data.to.pipelines;

import org.starexec.data.to.Processor;

/**
 * This class wraps the data we store in the job_stage_params SQL table
 * @author Eric
 *
 */

public class StageAttributes {
	
	/**
	 * Enum describing options for what to do with job pair output.
	 * Applies to both stdout and 
	 */
	public static enum SaveResultsOption {
		NO_SAVE(1),       // do not write output back in any way
		SAVE(2),          // save output as pair results (default, used by all non-pipeline jobs)
		CREATE_BENCH(3);  // both save results as output and also create benchmark from output
		
		
		private int val;
		
		private SaveResultsOption(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static SaveResultsOption valueOf(int val) {
			switch(val) {			
				case 1:
					return NO_SAVE;
				case 2:
					return SAVE;
				case 3:
					return CREATE_BENCH;
				default:
					return null;				
			}
		}
		
		/**
		 * Returns a SaveResultsOption given one of the strings defined
		 * in batchJobSchema.xsd
		 * @param val
		 * @return
		 */
		public static SaveResultsOption stringToOption(String val) {
			switch(val) {
			case "NoSave":
				return NO_SAVE;
			case "Save":
				return SAVE;
			case "CreateBench":
				return CREATE_BENCH;
			default:
				return null;
			}
		}
	}
	
	
	private int jobId;
	private int stageNumber;
	private int wallclockTimeout;
	private int cpuTimeout;
	private long maxMemory;
	private int resultsInterval;
	private Integer spaceId; // null if not given. Not required
	private Processor preProcessor;
	private Processor postProcessor;
	private String benchSuffix=null;
	private SaveResultsOption stdoutSaveOption;
	private SaveResultsOption extraOutputSaveOption;
	public StageAttributes() {
		jobId=-1;
		stageNumber=-1;
		wallclockTimeout=-1;
		cpuTimeout=-1;
		maxMemory=-1;
		spaceId=null;
		preProcessor=null;
		postProcessor=null;
		setResultsInterval(0);
		setStdoutSaveOption(SaveResultsOption.SAVE);
		setExtraOutputSaveOption(SaveResultsOption.SAVE);
	}
	
	public int getJobId() {
		return jobId;
	}
	public void setJobId(int jobId) {
		this.jobId = jobId;
	}
	public int getStageNumber() {
		return stageNumber;
	}
	public void setStageNumber(int stageId) {
		this.stageNumber = stageId;
	}
	public int getWallclockTimeout() {
		return wallclockTimeout;
	}
	public void setWallclockTimeout(int wallclockTimeout) {
		this.wallclockTimeout = wallclockTimeout;
	}
	public int getCpuTimeout() {
		return cpuTimeout;
	}
	public void setCpuTimeout(int cpuTimeout) {
		this.cpuTimeout = cpuTimeout;
	}
	public long getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	public Integer getSpaceId() {
		return spaceId;
	}
	public void setSpaceId(Integer spaceId) {
		this.spaceId = spaceId;
	}

	public Processor getPreProcessor() {
		return preProcessor;
	}

	public void setPreProcessor(Processor preProcessor) {
		this.preProcessor = preProcessor;
	}

	public Processor getPostProcessor() {
		return postProcessor;
	}

	public void setPostProcessor(Processor postProcessor) {
		this.postProcessor = postProcessor;
	}

	public String getBenchSuffix() {
		return benchSuffix;
	}

	public void setBenchSuffix(String benchSuffix) {
		this.benchSuffix = benchSuffix;
	}

	public int getResultsInterval() {
		return resultsInterval;
	}

	public void setResultsInterval(int resultsInterval) {
		this.resultsInterval = resultsInterval;
	}

	public SaveResultsOption getStdoutSaveOption() {
		return stdoutSaveOption;
	}

	public void setStdoutSaveOption(SaveResultsOption stdoutSaveOption) {
		this.stdoutSaveOption = stdoutSaveOption;
	}

	public SaveResultsOption getExtraOutputSaveOption() {
		return extraOutputSaveOption;
	}

	public void setExtraOutputSaveOption(SaveResultsOption extraOutputSaveOption) {
		this.extraOutputSaveOption = extraOutputSaveOption;
	}
}
