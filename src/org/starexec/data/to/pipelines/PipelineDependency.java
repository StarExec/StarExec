package org.starexec.data.to.pipelines;

/**
 * Class represents a dependency for some solver pipeline. Dependencies
 * can be either on benchmarks or on the output of a previous stage
 * @author Eric
 *
 */
public class PipelineDependency {
	
	/**
	 * Type of pipeline dependency is either a benchmark or an 'artifact,' the
	 * output of some previous stage.
	 */
	public static enum PipelineInputType {
		
		BENCHMARK(1), 
		ARTIFACT(2);  //type for the output from a previous stage
		
		
		private int val;
		
		private PipelineInputType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static PipelineInputType valueOf(int val) {
			switch(val) {			
				case 1:
					return BENCHMARK;
				case 2:
					return ARTIFACT;
				
				default:
					return null;				
			}
		}
	}
	
	
	
	private int stageId; // the stage this dependency is for
	private int dependencyId; // either a 1-indexed number of a stage in a pipeline if the type is "artifact" 
						 	  // or a 1-indexed number indicating which of the n benchmarks a user has uploaded
	private PipelineInputType type; // whether this is a benchmark or artifact
	private int inputNumber; // whether this is the first, second... etc. input to the stage
	
	
	
	
	public int getStageId() {
		return stageId;
	}
	public void setStageId(int stage_id) {
		this.stageId = stage_id;
	}
	public int getDependencyId() {
		return dependencyId;
	}
	public void setDependencyId(int dependency_id) {
		this.dependencyId = dependency_id;
	}
	public PipelineInputType getType() {
		return type;
	}
	public void setType(PipelineInputType type) {
		this.type = type;
	}
	public int getInputNumber() {
		return inputNumber;
	}
	public void setInputNumber(int inputNumber) {
		this.inputNumber = inputNumber;
	}
	
}
	

	
	
