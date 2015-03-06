package org.starexec.data.to.compare;
import java.util.Comparator;

import org.starexec.constants.R;
import org.starexec.data.to.*;
public class SolverComparisonComparator implements Comparator<SolverComparison> {
	private int column; //will specify which field we are using to sort the job pairs
	private boolean asc;
	private boolean isWallclock;
	
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
	
	public SolverComparisonComparator(int c, boolean w, boolean a) {
		column=c;
		isWallclock=w;
		asc=a;
	}
	
	
	private  int compareSolverComparisonNums(SolverComparison c1, SolverComparison c2) {
		try {
			double db1=0;
			double db2=0;
			if (column==1) {
				if (isWallclock) {
					db1=c1.getFirstPair().getPrimaryWallclockTime();
					db2=c2.getFirstPair().getPrimaryWallclockTime();
				} else {
					db1=c1.getFirstPair().getPrimaryCpuTime();
					db2=c2.getFirstPair().getPrimaryCpuTime();
				}
			} else if (column==2) {
				if (isWallclock) {
					db1=c1.getSecondPair().getPrimaryWallclockTime();
					db2=c2.getSecondPair().getPrimaryWallclockTime();
				} else {
					db1=c1.getSecondPair().getPrimaryCpuTime();
					db2=c2.getSecondPair().getPrimaryCpuTime();
				}
			} else if (column==3) {
				if (isWallclock) {
					db1=c1.getWallclockDifference();
					db2=c2.getWallclockDifference();
				} else {
					db1=c1.getCpuDifference();
					db2=c2.getCpuDifference();
				}
			} else {
				if (c1.doResultsMatch()) {
					db1=1;
				}
				if (c2.doResultsMatch()) {
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
				str1=c1.getSecondPair().getStages().get(0).getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getSecondPair().getStages().get(0).getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (column==0) {
				str1=c1.getBenchmark().getName();
				str2=c2.getBenchmark().getName();
			} else {
				str1=c1.getFirstPair().getStages().get(0).getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getFirstPair().getStages().get(0).getAttributes().getProperty(R.STAREXEC_RESULT);
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
