package org.starexec.data.to.compare;

import java.util.Comparator;

import org.starexec.constants.R;
import org.starexec.data.to.JobPair;

public class JobPairComparator implements Comparator<JobPair> {
	private int column; //will specify which field we are using to sort the job pairs
	
	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param sortIndex the value to sort on
	 * 0 = bench name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 5 = starexec-result attr
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private int compareJobPairStrings(JobPair jp1, JobPair jp2) {
		try {
			String str1=null;
			String str2=null;
			if (column==3) {
				str1=jp1.getStatus().getStatus();
				str2=jp2.getStatus().getStatus();
			}
			else if (column==5) {
				str1=jp1.getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=jp2.getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (column==0) {
				str1=jp1.getBench().getName();
				str2=jp2.getBench().getName();
			} else if (column==2) {
				str1=jp1.getConfiguration().getName();
				str2=jp2.getConfiguration().getName();
			} else {
				str1=jp1.getSolver().getName();
				str2=jp2.getSolver().getName();
			}
			//if str1 lexicographically follows str2, put str2 first
			return str1.compareToIgnoreCase(str2);
			
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		
		return 0; // 
	}
	
	/**
	 * Compares the solver names of jp1 and jp2 
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @param ASC Whether sorting is to be done ASC or DESC
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */ 
	private int compareJobPairNums(JobPair jp1, JobPair jp2) {
		try {
			double db1=0;
			double db2=0;
			if (column==6) {
				db1=jp1.getId();
				db2=jp2.getId();
			} else if (column==7) {
				db1=jp1.getCompletionId();
				db2=jp2.getCompletionId();
			} else if (column==4) {
				db1=jp1.getWallclockTime();
				db2=jp2.getWallclockTime();
				
			} else  {
				db1=jp1.getCpuTime();
				db2=jp2.getCpuTime();
			}
			
			return Double.compare(db1, db2);
			
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		
		return 0;
	}
	
	
	/**
	 * @param c
	 * 0 = bench name
	 * 1 = solver name
	 * 2 = config name
	 * 3 = status name
	 * 4 = wallclock time
	 * 5 = starexec-result attr
	 * 6 pair id
	 * 7 completion id
	 * 8 is cpu time
	 */
	public JobPairComparator(int c) {
		column=c;
	}
	
	@Override
	public int compare(JobPair o1, JobPair o2) {
		if (column!=4 &&column!=6 &&column!=7) {
			return compareJobPairStrings(o1,o2);
		} else {
			return compareJobPairNums(o1,o2);
		}
	}


}
