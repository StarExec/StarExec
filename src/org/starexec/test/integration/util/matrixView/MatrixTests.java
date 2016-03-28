package org.starexec.test.integration.util.matrixView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import org.junit.Assert;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecException;
import org.starexec.exceptions.StarExecSecurityException;
import org.starexec.util.matrixView.Matrix;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.test.integration.StarexecTest;
import org.starexec.test.integration.TestSequence;

public class MatrixTests extends TestSequence {
	private static final Logger log = Logger.getLogger(MatrixTests.class);

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
		job=ResourceLoader.loadJobIntoDatabase(space.getId(), user.getId(), -1, postProc.getId(), solverIds, benchmarkIds,
											   cpuTimeout,wallclockTimeout,gbMemory);
		job2=ResourceLoader.loadJobIntoDatabase(space.getId(), user2.getId(), -1, postProc.getId(), solverIds, 
												benchmarkIds, cpuTimeout, wallclockTimeout, gbMemory);
		Spaces.addJobSpace(space.getName(), job.getId());
		Assert.assertNotNull(Jobs.get(job.getId()));
	}

	@Override
	protected void teardown() {
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
