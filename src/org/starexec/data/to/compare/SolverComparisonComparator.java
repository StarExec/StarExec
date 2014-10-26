package org.starexec.data.to.compare;
import java.util.Comparator;

import org.starexec.constants.R;
import org.starexec.data.to.*;
public class SolverComparisonComparator implements Comparator<SolverComparison> {
	private int column; //will specify which field we are using to sort the job pairs
	private boolean isWallclock;
	public SolverComparisonComparator(int c, boolean w) {
		column=c;
		isWallclock=w;
	}
	
	
	/**
	 * 
	 * @param jp1
	 * @param jp2
	 * @param sortIndex
	 * 1 pair 1 time
	 * 2 pair 2 time
	 * 3 time diff
	 * 6 same-result column
	 * @param ASC
	 * @param isWallclock
	 * @return
	 */
	private  int compareSolverComparisonNums(SolverComparison c1, SolverComparison c2) {
		int answer=0;
		try {
			double db1=0;
			double db2=0;
			if (column==1) {
				if (isWallclock) {
					db1=c1.getFirstPair().getWallclockTime();
					db2=c2.getFirstPair().getWallclockTime();
				} else {
					db1=c1.getFirstPair().getCpuTime();
					db2=c2.getFirstPair().getCpuTime();
				}
			} else if (column==2) {
				if (isWallclock) {
					db1=c1.getSecondPair().getWallclockTime();
					db2=c2.getSecondPair().getWallclockTime();
				} else {
					db1=c1.getSecondPair().getCpuTime();
					db2=c2.getSecondPair().getCpuTime();
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
			
			
			
			//if db1> db2, then db2 should go first
			if (db1>db2) {
				return 1;
			}
			if (db1==db2) {
				return 0;
			}
			return -1;
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
		int answer=0;
		try {
			String str1=null;
			String str2=null;
			 if (column==5) {
				str1=c1.getSecondPair().getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getSecondPair().getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (column==0) {
				str1=c1.getBenchmark().getName();
				str2=c2.getBenchmark().getName();
			} else {
				str1=c1.getFirstPair().getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=c2.getFirstPair().getAttributes().getProperty(R.STAREXEC_RESULT);
			}
			//if str1 lexicographically follows str2, put str2 first
			if (str1.compareTo(str2)>0) {
				answer=1;
			}
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		return 0;
	}
	
	
	
	@Override
	public int compare(SolverComparison o1, SolverComparison o2) {
		if (column==0 || column==4 || column==5) {
			return compareSolverComparisonStrings(o1,o2);
		} else {
			return compareSolverComparisonNums(o1,o2);
		}
	}
	
}
