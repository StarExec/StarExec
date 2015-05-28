package org.starexec.data.to.pipelines;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.starexec.data.to.*;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;

/**
 * Class represents the top level of a solver pipeline
 * @author Eric
 *
 */

public class SolverPipeline extends Identifiable implements Nameable {
	private int userId;
	private String name;
	private List<PipelineStage> stages=null;
	private Timestamp uploadDate;	
	private int primaryStageId; //what is the id of the primary stage? Before addition to the database, stores primary stage NUMBER
	public SolverPipeline() {
		stages=new ArrayList<PipelineStage>();
	}
	
	public int getUserId() {
		return userId;
	}
	public void setUserId(int id) {
		this.userId = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Timestamp getUploadDate() {
		return uploadDate;
	}
	public void setUploadDate(Timestamp uploadDate) {
		this.uploadDate = uploadDate;
	}
	public List<PipelineStage> getStages() {
		return stages;
	}
	public void setStages(List<PipelineStage> stages) {
		this.stages = stages;
	}
	
	public void addStage(PipelineStage stage) {
		this.stages.add(stage);
	}
	
	/**
	 * Returns the number of benchmark inputs expected by this pipeline. 
	 * This method requires that all stages and dependencies are populated to work properly.
	 * @return
	 */
	public int getRequiredNumberOfInputs() {
		int inputs=0;
		for (PipelineStage stage : stages) {
			for (PipelineDependency dep : stage.getDependencies()) {
				if (dep.getType()==PipelineInputType.BENCHMARK)  {
					inputs=Math.max(inputs, dep.getDependencyId());
				}
			}
		}
		
		return inputs;
	}

	public int getPrimaryStageNumber() {
		return primaryStageId;
	}

	public void setPrimaryStageNumber(int primaryStageId) {
		this.primaryStageId = primaryStageId;
	}
	
}
