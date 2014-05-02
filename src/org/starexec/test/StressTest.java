package org.starexec.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.test.resources.ResourceLoader;
import org.starexec.util.Util;

public class StressTest {
	private static final Logger log = Logger.getLogger(StressTest.class);

	//These values specify how many primitives the stress test will produce.
	private static int USER_COUNT=2000;
	private static int SPACE_COUNT=10000;
	
	private static int JOB_SPACE_COUNT=100;
	private static int JOB_COUNT=4;
	private static String SOLVER_NAME="CVC4.zip";
	private static String BENCHMARK_NAME="app12.zip"; //contains about 1500 benchmarks
	
	
	private static Random rand=new Random();
	
	private static void writeFakeJobPairOutput(JobPair pair) {
		try {
			File f=new File(JobPairs.getFilePath(pair));
			f.getParentFile().mkdirs();
			String randomOutput=TestUtil.getRandomAlphaString(1000);
			FileUtils.writeStringToFile(f, randomOutput);
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}
	
	private static Job loadBigJob(int parentSpaceId, int ownerId, int spaceCount, String solverName, String benchmarkName) {
		
		Users.setDiskQuota(ownerId, Util.gigabytesToBytes(1000)); //make sure we have the quota
		List<User> owner=new ArrayList<User>();
		owner.add(Users.get(ownerId));
		List<Space> spaces=loadSpaces(owner,parentSpaceId,spaceCount);
		
		for (Space s : spaces) {
			ResourceLoader.loadSolverIntoDatabase(solverName, s.getId(), ownerId);
			ResourceLoader.loadBenchmarksIntoDatabase(benchmarkName, s.getId(), ownerId);
		}
		Processor postProc=ResourceLoader.loadProcessorIntoDatabase("postproc.sh", ProcessorType.POST, Spaces.GetCommunityOfSpace(parentSpaceId));
		Job job=ResourceLoader.loadJobHierarchyIntoDatabase(parentSpaceId, ownerId, 1, postProc.getId());
		
		//Jobs.pause(job.getId()); //we don't want to actually run this job, as it will be too large
		
		//for (JobPair pair : job.getJobPairs()) {
	//		writeFakeJobPairOutput(pair);
		//	JobPairs.setPairStatus(pair.getId(), StatusCode.STATUS_COMPLETE.getVal());
	//	}
	//	Jobs.resume(job.getId());
		
		
		
		return job;
	}

	
	/**
	 * Loads the given number of new users into the database
	 * @return The list of new test users
	 */
	private static List<User> loadUsers(int count) {
		List<User> users=new ArrayList<User>();
		for (int i=0;i<count;i++) {
			users.add(ResourceLoader.loadUserIntoDatabase());
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
		List<Space> spaces=new ArrayList<Space> ();
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

				spaces.add(ResourceLoader.loadSpaceIntoDatabase(uid, parentSpace.getId()));
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
		List<Solver> solvers=new ArrayList<Solver>();
		for (Space s : spaces) {
			int solverCount=rand.nextInt(max-min+1)+min;
			while (solverCount>0) {
				solverCount--;
				int uid=users.get(rand.nextInt(users.size())).getId();
				solvers.add(ResourceLoader.loadSolverIntoDatabase(solverName,s.getId(),uid));
				Users.associate(uid, s.getId());

			}
		}
		return solvers;
	}
	
	public static void execute() {
		List<User> users=loadUsers(USER_COUNT);
		int communityLeaderId=users.get(rand.nextInt(users.size())).getId();
		Space community=ResourceLoader.loadSpaceIntoDatabase(communityLeaderId, 1);
		List<Space> spaces=loadSpaces(users,community.getId(), SPACE_COUNT);
		associateUsers(spaces,users,2,5);
		List<Solver> solvers=addSolvers(spaces,users,2,3,SOLVER_NAME);
		String name=null;
		for (int x=0;x<1000;x++) {
			name=TestUtil.getRandomJobName().substring(0,30);
			Space jobRootSpace=ResourceLoader.loadSpaceIntoDatabase(users.get(0).getId(),spaces.get(0).getId(),name);
			Job job=StressTest.loadBigJob(jobRootSpace.getId(), users.get(0).getId(), 1, SOLVER_NAME, BENCHMARK_NAME);
		}
		name="aaaaJobSpace";
		for (int x=0;x<JOB_COUNT;x++) {
			name=name+"a";
			Space jobRootSpace=ResourceLoader.loadSpaceIntoDatabase(users.get(0).getId(), spaces.get(0).getId(), name);
			Job job=StressTest.loadBigJob(jobRootSpace.getId(), users.get(0).getId(), JOB_SPACE_COUNT, SOLVER_NAME, BENCHMARK_NAME);
		}
		
	}
}
