package org.starexec.test.integration.util.matrixView;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.*;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.test.TestUtil;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;
import org.starexec.util.matrixView.Matrix;

import java.util.*;

public class MatrixTests extends TestSequence {
	private static final StarLogger log = StarLogger.getLogger(MatrixTests.class);

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


	@Override 
	protected String getTestName() {
		return "MatrixTests";
	}

	@Override
	protected void setup() {
		user=loader.loadUserIntoDatabase();
		user2=loader.loadUserIntoDatabase();
		nonOwner=loader.loadUserIntoDatabase();
		admin=loader.loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa",R.ADMIN_ROLE_NAME);
		space=loader.loadSpaceIntoDatabase(user.getId(), Communities.getTestCommunity().getId());
		
		solver=loader.loadSolverIntoDatabase("CVC4.zip", space.getId(), user.getId());
		postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Communities.getTestCommunity().getId());
		benchmarkIds=loader.loadBenchmarksIntoDatabase("benchmarks.zip",space.getId(),user.getId());
		
		List<Integer> solverIds= new ArrayList<>();
		solverIds.add(solver.getId());
		job=loader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,
											   cpuTimeout,wallclockTimeout,gbMemory);
		job2=loader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, 
												benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Spaces.addJobSpace(space.getName(), job.getId());
		Assert.assertNotNull(Jobs.get(job.getId()));
	}

	@Override
	protected void teardown() {
		loader.deleteAllPrimitives();
	}

	@StarexecTest
	private void getMatrixForJobSpaceTest() {
		List<JobPair> jobPairs = job.getJobPairs();
		Matrix matrix;
		String jobSpaceName = space.getName();
		int jobSpaceId = space.getId();
		
		List<Benchmark> benchmarks = Benchmarks.get(benchmarkIds);	
		// sort alphabetically case insensitive
		Collections.sort(benchmarks, new Comparator<Benchmark>() {
			public int compare(Benchmark b1, Benchmark b2) {
				return b1.getName().toLowerCase().compareTo(b2.getName().toLowerCase());
			}
		});

		try {
			matrix = Matrix.getMatrixForJobSpaceFromJobAndStageNumber(job, space.getId(), jobPairs.get(0).getPrimaryStageNumber());
		} catch (StarExecException e) {
			Assert.fail("StarExecException thrown while trying to get matrix: " + e.getMessage());
			return;
		}

		List<Benchmark> benchmarkHeaders = matrix.getBenchmarksByRow();

		Assert.assertEquals(benchmarkHeaders.size(), benchmarks.size());
		Iterator<Benchmark> headerIt = benchmarkHeaders.iterator();
		Iterator<Benchmark> benchIt = benchmarks.iterator();
		while (headerIt.hasNext() || benchIt.hasNext()) {
			Assert.assertEquals(headerIt.next().getName(), benchIt.next().getName());
		}

		List<Pair<Solver, Configuration>> solverHeader = matrix.getSolverConfigsByColumn();
		Assert.assertEquals(solverHeader.get(0).getLeft().getName(), solver.getName());
	}
}
