package org.starexec.data.to;

import org.starexec.constants.R;

public class SolverComparison {
	private JobPair pair1;
	private JobPair pair2;

	/**
	 * Both pairs are assumed to have only a single stage
	 *
	 * @param p1
	 * @param p2
	 * @throws Exception
	 */
	public SolverComparison(JobPair p1, JobPair p2) throws Exception {
		if (p1.getBench().getId() != p2.getBench().getId()) {
		}
		if (p1.getStages().size() != 1 || p2.getStages().size() != 1) {
			throw new Exception("both pairs in a comparision must have only a single populated stage");
		}

		pair1 = p1;
		pair2 = p2;
	}

	/**
	 * Returns pair2 wallclock time minus pair1 wallclock time
	 *
	 * @return
	 */
	public double getWallclockDifference(int stageNumber) {
		return pair2.getStageFromNumber(stageNumber).getWallclockTime() -
				pair1.getStageFromNumber(stageNumber).getWallclockTime();
	}

	public double getCpuDifference(int stageNumber) {
		return pair2.getStageFromNumber(stageNumber).getCpuTime() - pair1.getStageFromNumber(stageNumber).getCpuTime();
	}

	public Benchmark getBenchmark() {
		return pair1.getBench();
	}

	public JobPair getFirstPair() {
		return pair1;
	}

	public JobPair getSecondPair() {
		return pair2;
	}

	/**
	 * Returns true if the result of the first pair is the same as the result of the second pair
	 *
	 * @return
	 */
	@SuppressWarnings("StringEquality")
	public boolean doResultsMatch(int stageNumber) {
		String result1 = pair1.getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
		String result2 = pair2.getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
		return result1 == result2 || !(result1 == null || result2 == null) && result1.equals(result2);
	}
}
