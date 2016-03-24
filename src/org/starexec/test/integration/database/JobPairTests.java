package org.starexec.test.integration.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.junit.Assert;
import org.mockito.Mockito;
import org.starexec.backend.GridEngineBackend;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PairStageProcessorTriple;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.test.resources.ResourceLoader;
/**
 * Tests for org.starexec.data.database.JobPairs.java
 * @author Eric
 */
public class JobPairTests extends TestSequence {

	
	private Space space=null; //space to put the test job
	private Solver solver=null; //solver to use for the job
	private Job job=null;       
	private Processor postProc=null; //post processor to use for the job
	private List<Integer> benchmarkIds=null; // benchmarks to use for the job
	private User user=null;                  //owner of all the test primitives
	private User nonOwner=null;
	private User admin=null;
	private int wallclockTimeout=100;
	private int cpuTimeout=100;
	private int gbMemory=1;
	
	private User user2=null;
	private Job job2=null;
	private Random rand = new Random();
		
	@Override
	protected String getTestName() {
		return "JobPairTests";
	}
	
	@StarexecTest
	private void addAndRetrieveAttributesTest() {
		JobPair jp=job.getJobPairs().get(0);
		Properties p=new Properties();
		String prop=TestUtil.getRandomAlphaString(14);
		p.put(prop, prop);
		Assert.assertTrue(JobPairs.addJobPairAttributes(jp.getId(),jp.getPrimaryStage().getStageNumber(), p));
		Properties test=JobPairs.getAttributes(jp.getId()).get(jp.getPrimaryStage().getStageNumber());
		Assert.assertTrue(test.contains(prop));		
	}
	
	@StarexecTest
	private void updateBackendIdTest() {
		JobPair jp = job.getJobPairs().get(0);
		int backendId = rand.nextInt();
		Assert.assertTrue(JobPairs.updateBackendExecId(jp.getId(), backendId));
		Assert.assertEquals(backendId, JobPairs.getPair(jp.getId()).getBackendExecId());
		jp.setBackendExecId(backendId);
		
	}
	
	@StarexecTest
	private void getJobPairLogTest() {
		String path=JobPairs.getLogFilePath(job.getJobPairs().get(0));
		Assert.assertNotNull(path);
	}
	
	@StarexecTest
	private void getJobPairLogByIdTest() {
		String path=JobPairs.getStdout(job.getJobPairs().get(0).getId());
		Assert.assertNotNull(path);
	}
	
	@StarexecTest
	private void getJobPairPathTest() {
		String path=JobPairs.getPairStdout(job.getJobPairs().get(0));
		Assert.assertNotNull(path);
	}
	
	@StarexecTest
	private void getJobPairPathByIdTest() {
		String path=JobPairs.getStdout(job.getJobPairs().get(0).getId());
		Assert.assertNotNull(path);
	}
	
