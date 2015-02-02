package org.starexec.data.to.pipelines;

import java.util.ArrayList;
import java.util.List;

import org.starexec.data.to.*;

public class PipelineStage extends Identifiable {

	private int pipelineId;
	private int executableId;
	private List<PipelineDependency> dependencies;
	
	public PipelineStage() {
		setDependencies(new ArrayList<PipelineDependency>());
	}
	
	public int getPipelineId() {
		return pipelineId;
	}
	public void setPipelineId(int pipelineId) {
		this.pipelineId = pipelineId;
	}
	public int getExecutableId() {
		return executableId;
	}
	public void setExecutableId(int executableId) {
		this.executableId = executableId;
	}

	public List<PipelineDependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<PipelineDependency> dependencies) {
		this.dependencies = dependencies;
	}
}
