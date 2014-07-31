package org.starexec.data.to;

import org.starexec.constants.R;

public class SolverComparison {
	private JobPair pair1;
	private JobPair pair2;
	public SolverComparison(JobPair p1, JobPair p2) throws Exception {
		if (p1.getBench().getId()!=p2.getBench().getId()) {
			throw new Exception("both job pairs in a comparison must have the same benchmark!");
		}
		
		pair1=p1;
		pair2=p2;
	}
	
	/**
	 * Returns pair2 wallclock time minus pair1 wallclock time
	 * @return
	 */
	public double getWallclockDifference() {
		return pair2.getWallclockTime()-pair1.getWallclockTime();
	}
	
	public double getCpuDifference() {
		return pair2.getCpuTime()-pair1.getCpuTime();
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
	 * @return
	 */
	
	public boolean doResultsMatch() {
		
		String result1=pair1.getAttributes().getProperty(R.STAREXEC_RESULT);
		String result2=pair2.getAttributes().getProperty(R.STAREXEC_RESULT);
		if (result1==null && result2==null) {
			return true;
		} else if (result1== null || result2==null) {
			return false;
		} else {
			return result1.equals(result2);

		}
		
	}
}
