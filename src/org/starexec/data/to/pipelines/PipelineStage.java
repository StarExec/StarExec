package org.starexec.data.to.pipelines;

import org.starexec.data.to.Identifiable;

import java.util.ArrayList;
import java.util.List;

public class PipelineStage extends Identifiable {

	private int pipelineId;
	private Integer configId;
	private boolean noOp;
	//this field is not stored in the database-- it is transiently used during a job XML upload
	private boolean isPrimary;
	private List<PipelineDependency> dependencies;
	
	public PipelineStage() {
		setDependencies(new ArrayList<>());
	}
	
	public int getPipelineId() {
		return pipelineId;
	}
	public void setPipelineId(int pipelineId) {
		this.pipelineId = pipelineId;
	}
	public Integer getConfigId() {
		return configId;
	}
	public void setConfigId(Integer configId) {
		this.configId = configId;
	}

	public List<PipelineDependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<PipelineDependency> dependencies) {
		this.dependencies = dependencies;
	}
	public void addDependency(PipelineDependency dep) {
		this.dependencies.add(dep);
	}

	

	

	public boolean isPrimary() {
		return isPrimary;
	}

	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}

	public boolean isNoOp() {
		return noOp;
	}

	public void setNoOp(boolean noOp) {
		this.noOp = noOp;
	}
}
