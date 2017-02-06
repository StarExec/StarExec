package org.starexec.data.to.compare;

import java.util.Comparator;

import org.starexec.constants.R;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.logger.StarLogger;

public class JobPairComparator implements Comparator<JobPair> {
	protected static final StarLogger log = StarLogger.getLogger(JobPairComparator.class);

	private int column; //will specify which field we are using to sort the job pairs
	private int stageNumber;
	private boolean asc;
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
		JoblineStage stage1=jp1.getStageFromNumber(stageNumber);
		JoblineStage stage2=jp2.getStageFromNumber(stageNumber);
		try {
			String str1=null;
			String str2=null;
			if (column==3) {
				str1=stage1.getStatus().getStatus();
				str2=stage2.getStatus().getStatus();
			}
			else if (column==5) {
				str1=stage1.getAttributes().getProperty(R.STAREXEC_RESULT);
				str2=stage2.getAttributes().getProperty(R.STAREXEC_RESULT);
			} else if (column==0) {
				str1=jp1.getBench().getName();
				str2=jp2.getBench().getName();
			} else if (column==2) {
				str1=stage1.getConfiguration().getName();
				str2=stage2.getConfiguration().getName();
			} else {
				str1=stage1.getSolver().getName();
				str2=stage2.getSolver().getName();
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
			JoblineStage stage1=jp1.getStageFromNumber(stageNumber);
			JoblineStage stage2=jp2.getStageFromNumber(stageNumber);
			double db1=0;
			double db2=0;
			if (column==6) {
				db1=jp1.getId();
				db2=jp2.getId();
			} else if (column==7) {
				db1=jp1.getCompletionId();
				db2=jp2.getCompletionId();
			} else if (column==4) {
				db1=stage1.getWallclockTime();
				db2=stage2.getWallclockTime();
				
			} else  {
				db1=stage1.getCpuTime();
				db2=stage2.getCpuTime();			

			}
			
			return Double.compare(db1, db2);
			
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}
		
		return 0;
	}
	
	
	/**
	 * @param c Which column to compare on
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
	public JobPairComparator(int c, int stage, boolean a) {
		column=c;
		stageNumber=stage;
		asc=a;
	}
	
	@Override
	public int compare(JobPair o1, JobPair o2) {
		if (!asc) {
			JobPair temp=o1;
			o1=o2;
			o2=temp;
		}
		if (column!=4 &&column!=6 &&column!=7 && column!=8) {
			return compareJobPairStrings(o1,o2);
		} else {
			return compareJobPairNums(o1,o2);
		}
	}


}
