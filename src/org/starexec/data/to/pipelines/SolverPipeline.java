package org.starexec.data.to.pipelines;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.starexec.data.to.*;

/**
 * Class represents the top level of a solver pipeline
 * @author Eric
 *
 */

public class SolverPipeline extends Identifiable {
	private int userId;
	private String name;
	private List<PipelineStage> stages=null;
	private Timestamp uploadDate;	
	
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
	
}