	@StarexecTest
	private void getJobPairTest() {
		JobPair test=JobPairs.getPair(job.getJobPairs().get(0).getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(test.getJobId(),job.getId());
	}
	
	@StarexecTest
	private void getJobPairDetailedTest() {
		JobPair test=JobPairs.getPairDetailed(job.getJobPairs().get(0).getId());
		Assert.assertNotNull(test);
		Assert.assertEquals(test.getJobId(),job.getId());
		
		Assert.assertEquals(test.getPrimarySolver().getName(),solver.getName());
	}
	
	@StarexecTest
	private void setPairStatusTest() {
		JobPair jp=JobPairs.getPair(job.getJobPairs().get(0).getId());
		Assert.assertTrue(JobPairs.setPairStatus(jp.getId(), StatusCode.STATUS_UNKNOWN.getVal()));
		Assert.assertEquals(StatusCode.STATUS_UNKNOWN.getVal(),JobPairs.getPair(jp.getId()).getStatus().getCode().getVal());
	}
	
	@StarexecTest
	private void setBrokenPairsToErrorStatusTest() throws IOException {
		JobPair jp=JobPairs.getPair(job.getJobPairs().get(0).getId());
		JobPairs.setPairStatus(jp.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		Jobs.setBrokenPairsToErrorStatus(R.BACKEND);
		Assert.assertTrue(JobPairs.getPair(job.getJobPairs().get(0).getId()).getStatus().getCode()==Status.StatusCode.ERROR_SUBMIT_FAIL);
	}
	
	@StarexecTest
	private void getStdOutTest() {
		JobPair jp = job.getJobPairs().get(0);
		String output = JobPairs.getStdOut(jp.getId(), 1, 1000);
		Assert.assertNotNull(output);
	}
	@StarexecTest
	private void getStdOutNoStagesTest() {
		JobPair jp = job.getJobPairs().get(0);

		String output = JobPairs.getStdout(jp.getId());
		Assert.assertNotNull(output);
	}
	
	@StarexecTest
	private void getPairsToBeProcessedTest() {
		JobPair jp = job.getJobPairs().get(0);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_PROCESSING.getVal());
		List<PairStageProcessorTriple> pairs = JobPairs.getAllPairsForProcessing();
		boolean found = false;
		for (PairStageProcessorTriple triple : pairs) {
			found = found || triple.getPairId()==jp.getId();
		}		
		Assert.assertTrue(found);
		JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void setBrokenPairsToErrorStatusNoChange() throws IOException {
		JobPair jp=JobPairs.getPair(job.getJobPairs().get(0).getId());
		HashSet<Integer> set = new HashSet<Integer>();
		set.add(jp.getBackendExecId());
		JobPairs.setPairStatus(jp.getId(), StatusCode.STATUS_ENQUEUED.getVal());
		GridEngineBackend backend = Mockito.mock(GridEngineBackend.class);
		Mockito.when(backend.getActiveExecutionIds()).thenReturn(set);
		Jobs.setBrokenPairsToErrorStatus(backend);
		Assert.assertTrue(JobPairs.getPair(job.getJobPairs().get(0).getId()).getStatus().getCode()==Status.StatusCode.STATUS_ENQUEUED);
	}

	@StarexecTest
	private void isPairCorrectIncompleteTest() {
		JoblineStage stage = new JoblineStage();
		stage.getStatus().setCode(StatusCode.STATUS_PENDING_SUBMIT.getVal());
		Assert.assertEquals(-1, JobPairs.isPairCorrect(stage));
	}
	
	@StarexecTest
	private void isPairCorrectCompleteTest() {
		JoblineStage stage = new JoblineStage();
		Properties attrs = new Properties();
		attrs.setProperty(R.EXPECTED_RESULT, "result");
		attrs.setProperty(R.STAREXEC_RESULT, "result");
		stage.setAttributes(attrs);
		stage.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertEquals(0, JobPairs.isPairCorrect(stage));
	}
	
	@StarexecTest
	private void isPairCorrectWrongTest() {
		JoblineStage stage = new JoblineStage();
		Properties attrs = new Properties();
		attrs.setProperty(R.EXPECTED_RESULT, "result");
		attrs.setProperty(R.STAREXEC_RESULT, "wrong");
		stage.setAttributes(attrs);
		stage.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertEquals(1, JobPairs.isPairCorrect(stage));
	}
	
	@StarexecTest
	private void isPairCorrectUnknownTest() {
		JoblineStage stage = new JoblineStage();
		Properties attrs = new Properties();
		attrs.setProperty(R.EXPECTED_RESULT, "result");
		attrs.setProperty(R.STAREXEC_RESULT,R.STAREXEC_UNKNOWN);
		stage.setAttributes(attrs);
		stage.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertEquals(2, JobPairs.isPairCorrect(stage));
	}
	
	@StarexecTest
	private void isPairCorrectNoExpectedTest() {
		JoblineStage stage = new JoblineStage();
		Properties attrs = new Properties();
		attrs.setProperty(R.STAREXEC_RESULT,"result");
		stage.setAttributes(attrs);
		stage.getStatus().setCode(StatusCode.STATUS_COMPLETE.getVal());
		Assert.assertEquals(2, JobPairs.isPairCorrect(stage));
	}
	
	@StarexecTest
	private void setPairStageStatusTest() {
		JobPair jp = job.getJobPairs().get(0);
		JoblineStage stage = jp.getStages().get(0);
		Assert.assertTrue(JobPairs.setPairStatus(jp.getId(), stage.getStageNumber(), StatusCode.STATUS_UNKNOWN.getVal()));
		Assert.assertEquals(StatusCode.STATUS_UNKNOWN.getVal(), JobPairs.getPairDetailed(jp.getId()).getStages().get(0).getStatus().getCode().getVal());
		JobPairs.setPairStatus(jp.getId(), stage.getStageNumber(), StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void setAllPairStageStatusTest() {
		JobPair jp = job.getJobPairs().get(0);
		Assert.assertTrue(JobPairs.setAllPairStageStatus(jp.getId(), StatusCode.STATUS_UNKNOWN.getVal()));
		Assert.assertEquals(StatusCode.STATUS_UNKNOWN.getVal(), JobPairs.getPairDetailed(jp.getId()).getStages().get(0).getStatus().getCode().getVal());
		JobPairs.setAllPairStageStatus(jp.getId(),StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@StarexecTest
	private void setStatusForPairAndStagesTest() {
		JobPair jp = job.getJobPairs().get(0);
		Assert.assertTrue(JobPairs.setStatusForPairAndStages(jp.getId(), StatusCode.STATUS_KILLED.getVal()));
		JobPair updatedPair = JobPairs.getPairDetailed(jp.getId());
		Assert.assertEquals(StatusCode.STATUS_KILLED.getVal(), updatedPair.getStages().get(0).getStatus().getCode().getVal());
		Assert.assertEquals(StatusCode.STATUS_KILLED.getVal(), updatedPair.getStatus().getCode().getVal());
		JobPairs.setStatusForPairAndStages(jp.getId(),StatusCode.STATUS_COMPLETE.getVal());
	}
	
	@Override
	protected void setup() throws Exception {
		user=ResourceLoader.loadUserIntoDatabase();
		user2=ResourceLoader.loadUserIntoDatabase();
		nonOwner=ResourceLoader.loadUserIntoDatabase();
		admin=Users.getAdmins().get(0);
		space=ResourceLoader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		solver=ResourceLoader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=ResourceLoader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds=new ArrayList<Integer>();
		solverIds.add(solver.getId());
		job=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,cpuTimeout,wallclockTimeout,gbMemory);
		job2=ResourceLoader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Assert.assertNotNull(Jobs.get(job.getId()));				
	}

	@Override
	protected void teardown() throws Exception {
		Jobs.deleteAndRemove(job.getId());
		Jobs.deleteAndRemove(job2.getId());
		Solvers.deleteAndRemoveSolver(solver.getId());
		for (Integer i : benchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		Processors.delete(postProc.getId());
		Spaces.removeSubspace(space.getId());
		Users.deleteUser(user.getId());
		Users.deleteUser(user2.getId());
		Users.deleteUser(nonOwner.getId());
		
	}

}
