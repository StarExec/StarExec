package org.starexec.data.to.pipelines;

/**
 * Very simple class that simply holds a triple of a pair ID, a stage ID, and a processor ID
 * @author Eric
 *
 */

public class PairStageProcessorTriple {

	private int pairId;
	private int stageId;
	private int processorId;
	public int getPairId() {
		return pairId;
	}
	public void setPairId(int pairId) {
		this.pairId = pairId;
	}
	public int getStageId() {
		return stageId;
	}
	public void setStageId(int stageId) {
		this.stageId = stageId;
	}
	public int getProcessorId() {
		return processorId;
	}
	public void setProcessorId(int processorId) {
		this.processorId = processorId;
	}
}
