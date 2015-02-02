package org.starexec.data.to.pipelines;

import org.starexec.data.to.Solver.ExecutableType;

public class PipelineDependency {
	
	
	/**
	 * Represents the type of the processor (along with it's SQL storage values)
	 */
	public static enum PipelineDependencyType {
		
		BENCHMARK(1), 
		ARTIFACT(2);  //type for the output from a previous stage
		
		
		private int val;
		
		private PipelineDependencyType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static PipelineDependencyType valueOf(int val) {
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
	
	
	
	private int stageId;
	private int dependencyId;
	private PipelineDependencyType type;
	
	
	
	
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
	public PipelineDependencyType getType() {
		return type;
	}
	public void setType(PipelineDependencyType type) {
		this.type = type;
	}
	
}
	

	
	
