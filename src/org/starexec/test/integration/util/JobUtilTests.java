package org.starexec.test.integration.util;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.data.to.enums.ConfigXmlAttribute;
import org.starexec.data.to.enums.JobXmlType;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.data.to.tuples.ConfigAttrMapPair;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.JobUtil;
import org.starexec.util.Util;

public class JobUtilTests extends TestSequence {
	User admin = null;
	
	Solver solver = null;
	
	List<Integer> benchmarkIds = null;
	
	@StarexecTest
	private void testJobXMLUpload() throws Exception {
		int cId = solver.getConfigurations().get(0).getId();
		File xml = loader.getTestXMLFile(cId, cId, benchmarkIds.get(0), benchmarkIds.get(1));
		JobUtil util = new JobUtil();
		List<Integer> jobIds = util.createJobsFromFile(
				xml,
				admin.getId(),
				Communities.getTestCommunity().getId(),
				JobXmlType.STANDARD,
				new ConfigAttrMapPair(ConfigXmlAttribute.ID));
		Assert.assertEquals(1, jobIds.size());
		Job j = Jobs.get(jobIds.get(0));
		j.setJobPairs(Jobs.getJobPairsInJobSpaceHierarchy(j.getPrimarySpace(), PrimitivesToAnonymize.NONE));
		Assert.assertEquals("test xml job", j.getName());
		Assert.assertEquals("test job", j.getDescription());
		Assert.assertEquals(1, j.getQueue().getId());
		
		StageAttributes attrs1 = j.getStageAttributesByStageNumber(1);
		Assert.assertEquals(2000, attrs1.getCpuTimeout());
		Assert.assertEquals(5, attrs1.getWallclockTimeout());
		Assert.assertEquals(Util.gigabytesToBytes(3.0), attrs1.getMaxMemory());
		
		StageAttributes attrs2 = j.getStageAttributesByStageNumber(2);
		Assert.assertEquals(12, attrs2.getCpuTimeout());
		Assert.assertEquals(11, attrs2.getWallclockTimeout());
		Assert.assertEquals(Util.gigabytesToBytes(2.0), attrs2.getMaxMemory());
		
		Assert.assertEquals(1, j.getJobPairs().size());
		JobPair jp = j.getJobPairs().get(0);
		Assert.assertEquals(3, jp.getStages().size());
		
		Jobs.deleteAndRemove(j.getId());
	}
	
	@Override
	protected String getTestName() {
		return "JobUtilTests";
	}

	@Override
	protected void setup() throws Exception {
		admin = loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		solver = loader.loadSolverIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
		benchmarkIds = loader.loadBenchmarksIntoDatabase(Communities.getTestCommunity().getId(), admin.getId());
		
	}

	@Override
	protected void teardown() throws Exception {
		loader.deleteAllPrimitives();
	}

}
