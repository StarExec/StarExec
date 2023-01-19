package org.starexec.data.to.compare;

import org.starexec.constants.R;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.logger.StarLogger;

import java.util.Comparator;

public class JobPairComparator implements Comparator<JobPair> {
	protected static final StarLogger log = StarLogger.getLogger(JobPairComparator.class);

	private final int column; //will specify which field we are using to sort the job pairs
	private final int stageNumber;
	private final boolean asc;

	/**
	 * Compares the solver names of jp1 and jp2
	 *
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */
	private int compareJobPairStrings(JobPair jp1, JobPair jp2) {
		JoblineStage stage1 = jp1.getStageFromNumber(stageNumber);
		JoblineStage stage2 = jp2.getStageFromNumber(stageNumber);
		try {
			String str1 = null;
			String str2 = null;
			switch (column) {
			case 3:
				str1 = stage1.getStatus().getStatus();
				str2 = stage2.getStatus().getStatus();
				break;
			case 5:
				str1 = stage1.getAttributes().getProperty(R.STAREXEC_RESULT);
				str2 = stage2.getAttributes().getProperty(R.STAREXEC_RESULT);
				break;
			case 0:
				str1 = jp1.getBench().getName();
				str2 = jp2.getBench().getName();
				break;
			case 2:
				str1 = stage1.getConfiguration().getName();
				str2 = stage2.getConfiguration().getName();
				break;
			default:
				str1 = stage1.getSolver().getName();
				str2 = stage2.getSolver().getName();
				break;
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
	 *
	 * @param jp1 The first job pair
	 * @param jp2 The second job pair
	 * @return 0 if jp1 should come first in a sorted list, 1 otherwise
	 * @author Eric Burns
	 */
	private int compareJobPairNums(JobPair jp1, JobPair jp2) {
		try {
			JoblineStage stage1 = jp1.getStageFromNumber(stageNumber);
			JoblineStage stage2 = jp2.getStageFromNumber(stageNumber);
			double db1 = 0;
			double db2 = 0;
			switch (column) {
			case 6:
				db1 = jp1.getId();
				db2 = jp2.getId();
				break;
			case 7:
				db1 = jp1.getCompletionId();
				db2 = jp2.getCompletionId();
				break;
			case 4:
				db1 = stage1.getWallclockTime();
				db2 = stage2.getWallclockTime();
				break;
			default:
				db1 = stage1.getCpuTime();
				db2 = stage2.getCpuTime();
				break;
			}

			return Double.compare(db1, db2);
		} catch (Exception e) {
			//either solver name was null, so we can just return jp1 as being first
		}

		return 0;
	}

	/**
	 * @param c Which column to compare on 0 = bench name 1 = solver name 2 = config name 3 = status name 4 = wallclock
	 * time 5 = starexec-result attr 6 pair id 7 completion id 8 is cpu time
	 */
	public JobPairComparator(int c, int stage, boolean a) {
		column = c;
		stageNumber = stage;
		asc = a;
	}

	@Override
	public int compare(JobPair o1, JobPair o2) {
		if (!asc) {
			JobPair temp = o1;
			o1 = o2;
			o2 = temp;
		}
		if (column != 4 && column != 6 && column != 7 && column != 8) {
			return compareJobPairStrings(o1, o2);
		} else {
			return compareJobPairNums(o1, o2);
		}
	}
}
