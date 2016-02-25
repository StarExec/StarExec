package org.starexec.test.integration.database;

import java.io.File;
import java.util.List;

import org.starexec.constants.R;
import org.starexec.data.database.Statistics;
import org.starexec.data.to.JobPair;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.testng.Assert;

public class StatisticsTests extends TestSequence {
	
	List<JobPair> spaceOverviewPairs = null;
	
	@StarexecTest
	private void makeSpaceOverviewTest() {
		String filepath = Statistics.makeSpaceOverviewChart(spaceOverviewPairs, false, false, 1);
		File f = new File(new File(R.STAREXEC_ROOT).getParent(), filepath);
		Assert.assertTrue(f.exists());
	}
	
	@Override
	protected String getTestName() {
		return "StatisticsTests";
	}

	@Override
	protected void setup() throws Exception {
		spaceOverviewPairs = TestUtil.getFakeJobPairs(2000);
		
	}

	@Override
	protected void teardown() throws Exception {		
	}

}
