package org.starexec.data.to.compare;

import org.starexec.constants.R;
import org.starexec.data.to.SolverComparison;
import org.starexec.data.to.pipelines.JoblineStage;

import java.util.Comparator;
public class SolverComparisonComparator implements Comparator<SolverComparison> {
	private int column; //will specify which field we are using to sort the job pairs
	private boolean asc;
	private boolean isWallclock;
	private int stageNumber;
	/**
	 * Creates a new object that will compare SolverComparisons on different fields based on the given 
	 * parameters
	 * 
	 * 0 Benchmark Name
	 * 1 First pair wallclock time OR CPU time, depending on whether  isWallclock is true
	 * 2 Second pair wallclock time OR CPU time, depending on whether  isWallclock is true
	 * 3 Differences between wallclock OR CPU times, depending on whether isWallclock is true
	 * 4 Starexec-result of first pair
	 * 5 Starexec-result of second pair
	 * 6 Results-match column
	 */
	
	public SolverComparisonComparator(int c, boolean w, boolean a, int s) {
		column=c;
		isWallclock=w;
		asc=a;
		stageNumber=s;
	}
	
	
	private  int compareSolverComparisonNums(SolverComparison c1, SolverComparison c2) {
		try {
			double db1=0;
			double db2=0;
			JoblineStage stage11=c1.getFirstPair().getStageFromNumber(stageNumber);
			JoblineStage stage12=c2.getFirstPair().getStageFromNumber(stageNumber);

			
			JoblineStage stage21=c1.getSecondPair().getStageFromNumber(stageNumber);
			JoblineStage stage22=c2.getSecondPair().getStageFromNumber(stageNumber);
			
			if (column==1) {
				if (isWallclock) {
					db1=stage11.getWallclockTime();
					db2=stage12.getWallclockTime();
				} else {
					db1=stage11.getCpuTime();
					db2=stage12.getCpuTime();
				}
			} else if (column==2) {
				if (isWallclock) {
					db1=stage21.getWallclockTime();
					db2=stage22.getWallclockTime();
				} else {
					db1=stage21.getCpuTime();
					db2=stage22.getCpuTime();
				}
			} else if (column==3) {
				if (isWallclock) {
					db1=c1.getWallclockDifference(stageNumber);
					db2=c2.getWallclockDifference(stageNumber);
				} else {
					db1=c1.getCpuDifference(stageNumber);
					db2=c2.getCpuDifference(stageNumber);
				}
			} else {
				if (c1.doResultsMatch(stageNumber)) {
					db1=1;
				}
				if (c2.doResultsMatch(stageNumber)) {
					db2=1;
				}
			}
			
			return Double.compare(db1, db2);
			
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		
		return 0;
	}
	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param sortIndex the value to sort on
	 * 0 = bench name
	 * 4 = result 1
	 * 5 = result 2
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private int compareSolverComparisonStrings(SolverComparison c1, SolverComparison c2) {
		try {
			String str1=null;
			String str2=null;
			
			
			
			
			 if (column==5) {
				str1=c1.getSecondPair().getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getSecondPair().getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (column==0) {
				str1=c1.getBenchmark().getName();
				str2=c2.getBenchmark().getName();
			} else {
				str1=c1.getFirstPair().getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getFirstPair().getStageFromNumber(stageNumber).getAttributes().getProperty(R.STAREXEC_RESULT);
			}
			//if str1 lexicographically follows str2, put str2 first
			return str1.compareToIgnoreCase(str2);
				
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		return 0;
	}
	
	
	/**
	 * 0 Benchmark Name
	 * 1 First pair wallclock time OR CPU time, depending on whether  isWallclock is true
	 * 2 Second pair wallclock time OR CPU time, depending on whether  isWallclock is true
	 * 3 Differences between wallclock OR CPU times, depending on whether isWallclock is true
	 * 4 Starexec-result of first pair
	 * 5 Starexec-result of second pair
	 * 6 Results-match column
	 */
	
	@Override
	public int compare(SolverComparison o1, SolverComparison o2) {
		if (!asc) {
			SolverComparison temp=o1;
			o1=o2;
			o2=temp;
		}
		if (column==0 || column==4 || column==5) {
			return compareSolverComparisonStrings(o1,o2);
		} else {
			return compareSolverComparisonNums(o1,o2);
		}
	}
	
}
