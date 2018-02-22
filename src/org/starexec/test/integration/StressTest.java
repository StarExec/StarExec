package org.starexec.test.integration;

import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.*;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.logger.StarLogger;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StressTest {
	private static final StarLogger log = StarLogger.getLogger(StressTest.class);

	private static final String SOLVER_NAME="smallsolver.zip";
	private static final String BENCHMARK_NAME="app12.zip"; //contains about 1500 benchmarks
	private static final ResourceLoader loader = new ResourceLoader();
	
	private static final Random rand=new Random();
	
	private static Job loadBigJob(int parentSpaceId, int ownerId, int spaceCount, String solverName, String benchmarkName,
			int minSolversPerSpace, int maxSolversPerSpace, int minBenchmarksPerSpace, int maxBenchmarksPerSpace) {
		
		Users.setDiskQuota(ownerId, Util.gigabytesToBytes(1000)); //make sure we have the quota
		List<User> owner= new ArrayList<>();
		owner.add(Users.get(ownerId));
		List<Space> spaces=loadSpaces(owner,parentSpaceId,spaceCount);
		
		addSolvers(spaces,owner,minSolversPerSpace,maxSolversPerSpace,solverName);
		addBenchmarks(spaces,owner,minBenchmarksPerSpace,maxBenchmarksPerSpace,benchmarkName);
		
		Processor postProc=loader.loadProcessorIntoDatabase("postproc.zip", ProcessorType.POST, Spaces.getCommunityOfSpace(parentSpaceId));
		Job job=loader.loadJobHierarchyIntoDatabase(parentSpaceId, ownerId, 1, postProc.getId());
		
		Jobs.pause(job.getId()); //we don't want to actually run this job, as it will be too large
		
		for (JobPair pair : job.getJobPairs()) {
			loader.writeFakeJobPairOutput(pair);
			JobPairs.setStatusForPairAndStages(pair.getId(), StatusCode.STATUS_COMPLETE.getVal());
		}
		Jobs.resume(job.getId());
		
		
		
		return job;
	}

	
	/**
	 * Loads the given number of new users into the database
	 * @return The list of new test users
	 */
	private static List<User> loadUsers(int count) {
		List<User> users= new ArrayList<>();
		for (int i=0;i<count;i++) {
			users.add(loader.loadUserIntoDatabase());
		}
		return users;
	}
	
	/**
	 * Populates the database with the given number of spaces, with a root at
	 * root space id.  Non-leaf spaces will have between 1 and 5 subspaces.
	 * @param owners A list of users, which will be sampled randomly to assign leaders
	 * to the spaces
	 * @param count The number of spaces to make
	 * @return
	 */
	private static List<Space> loadSpaces(List<User> owners, int rootSpaceId, int count) {
		List<Space> spaces= new ArrayList<>();
		spaces.add(Spaces.get(rootSpaceId));
		int parentSpaceIndex=0;
		while (count>0) {
			
			Space parentSpace=spaces.get(parentSpaceIndex);
			int numSubspaces=rand.nextInt(5)+1; //adding this number of subspaces to the current parent
			while (numSubspaces>0) {
				numSubspaces--;
				count--;
				if (count<0) {
					break;
				}
				int uid=owners.get(rand.nextInt(owners.size())).getId();

				spaces.add(loader.loadSpaceIntoDatabase(uid, parentSpace.getId()));
			}
			parentSpaceIndex++; //go to the next parent space.
		}
		return spaces;
	}
	
	/**
	 * Associates users randomly among the spaces. 
	 * @param spaces Spaces to add users to
	 * @param users Users to put in spaces
	 * @param min Minimum number of users added to each space
	 * @param max Maximum number of users added to each space
	 */
	private static void associateUsers(List<Space> spaces, List<User> users, int min, int max) {
		for (Space s : spaces) {
			int userCount=rand.nextInt(max-min+1)+min;
			while (userCount>0) {
				userCount--;
				int uid=users.get(rand.nextInt(users.size())).getId();
				Users.associate(uid, s.getId());

			}
		}
	}
	/**
	 * Adds solvers with random owners to random spaces
	 * @param spaces Spaces to add solvers too
	 * @param users Users to be owners of the solvers
	 * @param min Minimum number of solvers added to each space
	 * @param max Maximum number of solvers added to each space
	 */
	private static List<Solver> addSolvers(List<Space> spaces, List<User> users, int min, int max, String solverName) {
		List<Solver> solvers= new ArrayList<>();
		for (Space s : spaces) {
			int solverCount=rand.nextInt(max-min+1)+min;
			while (solverCount>0) {
				solverCount--;
				int uid=users.get(rand.nextInt(users.size())).getId();
				solvers.add(loader.loadSolverIntoDatabase(solverName,s.getId(),uid));
			}
		}
		return solvers;
	}
	
	/**
	 * Adds solvers with random owners to random spaces
	 * @param spaces Spaces to add solvers too
	 * @param users Users to be owners of the solvers
	 * @param min Minimum number of solvers added to each space
	 * @param max Maximum number of solvers added to each space
	 */
	private static List<Integer> addBenchmarks(List<Space> spaces, List<User> users, int min, int max, String benchName) {
		List<Integer> benchmarks= new ArrayList<>();
		for (Space s : spaces) {
			int benchCount=rand.nextInt(max-min+1)+min;
			while (benchCount>0) {
				benchCount--;
				int uid=users.get(rand.nextInt(users.size())).getId();
				benchmarks.addAll(loader.loadBenchmarksIntoDatabase(benchName,s.getId(),uid));
			}
		}
		return benchmarks;
	}
	
	/**
	 * Runs a stress test of a size determined by the given parameters
	 * @param userCount
	 * @param spaceCount
	 * @param minUsersPerSpace
	 * @param maxUsersPerSpace
	 * @param minSolversPerSpace
	 * @param maxSolversPerSpace
	 * @param minBenchmarksPerSpace
	 * @param maxBenchmarksPerSpace
	 * @param jobCount
	 * @param spaceCountPerJob
	 */
	public static void execute(int userCount, int spaceCount, int minUsersPerSpace, int maxUsersPerSpace,
			int minSolversPerSpace, int maxSolversPerSpace, int minBenchmarksPerSpace, int maxBenchmarksPerSpace,
			int jobCount, int spaceCountPerJob) {
		List<User> users=loadUsers(userCount);
		int communityLeaderId=users.get(rand.nextInt(users.size())).getId();
		Space community=loader.loadSpaceIntoDatabase(communityLeaderId, 1);
		List<Space> spaces=loadSpaces(users,community.getId(), spaceCount);
		associateUsers(spaces,users,minUsersPerSpace,maxUsersPerSpace);
		addSolvers(spaces,users,minSolversPerSpace,maxSolversPerSpace,SOLVER_NAME);
		addBenchmarks(spaces,users,minBenchmarksPerSpace,maxBenchmarksPerSpace,BENCHMARK_NAME);
		String name="JobSpace";
		Space jobParentSpace=loader.loadSpaceIntoDatabase(users.get(0).getId(), spaces.get(0).getId(), name);
		for (int x=0;x<jobCount;x++) {
			Space jobRootSpace=loader.loadSpaceIntoDatabase(users.get(0).getId(), jobParentSpace.getId());
			StressTest.loadBigJob(jobRootSpace.getId(), users.get(0).getId(), spaceCountPerJob, SOLVER_NAME, BENCHMARK_NAME,minSolversPerSpace,maxSolversPerSpace, minBenchmarksPerSpace,maxBenchmarksPerSpace);
		}
		
	}
}
