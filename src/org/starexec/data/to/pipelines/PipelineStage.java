package org.starexec.data.to.pipelines;

import java.util.ArrayList;
import java.util.List;

import org.starexec.data.to.*;

public class PipelineStage extends Identifiable {

	private int pipelineId;
	private int configId;
	private boolean keepOutput;
	private List<PipelineDependency> dependencies;
	
	public PipelineStage() {
		setDependencies(new ArrayList<PipelineDependency>());
		keepOutput=false;
	}
	
	public int getPipelineId() {
		return pipelineId;
	}
	public void setPipelineId(int pipelineId) {
		this.pipelineId = pipelineId;
	}
	public int getConfigId() {
		return configId;
	}
	public void setConfigId(int configId) {
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

	public boolean doKeepOutput() {
		return keepOutput;
	}

	public void setKeepOutput(boolean keepOutput) {
		this.keepOutput = keepOutput;
	}
}
