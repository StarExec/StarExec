package org.starexec.data.database;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.starexec.backend.Backend;
import org.starexec.backend.GridEngineBackend;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.database.AnonymousLinks.PrimitivesToAnonymize;
import org.starexec.data.to.*;
import org.starexec.data.to.SolverBuildStatus.SolverBuildStatusCode;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.compare.JobPairComparator;
import org.starexec.data.to.compare.SolverComparisonComparator;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.data.to.tuples.AttributesTableData;
import org.starexec.data.to.tuples.TimePair;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Handles all database interaction for jobs (NOT grid engine job execution, see JobManager for that)
 *
 * @author Tyler Jensen
 */

public class Jobs {
	private static final StarLogger log = StarLogger.getLogger(Jobs.class);

	/**
	 * Returns a list of job spaces that are present in the given path. Spaces are returned ordered from top level to
	 * bottom level. An exception is thrown if the given path is null or empty
	 *
	 * @param path The / delimited path
	 * @return
	 */
	private static String[] getSpaceNames(String path) throws IllegalArgumentException {
		if (Util.isNullOrEmpty(path)) {
			throw new IllegalArgumentException("Job paths cannot be empty");
		}
		return path.split(R.JOB_PAIR_PATH_DELIMITER);
	}

	/**
	 * Creates a Space object for use during job creation for any job where benchmarks are going to be saved
	 *
	 * @param name
	 * @param parent
	 * @return
	 */
	private static Space getNewSpaceForJobCreation(String name, Space parent, int parentId) {
		Space s = new Space();
		s.setDescription("");
		s.setName(name);
		s.setStickyLeaders(parent.isStickyLeaders());
		s.setPermission(parent.getPermission());
		s.setLocked(parent.isLocked());
		s.setPublic(parent.isPublic());
		s.setParentSpace(parentId);
		return s;
	}

	/**
	 * Given a set job job pairs, creates a set of spaces that mirrors the job space hierarchy
	 *
	 * @param pairs The pairs to use
	 * @param userId The user who will own all the new spaces
	 * @param con An open connection to make SQL calls on
	 * @param parentSpaceId The ID of the parent space to root the hierarchy in
	 * @throws Exception An exception if some space cannot be added
	 */
	public static void createSpacesForPairs(List<JobPair> pairs, int userId, Connection con, int parentSpaceId)
			throws Exception {
		Space parent = null;
		parent = Spaces.get(parentSpaceId, con);
		parent.setPermission(Permissions.getSpaceDefault(parentSpaceId));
		HashMap<String, Integer> pathsToIds = new HashMap<>(); // maps a job space path to a job space id
		for (JobPair pair : pairs) {
			//log.debug("finding spaces for a new pair with path = " +pair.getPath());
			String[] spaces = getSpaceNames(pair.getPath());
			StringBuilder curPathBuilder = new StringBuilder();
			for (String name : spaces) {
				curPathBuilder.append(R.JOB_PAIR_PATH_DELIMITER);
				curPathBuilder.append(name);
				//if we need to create a new space
				if (!pathsToIds.containsKey(curPathBuilder.toString())) {
					String parentPath = curPathBuilder.toString();
					parentPath = parentPath.substring(0, parentPath.lastIndexOf('/'));

					// note that it is assumed that there are no name conflicts here. The security check is done
					// outside this function
					int parentId = 0;
					if (!parentPath.isEmpty()) {
						parentId = pathsToIds.get(parentPath);
					} else {
						parentId = parent.getId();
					}
					int newId = Spaces.add(con, getNewSpaceForJobCreation(name, parent, parentId), userId);
					if (newId == -1) {
						throw new Exception("error adding new space-- creating spaces for job failed");
					}
					pathsToIds.put(curPathBuilder.toString(), newId);
				}
			}
		}
	}

	/**
	 * Creates all the job spaces needed for a set of pairs. All pairs must have their paths set and they must all be
	 * rooted at the same space. Upon return, each pair will have its job space id set to the correct job space
	 *
	 * @param jobId ID of the job that owns the given pairs
	 * @param pairs The list of pairs to make paths for
	 * @param con The open connection to make calls on
	 * @return The ID of the root job space for this list of pairs, or null on error.
	 * @throws Exception
	 */
	public static Integer createJobSpacesForPairs(int jobId, List<JobPair> pairs, Connection con) {

		//this hashmap maps every job space ID to the maximal number of stages
		// of any pair that is in the hierarchy rooted at the job space
		HashMap<Integer, Integer> idsToMaxStages = new HashMap<>();
		HashMap<String, Integer> pathsToIds = new HashMap<>(); // maps a job space path to a job space id
		int topLevelSpaceId = -1; // -1 indicates that it is not set
		for (JobPair pair : pairs) {
			//log.debug("finding spaces for a new pair with path = " +pair.getPath());
			String[] spaces = getSpaceNames(pair.getPath());
			StringBuilder curPathBuilder = new StringBuilder();
			for (String jobSpaceName : spaces) {
				curPathBuilder.append(R.JOB_PAIR_PATH_DELIMITER);
				curPathBuilder.append(jobSpaceName);

				//if we need to create a new space
				if (!pathsToIds.containsKey(curPathBuilder.toString())) {
					String parentPath = curPathBuilder.toString();
					parentPath = parentPath.substring(0, parentPath.lastIndexOf(R.JOB_PAIR_PATH_DELIMITER));
					pathsToIds.put(curPathBuilder.toString(), Spaces.addJobSpace(jobSpaceName, jobId, con));
					int id = pathsToIds.get(curPathBuilder.toString());
					if (topLevelSpaceId == -1) {
						topLevelSpaceId = id;
					}
					idsToMaxStages.put(id, pair.getStages().size());
					//associate the new space to its parent

					if (!parentPath.isEmpty()) {
						int parentId = pathsToIds.get(parentPath);
						Spaces.associateJobSpaces(parentId, pathsToIds.get(curPathBuilder.toString()), con);
					}
				}
				int id = pathsToIds.get(curPathBuilder.toString());
				idsToMaxStages.put(id, Math.max(idsToMaxStages.get(id), pair.getStages().size()));
			}
			pair.setJobSpaceId(pathsToIds.get(curPathBuilder.toString()));
		}
		for (Integer id : idsToMaxStages.keySet()) {
			Spaces.setJobSpaceMaxStages(id, idsToMaxStages.get(id), con);
		}
		return topLevelSpaceId;
	}

	/**
	 * Makes all new job spaces for the given job and moves all the pairs over to the new spaces. Useful if the job
	 * spaces for the given job were somehow corrupted, but path information for the pairs is correct
	 *
	 * @param jobId The ID of the job to fix
	 * @return True on success and false otherwise
	 */
	public static boolean recompileJobSpaces(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			List<JobPair> pairs = Jobs.getPairsSimple(jobId);

			int topLevel = createJobSpacesForPairs(jobId, pairs, con);
			Jobs.updatePrimarySpace(jobId, topLevel, con);
			JobPairs.updateJobSpaces(pairs, con);
			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			Common.doRollback(con);
			log.error("recompileJobSpaces", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	public static int countJobPairsToBeAddedFromConfigIdsForPairedBenchmarks(
			int jobId, Set<Integer> configIds, Set<Integer> idsOfDeletedJobPairs
	) {
		Job job = Jobs.getWithSimplePairs(jobId);
		List<JobPair> jobPairsToAdd = new ArrayList<>();
		List<JobPair> jobPairs = job.getJobPairs();
		int countOfJobPairsToAdd = 0;

		for (Integer configId : configIds) {
			Set<Integer> benchmarksAlreadySeen = new HashSet<>();
			// Get the solver associated with the config we want to add to the job.
			Solver solver = Solvers.getByConfigId(configId);
			for (JobPair pair : jobPairs) {
				// If a pair contains the solver add a new job pair with the new config to the job.
				if (pair.getStages().size() == 1 && // skip multi-stage pairs
						pair.getPrimaryStage().getSolver().getId() == solver.getId() &&
						!benchmarksAlreadySeen.contains(pair.getBench().getId()) &&
						// Skip pairs that the user has decided to delete.
						!idsOfDeletedJobPairs.contains(pair.getId())) {
					countOfJobPairsToAdd += 1;

					benchmarksAlreadySeen.add(pair.getBench().getId());
				}
			}
		}
		return countOfJobPairsToAdd;
	}

	public static int countJobPairsToBeAddedFromConfigIdsForAllBenchmarks(
			int jobId, Set<Integer> configIds, Set<Integer> idsOfDeletedJobPairs
	) {
		int jobPairsToAddCount = 0;

		// Maintain this hashmap that keeps track of which benchmark-solver-config triples we've seen.
		Map<Integer, Map<Integer, Set<Integer>>> jobMap = Jobs.getJobMapForPrimaryStage(jobId);

		Job job = Jobs.getWithSimplePairs(jobId);
		List<JobPair> jobPairs = job.getJobPairs();

		for (Integer configIdToAdd : configIds) {

			// Get the solver associated with the config we want to add to the job.
			final int solverIdToAdd = Solvers.getByConfigId(configIdToAdd).getId();


			for (JobPair pair : jobPairs) {
				final int pairBenchId = pair.getBench().getId();

				// Skip multi-stage pairs.
				if (pair.getStages().size() == 1 &&
						!jobMapContainsBenchSolverConfigTriple(jobMap, pairBenchId, solverIdToAdd, configIdToAdd)
						// Skip job pairs the user has deleted.
						&& !idsOfDeletedJobPairs.contains(pair.getId())) {

					// Add the new benchmark-solver-config pair so that we don't add it as a duplicate.
					addBenchSolverConfigTripleToJobMap(jobMap, pairBenchId, solverIdToAdd, configIdToAdd);

					final int pairSolverId = pair.getPrimaryStage().getSolver().getId();
					/*
					log.debug( "Counting job pairs to add, old bench-solver-config triple:
					"+pairBenchId+"-"+pairSolverId+"-"
							+pair.getPrimaryStage().getConfiguration().getId() );
					log.debug( "Counting job pairs to add, new bench-solver-config triple: "+pairBenchId
					+"-"+solverIdToAdd+"-"+configIdToAdd );
					log.debug("");
					*/


					jobPairsToAddCount += 1;
				}
			}
		}

		return jobPairsToAddCount;
	}

	private static boolean jobMapContainsBenchSolverConfigTriple(
			Map<Integer, Map<Integer, Set<Integer>>> jobMap, int benchId, int solverId, int configId
	) {
		return jobMap.containsKey(benchId) && jobMap.get(benchId).containsKey(solverId) &&
				jobMap.get(benchId).get(solverId).contains(configId);
	}

	/**
	 * Adds a new job pair using the input list of configurations for each existing job pair in the job that doesn't
	 * already contain the configuration.
	 *
	 * @param jobId the id of the job.
	 * @param configIds the configurations to add to the job.
	 * @author Albert Giegerich
	 */
	public static void addJobPairsFromConfigIdsForAllBenchmarks(int jobId, Set<Integer> configIds) {
		if (isReadOnly(jobId)) {
			log.warn("addJobPairsFromConfigIdsForAllBenchmarks", "Job is readonly: "+jobId);
			return;
		}

		List<JobPair> jobPairsToAdd = new ArrayList<>();
		// Maintain this hashmap that keeps track of which benchmark-solver-config triples we've seen.
		Map<Integer, Map<Integer, Set<Integer>>> jobMap = Jobs.getJobMapForPrimaryStage(jobId);
		for (Integer configIdToAdd : configIds) {

			// Get the solver associated with the config we want to add to the job.
			final Solver solverToAdd = Solvers.getByConfigId(configIdToAdd);
			final int solverIdToAdd = solverToAdd.getId();

			// Get new job pairs so that we don't modify a reference we've already added to jobPairsToAdd
			Job job = Jobs.getWithSimplePairs(jobId);
			List<JobPair> jobPairs = job.getJobPairs();

			for (JobPair pair : jobPairs) {
				final int pairBenchId = pair.getBench().getId();

				// Skip multi-stage pairs.
				if (pair.getStages().size() == 1 &&
						!jobMapContainsBenchSolverConfigTriple(jobMap, pairBenchId, solverIdToAdd, configIdToAdd)) {

					// Add the new benchmark-solver-config pair so that we don't add it as a duplicate.
					addBenchSolverConfigTripleToJobMap(jobMap, pairBenchId, solverIdToAdd, configIdToAdd);

					Configuration configToAdd = Solvers.getConfiguration(configIdToAdd);
					pair.getPrimaryStage().setSolver(solverToAdd);
					pair.getPrimaryStage().setConfiguration(configToAdd);

					jobPairsToAdd.add(pair);
				}
			}
		}

		// Add the new job pairs.
		JobPairs.addJobPairs(jobId, jobPairsToAdd);

		// Clear the cached job stats for this job so the new job pairs will contribute to the job stats.
		removeCachedJobStats(jobId);
	}

	/**
	 * Builds a mapping from all benchmarks to solvers paired with those benchmarks (in job pairs) to configs paired
	 * with those benchmarks and solvers (in job pairs) for a given stage. (Only uses pairs for the given stage.)
	 *
	 * @param jobId The job to get the pairs from to build the mapping.
	 * @param stageNumber The stage number to filter by.
	 * @author Albert Giegerich
	 */
	public static Map<Integer, Map<Integer, Set<Integer>>> getJobMapForStage(int jobId, int stageNumber) {
		return getJobMap(jobId, false, stageNumber);
	}

	/**
	 * Builds a mapping from all benchmarks to solvers paired with those benchmarks (in job pairs) to configs paired
	 * with those benchmarks and solvers (in job pairs) for the primary stage. (Only uses pairs for the primary stage.)
	 *
	 * @param jobId The job to get the pairs from to build the mapping.
	 * @author Albert Giegerich
	 */
	public static Map<Integer, Map<Integer, Set<Integer>>> getJobMapForPrimaryStage(int jobId) {
		return getJobMap(jobId, true, -1);
	}

	private static Map<Integer, Map<Integer, Set<Integer>>> getJobMap(
			int jobId, boolean usePrimaryStage, int stageNumber
	) {
		Map<Integer, Map<Integer, Set<Integer>>> jobMap = new HashMap<>();

		Job job = Jobs.getWithSimplePairs(jobId);
		List<JobPair> jobPairs = job.getJobPairs();

		for (JobPair pair : jobPairs) {
			JoblineStage stage = usePrimaryStage ? pair.getPrimaryStage() : pair.getStageFromNumber(stageNumber);
			if (stage == null) {
				log.debug("Found null stage, continuing");
				continue;
			}


			final int pairBenchId = pair.getBench().getId();
			final int pairSolverId = stage.getSolver().getId();
			final int pairConfigId = stage.getConfiguration().getId();

			if (pairBenchId == 1) {
				log.debug(
						"Building Map - bench-solver-config: " + pairBenchId + "-" + pairSolverId + "-" +
								pairConfigId);
			}

			addBenchSolverConfigTripleToJobMap(jobMap, pairBenchId, pairSolverId, pairConfigId);
		}

		return jobMap;
	}

	private static void addBenchSolverConfigTripleToJobMap(
			Map<Integer, Map<Integer, Set<Integer>>> jobMap, int benchId, int solverId, int configId
	) {
		if (jobMap.containsKey(benchId)) {
			if (jobMap.get(benchId).containsKey(solverId)) {
				jobMap.get(benchId).get(solverId).add(configId);
			} else {
				Set<Integer> configs = new HashSet<>();
				configs.add(configId);
				jobMap.get(benchId).put(solverId, configs);
			}
		} else {
			Map<Integer, Set<Integer>> solverToConfigs = new HashMap<>();
			Set<Integer> configs = new HashSet<>();
			configs.add(configId);
			solverToConfigs.put(solverId, configs);
			jobMap.put(benchId, solverToConfigs);
		}
	}

	/**
	 * Adds a new job pair using the input list of configurations for each existing job pair in the job that contains
	 * the solver associated with the configuration.
	 *
	 * @param jobId the id of the job.
	 * @param configIds the configurations to add to the job.
	 * @author Albert Giegerich
	 */
	public static void addJobPairsFromConfigIdsForPairedBenchmarks(int jobId, Set<Integer> configIds) {

		List<JobPair> jobPairsToAdd = new ArrayList<>();

		for (Integer configId : configIds) {
			Set<Integer> benchmarksAlreadySeen = new HashSet<>();
			// Get the solver associated with the config we want to add to the job.
			Solver solver = Solvers.getByConfigId(configId);

			// We need a fresh list of job pairs each time so that the references change with each iteration.
			Job job = Jobs.getWithSimplePairs(jobId);
			List<JobPair> jobPairs = job.getJobPairs();
			for (JobPair pair : jobPairs) {
				// If a pair contains the solver add a new job pair with the new config to the job.
				if (pair.getStages().size() == 1 && // skip multi-stage pairs
						pair.getPrimaryStage().getSolver().getId() == solver.getId() &&
						!benchmarksAlreadySeen.contains(pair.getBench().getId())) {
					// Modify the current pair by changing the configuration then add the new job pair to the job.
					pair.getPrimaryStage().setConfiguration(Solvers.getConfiguration(configId));
					jobPairsToAdd.add(pair);
					benchmarksAlreadySeen.add(pair.getBench().getId());
				}
			}
		}

		// Add the new job pairs.
		JobPairs.addJobPairs(jobId, jobPairsToAdd);

		// Clear the cached job stats for this job so the new job pairs will contribute to the job stats.
		removeCachedJobStats(jobId);
	}

	/**
	 * Adds a new job to the database. NOTE: This only records the job in the database, this does not actually submit a
	 * job for execution (see JobManager.submitJob). This method also fills in the IDs of job pairs of the given job
	 * object.
	 *
	 * @param job The job data to add to the database
	 * @param spaceId The id of the space to add the job to if pipelines have not yet been created (most cases) and
	 * false if they have (job XML)
	 * @return True if the operation was successful, false otherwise.
	 */
	public static boolean add(Job job, int spaceId) {
		Connection con = null;
		PreparedStatement procedure = null;
		try {
			log.debug("starting to add a new job with pair count =  " + job.getJobPairs().size());
			con = Common.getConnection();

			// gets the name of the root job space for this job
			String rootName = job.getRootSpaceName();
			//start a transaction that encapsulates making new spaces for mirrored hierarchies
			Common.beginTransaction(con);
			//get all the different space IDs for the places we need to created mirrors of the job space heirarchy
			HashSet<Integer> uniqueSpaceIds = new HashSet<>();
			for (StageAttributes attrs : job.getStageAttributes()) {
				if (attrs.getSpaceId() != null) {
					//make sure that there are no name conflicts when creating the mirrored space hierarchies.
					if (Spaces.getSubSpaceIDbyName(attrs.getSpaceId(), rootName, con) != -1) {
						throw new Exception("Error creating spaces for job: name conflict with space name " +
								                    rootName);
					}
					uniqueSpaceIds.add(attrs.getSpaceId());
				}
			}
			//create mirror space hierarchies for saving benchmarks if the user wishes
			for (Integer i : uniqueSpaceIds) {
				createSpacesForPairs(job.getJobPairs(), job.getUserId(), con, i);
			}
			//we end the first transaction here so that we don't end up keeping a lock on the space tables
			// for the entire duration of job creation
			Common.endTransaction(con);
			//creates the job space hierarchy for the job and returns the ID of the top level job space

			log.debug("finished getting subspaces, adding job");
			//the primary space of a job should be a job space ID instead of a space ID


			Jobs.addJob(con, job);
			job.setPrimarySpace(createJobSpacesForPairs(job.getId(), job.getJobPairs(), con));
			Jobs.updatePrimarySpace(job.getId(), job.getPrimarySpace(), con);
			//NOTE: By opening the transaction here, we are leaving open the possibility that some spaces
			//will be created even if job creation fails. However, this prevents the job space and the space
			//tables from being locked for the entire transaction, which may take a long time.
			Common.beginTransaction(con);
			// record the job being added in the reports table
			Reports.addToEventOccurrencesNotRelatedToQueue("jobs initiated", 1);
			// record the job being added for the queue it was added to
			Reports.addToEventOccurrencesForQueue("jobs initiated", 1, job.getQueue().getName());

			Analytics.JOB_CREATE.record(job.getUserId());


			log.debug("job added, associating next");
			//put the job in the space it was created in, assuming a space was selected
			if (spaceId > 0) {
				Jobs.associate(con, job.getId(), spaceId);
			}

			log.debug("job associated, adding this many stage attributes " + job.getStageAttributes().size());

			//this times out waiting for a lock if it isn't done after the transaction.
			for (StageAttributes attrs : job.getStageAttributes()) {
				attrs.setJobId(job.getId());
				Jobs.addJobStageAttributes(attrs, con);
			}

			log.debug("adding job pairs");

			JobPairs.addJobPairs(con, job.getId(), job.getJobPairs());

			Common.endTransaction(con);
			//Create the output directory for the job up front. This ensures that if a user
			//tries to download output before any exists, they will get a correctly formatted
			//zip containing an empty directory.
			new File(Jobs.getDirectory(job.getId())).mkdirs();
			log.debug("job added successfully");
			Jobs.resume(job.getId(), con); // now that the job has been added, we can resume
			return true;
		} catch (Exception e) {
			log.error("add", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Adds a job record to the database. This is a helper method for the Jobs.add method
	 *
	 * @param con The connection the update will take place on
	 * @param job The job to add
	 */
	private static void addJob(Connection con, Job job) {
		CallableStatement procedure = null;

		if (job.getBenchmarkingFramework() == null) {
			job.setBenchmarkingFramework(R.DEFAULT_BENCHMARKING_FRAMEWORK);
		}

		try {
			procedure = con.prepareCall("{CALL AddJob(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1, job.getUserId());
			procedure.setString(2, job.getName());
			procedure.setString(3, job.getDescription());
			procedure.setInt(4, job.getQueue().getId());
			procedure.setInt(5, job.getPrimarySpace());
			procedure.setLong(6, job.getSeed());
			procedure.setInt(7, job.getCpuTimeout());
			procedure.setInt(8, job.getWallclockTimeout());
			procedure.setInt(9, job.getSoftTimeLimit());
			procedure.setInt(10, job.getKillDelay());
			procedure.setLong(11, job.getMaxMemory());
			procedure.setBoolean(12, job.timestampIsSuppressed());
			procedure.setBoolean(13, job.isUsingDependencies());
			procedure.setBoolean(14, job.isBuildJob());
			procedure.setInt(15, job.getJobPairs().size());
			procedure.setString(16, job.getBenchmarkingFramework().toString());
			procedure.registerOutParameter(17, java.sql.Types.INTEGER);
			procedure.executeUpdate();

			// Update the job's ID so it can be used outside this method
			job.setId(procedure.getInt(17));
		} catch (Exception e) {
			log.error("addJob", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds an association between the given job id and the given space
	 *
	 * @param con The connection to make the association on
	 * @param jobId the id of the job we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @author Tyler Jensen
	 */
	protected static void associate(Connection con, int jobId, int spaceId) {

		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AssociateJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, spaceId);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error("associate", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds an association between all the given job ids and the given space
	 *
	 * @param jobIds the ids of the jobs we are associating to the space
	 * @param spaceId the ID of the space we are making the association to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> jobIds, int spaceId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction(con);

			for (int jid : jobIds) {
				Jobs.associate(con, jid, spaceId);
			}

			Common.endTransaction(con);
			return true;
		} catch (Exception e) {
			log.error("associate", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Removes all job database entries where the job has been deleted AND has been orphaned
	 *
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedJobs() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			//will contain the id of every job that is associated with a space
			HashSet<Integer> parentedJobs = new HashSet<>();
			procedure = con.prepareCall("{CALL GetJobsAssociatedWithSpaces()}");
			results = procedure.executeQuery();
			while (results.next()) {
				parentedJobs.add(results.getInt("id"));
			}
			Common.safeClose(procedure);
			Common.safeClose(results);

			procedure = con.prepareCall("CALL GetDeletedJobs()");
			results = procedure.executeQuery();

			while (results.next()) {
				Job j = resultsToJob(results);
				int jobId = j.getId();

				if (isReadOnly(jobId)) {
					log.info("cleanOrphanedDeletedJobs", "Job "+jobId+" is readonly. Not cleaning.");
					continue;
				}

				File jobDir = new File(Jobs.getDirectory(jobId));
				if (jobDir.exists()) {
					log.warn("a deleted job still exists on disk! id = " + jobId);
					if (!FileUtils.deleteQuietly(jobDir)) {
						log.warn("the job could not be deleted! Not removing job from the database");
						continue;
					}
				}
				// the benchmark has been deleted AND it is not associated with any spaces or job pairs
				if (!parentedJobs.contains(jobId)) {
					removeJobFromDatabase(jobId);
				}
			}
			return true;
		} catch (Exception e) {
			log.error("cleanOrphanedDeletedJobs", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Counts the number of pairs that occurred before the given completion ID (inclusive)
	 *
	 * @param jobId The ID of the job to count pairs for
	 * @param since The completion ID to use as the cutoff
	 * @return The integer number of pairs, or -1 on error
	 */
	public static int countOlderPairs(int jobId, int since) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CountOlderPairs(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, since);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("countOlderPairs", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Deletes the job with the given id from disk, and permanently removes the job from the database. This is used for
	 * testing and is NOT the normal procedure for deleting a job! Call "delete" instead.
	 *
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */

	public static boolean deleteAndRemove(int jobId) throws SQLException {
		Job j = Jobs.get(jobId);
		if (j != null) {
			log.debug("Called deleteAndRemove on the following job");
			log.debug(String.valueOf(jobId));
			log.debug(j.getName());
		}
		boolean success = delete(jobId);
		if (!success) {
			return false;
		}

		success = Jobs.removeJobFromDatabase(jobId);

		return success;
	}

	/**
	 * Jobs are ReadOnly if we are in Migration Mode _and_ this job exists in the
	 * OLD Job Output directory.
	 *
	 * @param jobId
	 * @return true if job is readonly, false otherwise
	 */
	public static boolean isReadOnly(int jobId) {
		return R.MIGRATION_MODE_ACTIVE
		    && getDirectory(jobId).startsWith(R.OLD_JOB_OUTPUT_DIRECTORY);
	}

	/**
	 * Sets the job's 'deleted' column to to true, indicating it has been deleted. Also updates the disk_size and
	 * total_pairs columns to 0
	 *
	 * @param jobId
	 * @return true on success and false otherwise
	 */
	public static boolean setDeletedColumn(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL DeleteJob(?)}");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setDeletedColumn", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Deletes the job with the given id from disk, and sets the "deleted" column in the database jobs table to true.
	 *
	 * @param jobId The ID of the job to delete
	 * @return True on success, false otherwise
	 */
	public static boolean delete(int jobId) throws SQLException {
		Connection con = null;
		CallableStatement procedure = null;

		if (Jobs.isReadOnly(jobId)) return false;

		try {
			//we should kill jobs before deleting  them so no additional pairs are run
			if (!Jobs.isJobComplete(jobId)) {
				Jobs.kill(jobId);
			}
			Jobs.setDeletedColumn(jobId);
			con = Common.getConnection();

			// Remove the jobs stats from the database.
			Jobs.removeCachedJobStats(jobId, con);

			procedure = con.prepareCall("{CALL DeleteAllJobPairsInJob(?)}");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();

			// we should delete on disk second. This takes a long time, and
			// we want users to quickly see that a job has been deleted
			if (!Util.safeDeleteDirectory(getDirectory(jobId))) {
				log.error("there was an error deleting the job directory!");
				return false;
			}


			return true;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Permanently removes a job from the database. This is a helper function and should NOT be called to delete a job!
	 * It will not delete a job on disk
	 *
	 * @return True on success and false otherwise
	 */

	private static boolean removeJobFromDatabase(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL RemoveJobFromDatabase(?)");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("removeJobFromDatabase", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	public static List<JobPair> getJobPairsToBeDeletedFromConfigIds(int jobId, Set<Integer> configIds) {
		Job job = Jobs.getWithSimplePairs(jobId);
		List<JobPair> pairsToDelete = new ArrayList<>();
		List<JobPair> jobPairs = job.getJobPairs();
		// Delete every pair that contains a config in the set of configs.
		for (JobPair pair : jobPairs) {
			if (pair.getStages().size() == 1 && configIds.contains(pair.getPrimaryStage().getConfiguration().getId()
			)) {
				pairsToDelete.add(pair);
			}
		}

		return pairsToDelete;
	}

	/**
	 * Deletes all the job pairs in a job that have a given configuration
	 *
	 * @param jobId the id of the job to delete from.
	 * @param configIds the configuration ids for which we want to delete job pairs.
	 * @author Albert Giegerich
	 */
	public static void deleteJobPairsWithConfigurationsFromJob(int jobId, Set<Integer> configIds) throws SQLException {
		JobPairs.deleteJobPairs(getJobPairsToBeDeletedFromConfigIds(jobId, configIds));
		removeCachedJobStatsForConfigs(jobId, configIds);
	}

	/**
	 * Gets information about the job with the given ID. Job pair information is not returned. Deleted jobs are not
	 * returned.
	 *
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	public static Job get(int jobId) {
		return get(jobId, false);
	}

	/**
	 * Adds the given StageAttributes to the database
	 *
	 * @param attrs The attributes object to add
	 * @param con The open connection to make the call on
	 * @return True on success and false otherwise
	 */

	public static boolean addJobStageAttributes(StageAttributes attrs, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL SetJobStageParams(?,?,?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1, attrs.getJobId());
			procedure.setInt(2, attrs.getStageNumber());
			procedure.setInt(3, attrs.getCpuTimeout());
			procedure.setInt(4, attrs.getWallclockTimeout());
			procedure.setLong(5, attrs.getMaxMemory());
			if (attrs.getSpaceId() == null) {
				procedure.setNull(6, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(6, attrs.getSpaceId());
			}
			if (attrs.getPostProcessor() == null) {
				procedure.setNull(7, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(7, attrs.getPostProcessor().getId());
			}
			if (attrs.getPreProcessor() == null) {
				procedure.setNull(8, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(8, attrs.getPreProcessor().getId());
			}
			if (attrs.getBenchSuffix() == null) {
				procedure.setNull(9, java.sql.Types.VARCHAR);
			} else {
				procedure.setString(9, attrs.getBenchSuffix());
			}
			procedure.setInt(10, attrs.getResultsInterval());
			procedure.setInt(11, attrs.getStdoutSaveOption().getVal());
			procedure.setInt(12, attrs.getExtraOutputSaveOption().getVal());
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addJobStageAttributes", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Adds the given StageAttributes object to the database
	 *
	 * @param attrs The attributes object to add
	 * @return True on success and false otherwise
	 */

	public static boolean addJobStageAttributes(StageAttributes attrs) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return addJobStageAttributes(attrs, con);
		} catch (Exception e) {
			log.error("addJobStageAttributes", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Sets the output benchmarks directory path for a job.
	 *
	 * @param jobId the ID of the job to update the path for.
	 * @param outputBenchmarksDirectory the path to the output benchmarks directory.
	 * @throws SQLException on database error.
	 */
	public static void setOutputBenchmarksPath(final int jobId, final String outputBenchmarksDirectory)
			throws SQLException {
		Common.update("{CALL SetOutputBenchmarksPath(?, ?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setString(2, outputBenchmarksDirectory);
		});
	}

	/**
	 * Gets the output benchmarks directory path for a job.
	 *
	 * @param jobId the ID of the job.
	 * @return the output benchmarks directory path
	 * @throws SQLException on database error.
	 */
	public static Optional<String> getOutputBenchmarksPath(int jobId) throws SQLException {
		return Common.query("{CALL GetOutputBenchmarksPath(?)}", procedure -> procedure.setInt(1, jobId), results -> {
			String path = null;
			if (results.next()) {
				path = results.getString("output_benchmarks_directory_path");
			}
			return Optional.ofNullable(path);
		});
	}

	/**
	 * @param jobId The id of the job to be gotten
	 * @return a job that has job pairs with the simple information included.
	 * @author Albert Giegerich
	 */
	public static Job getWithSimplePairs(int jobId) {
		return get(jobId, false, true);
	}

	public static Job resultsToJob(ResultSet results) throws SQLException {
		Job j = new Job();
		j.setId(results.getInt("jobs.id"));
		j.setUserId(results.getInt("user_id"));
		j.setName(results.getString("name"));
		j.setPrimarySpace(results.getInt("primary_space"));
		j.setPaused(results.getBoolean("paused"));
		j.setCreateTime(results.getTimestamp("created"));
		j.setCompleteTime(results.getTimestamp("completed"));
		j.setCpuTimeout(results.getInt("cpuTimeout"));
		j.setWallclockTimeout(results.getInt("clockTimeout"));
		j.setMaxMemory(results.getLong("maximum_memory"));
		j.setKillDelay(results.getInt("kill_delay"));
		j.setSoftTimeLimit(results.getInt("soft_time_limit"));
		j.setBuildJob(results.getBoolean("buildJob"));
		j.setDescription(results.getString("description"));
		j.setSeed(results.getLong("seed"));
		j.setTotalPairs(results.getInt("total_pairs"));
		j.setDiskSize(results.getLong("disk_size"));
		j.setSuppressTimestamp(results.getBoolean("suppress_timestamp"));
		j.setUsingDependencies(results.getBoolean("using_dependencies"));
		j.setBenchmarkingFramework(BenchmarkingFramework.valueOf(results.getString("benchmarking_framework")));
		j.setOutputBenchmarksPath(results.getString("output_benchmarks_directory_path"));

		final boolean isHighPriority = results.getBoolean("is_high_priority");
		if (isHighPriority) {
			j.setHighPriority();
		} else {
			j.setLowPriority();
		}
		return j;
	}

	/**
	 * Counts how many pairs a user has in total. In other words, sums up the pairs in all jobs created by the user.
	 * Excludes deleted jobs, which may be in the middle of deleting pairs
	 *
	 * @param userId The ID of the user to count for
	 * @return The count, or -1 on error. Answer will be 0 if user does not exist.
	 */
	public static int countPairsByUser(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CountPairsbyUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("total_pairs");
			}
		} catch (Exception e) {
			log.error("countPairsByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	private static Job get(int jobId, boolean includeDeleted, boolean getSimplePairs) {
		final String methodName = "get";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			if (includeDeleted) {
				procedure = con.prepareCall("{CALL GetJobByIdIncludeDeleted(?)}");
			} else {
				procedure = con.prepareCall("{CALL GetJobById(?)}");
			}

			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			if (results.next()) {
				Job j = resultsToJob(results);
				if (getSimplePairs) {
					j.setJobPairs(getPairsSimple(jobId));
				}
				j.setQueue(Queues.get(con, results.getInt("queue_id")));
				j.setStageAttributes(Jobs.getStageAttrsForJob(jobId, con));
				return j;
			}
		} catch (Exception e) {
			log.error("get", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		log.debug(methodName, "Could not find job with id: " + jobId);
		return null;
	}

	/**
	 * Gets information about the job with the given ID. Job pair information is not returned
	 *
	 * @param jobId The ID of the job in question
	 * @return The Job object that represents the job with the given ID
	 */
	private static Job get(int jobId, boolean includeDeleted) {
		return get(jobId, includeDeleted, false);
	}
	
	/**
	 * Gets all the SolverStats objects for a given job in the given space hierarchy
	 *
	 * This version uses a stored procedure (at its base) that includes configs marked as deleted. Used to construct
	 * the solver summary table in the job space view.
	 * Alexander Brown 9/20
	 *
	 * @param space The JobSpace root  in question
	 * @param stageNumber The ID of the stage to get data for
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list containing every SolverStats for the given job where the solvers reside in the given space
	 * @author Eric Burns
	 */

	public static Collection<SolverStats> getAllJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(
			JobSpace space, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize, boolean includeUnknown
	) {
		final int spaceId = space.getId();
		Collection<SolverStats> stats;
		
		stats = getCachedJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(spaceId, stageNumber, primitivesToAnonymize, includeUnknown);
		if (stats != null && !stats.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (SolverStats s : stats) {
				sb.append(s.toString() + "\n");
			}
			log.debug("stats already cached in database:\n" + sb.toString());
			return stats;
		}
		
		int jobId = space.getJobId();

		//we will cache the stats only if the job is complete
		boolean isJobComplete = Jobs.isJobComplete(jobId);

		//otherwise, we need to compile the stats
		log.debug("stats not present in database -- compiling stats now");
		List<JobPair> pairs = getJobPairsInJobSpaceHierarchy(spaceId, primitivesToAnonymize);

		//compiles pairs into solver stats
		stats = processPairsToSolverStats(jobId, pairs, includeUnknown);
		for (SolverStats s : stats) {
			s.setJobSpaceId(spaceId);
		}

		if (isJobComplete) {
			saveStats(jobId, stats, includeUnknown);
		}
		

		//next, we simply filter down the stats to the ones for the given stage
		stats.removeIf((s) -> s.getStageNumber() != stageNumber);

		return stats;
	}

	/**
	 * @param job The job to make the mapping for
	 * @param stageNumber Stage number to get mapping for
	 * @return Map from job space ID to solver stats objects
	 */
	public static Map<Integer, Collection<SolverStats>> buildJobSpaceIdToSolverStatsMapWallCpuTimesRounded(
			Job job, int stageNumber
	) {
		Map<Integer, Collection<SolverStats>> outputMap = buildJobSpaceIdToSolverStatsMap(job, stageNumber);
		for (Integer jobspaceId : outputMap.keySet()) {
			Collection<SolverStats> statsList = outputMap.get(jobspaceId);
			for (SolverStats stats : statsList) {
				stats.setWallTime(Math.round(stats.getWallTime() * 100) / 100.0);
				stats.setCpuTime(Math.round(stats.getCpuTime() * 100) / 100.0);
			}
		}
		return outputMap;
	}

	/**
	 * Builds a mapping of job space ID's to the stats for the solvers in that job space.
	 *
	 * @param job job that owns the job spaces to work on
	 * @param stageNumber The stage to filter solver stats by
	 * @return a mapping of job space ID's to the stats for the solvers in that job space
	 * @author Albert Giegerich
	 * @see org.starexec.data.database.JobPairs#buildJobSpaceIdToJobPairMapForJob
	 */
	public static Map<Integer, Collection<SolverStats>> buildJobSpaceIdToSolverStatsMap(Job job, int stageNumber) {
		int primaryJobSpaceId = job.getPrimarySpace();
		Map<Integer, Collection<SolverStats>> jobSpaceIdToSolverStatsMap = new HashMap<>();
		List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(primaryJobSpaceId, true);
		jobSpaces.add(Spaces.getJobSpace(primaryJobSpaceId));
		for (JobSpace jobspace : jobSpaces) {
			int jobspaceId = jobspace.getId();
			Collection<SolverStats> stats =
					getAllJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(jobspace, stageNumber, PrimitivesToAnonymize.NONE,false);
					// tmp
					log.debug( "\n\nTRIGGERED IN buildJobSpaceIdToSolverStatsMap(), Jobs.java:1214\n" );
			jobSpaceIdToSolverStatsMap.put(jobspaceId, stats);
		}
		return jobSpaceIdToSolverStatsMap;
	}

	/**
	 * Gets a list of jobs belonging to a space (without its job pairs but with job pair statistics)
	 *
	 * @param spaceId The id of the space to get jobs for
	 * @return A list of jobs existing directly in the space
	 * @author Tyler Jensen
	 */
	public static List<Job> getBySpace(int spaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpaceJobsById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<>();

			while (results.next()) {
				jobs.add(resultsToJob(results));
			}
			return jobs;
		} catch (Exception e) {
			log.error("getBySpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Get all the jobs enqueued on a specific queue
	 *
	 * @param queueId Id of the queue we are looking for
	 * @return a list of Jobs on the Queue
	 */
	public static List<Job> getByQueueId(int queueId) throws SQLException {
            log.debug("getByQueueId begins " + queueId);
            List<Job> r = Common.query("{CALL GetQueueJobsById(?)}", procedure -> procedure.setInt(1, queueId),
                                       Jobs::getJobsForNextPage
                                       );
            log.debug("getByQueueId ends ");
            return r;
	}

	/**
	 * Get all the jobs belong to a specific user
	 *
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 */
	public static List<Job> getByUserId(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUserJobsById(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			List<Job> jobs = new LinkedList<>();

			while (results.next()) {
				jobs.add(resultsToJob(results));
			}

			return jobs;
		} catch (Exception e) {
			log.error("getByUserId", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Gets the number of Jobs in a given space
	 *
	 * @param spaceId the id of the space to count the Jobs in
	 * @return the number of Jobs
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		return getCountInSpace(spaceId, "");
	}

	/**
	 * Gets the number of Jobs in a given space that match a given query
	 *
	 * @param spaceId the id of the space to count the Jobs in
	 * @param query The query to match the jobs against
	 * @return the number of Jobs
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobCountBySpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, query);
			results = procedure.executeQuery();
			int jobCount = 0;
			if (results.next()) {
				jobCount = results.getInt("jobCount");
			}
			return jobCount;
		} catch (Exception e) {
			log.error("getCountInSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}

	/**
	 * Retrieves a job from the database as well as its job pairs that were completed after "since" and its
	 * queue/processor info
	 *
	 * @param jobId The id of the job to get information for
	 * @param since The completion ID after which to get job pairs
	 * @return A job object containing information about the requested job, or null on failure
	 * @author Eric Burns
	 */
	public static Job getDetailed(int jobId, int since) {
		return getDetailed(jobId, since, true);
	}

	private static Job getDetailed(int jobId, int since, boolean getCompletedPairsOnly) {
		final String method = "getDetailed";
		log.info("getting detailed info for job " + jobId);
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobById(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();

			if (!results.next()) {
				return null;
			}

			final Job j = resultsToJob(results);
			j.setStageAttributes(Jobs.getStageAttrsForJob(jobId, con));
			if (getCompletedPairsOnly) {
				log.debug(method, "Getting job pairs for job with id=" + jobId + " since completionID=" + since);
				j.setJobPairs(Jobs.getNewCompletedPairsDetailed(j.getId(), since));
			} else {
				j.setJobPairs(Jobs.getAllPairs(jobId));
			}

			return j;
		} catch (Exception e) {
			log.error("getDetailed", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return null;
	}

	/**
	 * Gets a status description of a stage.
	 *
	 * @param stage the stage to get the status code from.
	 * @return the status code of the input stage.
	 * @author Albert Giegerich
	 */
	public static String getStatusFromStage(JoblineStage stage) {
		// 0 for solved 1 for wrong
		int correctCode = JobPairs.isPairCorrect(stage);
		StatusCode statusCode = stage.getStatus().getCode();
		if (correctCode == 0) {
			return "solved";
		} else if (correctCode == 1) {
			return "wrong";
		} else if (statusCode.statIncomplete()) {
			return "incomplete";
		} else if (statusCode.failed()) {
			return "failed";
		} else if (statusCode.resource()) {
			// Resources (time/memory) ran out.
			return "resource";
		} else {
			return "unknown";
		}
	}

	/**
	 * Returns the absolute filepath to the directory containing this job's output
	 *
	 * @param jobId The job to get the filepath for
	 * @return A string representing the path to the output directory
	 * @author Eric Burns
	 */
	public static String getDirectory(int jobId) {
		File file;
		String job = String.valueOf(jobId);
		/* If there exists an OLD_JOB_OUTPUT_DIRECTORY, we will check for jobs
		 * there first. */
		if (R.MIGRATION_MODE_ACTIVE) {
			file = new File(R.OLD_JOB_OUTPUT_DIRECTORY, job);
			if (file.exists()) {
				return file.getAbsolutePath();
			}
		}
		/* We could not find this job in OLD_JOB_OUTPUT_DIRECTORY, so we will
		 * return the default JOB_OUTPUT_DIRECTORY path */
		file = new File(R.JOB_OUTPUT_DIRECTORY, job);
		return file.getAbsolutePath();
	}

	/**
	 * Returns the absolute path to the directory containing all the log files for the given job
	 *
	 * @param jobId The ID of the job to get the log path for
	 * @return The absolute path as a string
	 */
	public static String getLogDirectory(int jobId) {
		File file;
		String job = String.valueOf(jobId);
		/* If there exists an OLD_JOB_LOG_DIRECTORY, we will check for jobs
		 * there first. */
		if (R.OLD_JOB_LOG_DIRECTORY != null) {
			file = new File(R.OLD_JOB_LOG_DIRECTORY, job);
			if (file.exists()) {
				return file.getAbsolutePath();
			}
		}
		/* We could not find this job in OLD_JOB_LOG_DIRECTORY, so we will
		 * return the default JOB_LOG_DIRECTORY path */
		file = new File(R.JOB_LOG_DIRECTORY, job);
		return file.getAbsolutePath();
	}

	private static List<JobPair> getPairsHelper(Connection con, String sqlMethod, int jobId) throws SQLException {
		return Common.queryUsingConnection(con, sqlMethod, procedure -> procedure.setInt(1, jobId), results -> {
			List<JobPair> returnList = new LinkedList<>();
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setId(results.getInt("id"));
				jp.setBackendExecId(results.getInt("sge_id"));
				returnList.add(jp);
			}
			return returnList;
		});
	}

	/**
	 * Gets all enqueued job pairs. Only populates the pair ID and the sge ID!
	 *
	 * @param con The connection to make the query on
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getEnqueuedPairs(Connection con, int jobId) throws SQLException {
		log.debug("getEnqueuePairs2 beginning...");
		return getPairsHelper(con, "{CALL GetEnqueuedJobPairsByJob(?)}", jobId);
	}

	/**
	 * Gets all enqueued job pairs. Only populates the pair ID and the sge ID!
	 *
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given queue.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getEnqueuedPairs(int jobId) throws SQLException {
		Connection con = null;

		try {
			con = Common.getConnection();
			return Jobs.getEnqueuedPairs(con, jobId);
		} catch (SQLException e) {
			log.error("getEnqueuedPairsDetailed", "jobId: " + jobId, e);
			throw e;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Retrieves the given job, even if it has been marked as "deleted" in the database. Deep data like job pairs are
	 * not populated
	 *
	 * @param jobId The ID of the job to retrieve
	 * @return The job if it could be found, or null if it could not
	 */
	public static Job getIncludeDeleted(int jobId) {
		return get(jobId, true);
	}

	/**
	 * Gets all the the attributes for every job pair in a job, and returns a HashMap mapping pair IDs a map of their
	 * stages to the stage's attributes
	 *
	 * @param con The connection to make the query on
	 * @param jobId The ID of the job to get attributes of
	 * @return A HashMap mapping pair IDs to properties. Some values may be null
	 * @author Eric Burns
	 */
	protected static HashMap<Integer, HashMap<Integer, Properties>> getJobAttributes(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		log.debug("Getting all attributes for job with ID = " + jobId);
		try {
			procedure = con.prepareCall("{CALL GetJobAttrs(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("getJobAttributes", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all attributes for every job pair associated with the given job
	 *
	 * @param jobId The ID of the job in question
	 * @return A HashMap mapping integer job-pair IDs to hashmaps that themselves map jobpair_stage_data ids to
	 * Properties for that stage
	 * @author Eric Burns
	 */
	public static HashMap<Integer, HashMap<Integer, Properties>> getJobAttributes(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getJobAttributes(con, jobId);
		} catch (Exception e) {
			log.error("getJobAttributes", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets the number of Jobs in the whole system
	 *
	 * @return The number of jobs in the system
	 * @author Wyatt Kaiser
	 */

	public static int getJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobCount()}");
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error("getJobCount", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}

	/**
	 * Get the total count of the jobs belong to a specific user
	 *
	 * @param userId Id of the user we are looking for
	 * @return The count of the jobs
	 * @author Ruoyu Zhang
	 */
	public static int getJobCountByUser(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobCountByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error("getJobCountByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;
	}

	/**
	 * Get the total count of the jobs belong to a specific user that match a specific query
	 *
	 * @param userId Id of the user we are looking for
	 * @param query The query to match the jobs against
	 * @return The count of the jobs
	 * @author Eric Burns
	 */
	public static int getJobCountByUser(int userId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobCountByUserWithQuery(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error("getJobCountByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return 0;
	}

	/**
	 * Returns the number of job pairs that exist for a given job in a given space that have the given stage
	 *
	 * @param jobSpaceId The ID of the job space containing the paris to count
	 * @param stageNumber The stage number. If <=0, means the primary stage
	 * @return the number of job pairs for the given job or -1 on failure
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpaceByStage(int jobSpaceId, int stageNumber) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetJobPairCountInJobSpace(?,?)}");

			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, stageNumber);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("jobPairCount");
			}
		} catch (Exception e) {
			log.error("getJobPairCountInJobSpaceByStage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return -1;
	}

	/**
	 * Returns the number of job pairs that exist for a given job in a given space
	 *
	 * @param jobSpaceId The ID of the job space containing the paris to count
	 * @param query The query to match the job pairs against
	 * @param stageNumber The stage number to consider
	 * @return the number of job pairs for the given job
	 * @author Eric Burns
	 */
	public static int getJobPairCountInJobSpaceByStage(int jobSpaceId, String query, int stageNumber) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		int jobPairCount = 0;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairCountByJobInJobSpaceWithQuery(?, ?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setString(2, query);
			procedure.setInt(3, stageNumber);
			results = procedure.executeQuery();
			if (results.next()) {
				jobPairCount = results.getInt("jobPairCount");
			}
		} catch (Exception e) {
			log.error("getJobPairCountInJobSpaceByStage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return jobPairCount;
	}

	/**
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where all the job
	 * pairs
	 * are in the given job space hierarchy and were operated on by the configuration with the given config ID
	 *
	 * @param query A DataTablesQuery object
	 * @param jobSpaceId The ID of the root job space of the job space hierarchy to get data for
	 * @param configId1 The ID of the first configuration of the comparision
	 * @param configId2 The ID of the second configuration of the comparison
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param stageNumber The stage number ot use for the comparison
	 * @param totals A size 2 int array that, upon return, will contain in the first slot the total number of pairs and
	 * in the second slot the total number of pairs after filtering
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object
	 * @author Eric Burns
	 */
	public static List<SolverComparison> getSolverComparisonsForNextPageByConfigInJobSpaceHierarchy(
			DataTablesQuery query, int jobSpaceId, int configId1, int configId2, int[] totals, boolean wallclock,
			int stageNumber
	) {
		List<JobPair> pairs = Jobs.getJobPairsInJobSpaceHierarchy(jobSpaceId, PrimitivesToAnonymize.NONE);
		List<JobPair> pairs1 = new ArrayList<>();
		List<JobPair> pairs2 = new ArrayList<>();
		for (JobPair jp : pairs) {
			JoblineStage stage = jp.getStageFromNumber(stageNumber);
			if (stage == null || stage.isNoOp()) {
				continue;
			}
			if (stage.getConfiguration().getId() == configId1) {
				pairs1.add(jp);
			} else if (stage.getConfiguration().getId() == configId2) {
				pairs2.add(jp);
			}
		}
		pairs1 = JobPairs.filterPairsByType(pairs1, "complete", stageNumber);
		pairs2 = JobPairs.filterPairsByType(pairs2, "complete", stageNumber);
		List<SolverComparison> comparisons = new ArrayList<>();
		HashMap<Integer, JobPair> benchesToPairs = new HashMap<>();
		for (JobPair jp : pairs1) {
			benchesToPairs.put(jp.getBench().getId(), jp);
		}
		for (JobPair jp : pairs2) {
			if (benchesToPairs.containsKey(jp.getBench().getId())) {
				try {
					comparisons.add(new SolverComparison(benchesToPairs.get(jp.getBench().getId()), jp));
				} catch (Exception e) {
					log.error("getSolverComparisonsForNextPageByConfigInJobSpaceHierarchy", e);
				}
			}
		}

		totals[0] = comparisons.size();
		comparisons = JobPairs.filterComparisons(comparisons, query.getSearchQuery());

		totals[1] = comparisons.size();
		SolverComparisonComparator compare =
				new SolverComparisonComparator(query.getSortColumn(), wallclock, query.isSortASC(), stageNumber);
		return Util.handlePagination(comparisons, compare, query.getStartingRecord(), query.getNumRecords());
	}

	/**
	 * Given a list of job pairs, filters and sorts them according to the given parameters and returns the set to
	 * display
	 *
	 * @param pairs The pairs to filter and sort
	 * @param query Parameters from data table describing which pairs to get in which order
	 * @param type The "type" to filter by, where the type refers to the different columns of the solver stats table
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param stageNumber The stage number containing the relevant data, or 0 for the primary stage
	 * @param totals A size 2 array that, on exit, will contain the total number of pairs after filtering by type and
	 * the total number of pairs after filtering by the query
	 * @return The list of job pairs to display in the next page
	 */
	public static List<JobPair> getJobPairsForNextPage(
			List<JobPair> pairs, DataTablesQuery query, String type, boolean wallclock, int stageNumber, int[] totals
	) {
		pairs = JobPairs.filterPairsByType(pairs, type, stageNumber);

		totals[0] = pairs.size();

		pairs = JobPairs.filterPairs(pairs, query.getSearchQuery(), stageNumber);

		totals[1] = pairs.size();
		int indexOfColumnSortedBy = query.getSortColumn();
		if (!wallclock && indexOfColumnSortedBy == 4) {
			indexOfColumnSortedBy = 8;
		}
		JobPairComparator compare = new JobPairComparator(indexOfColumnSortedBy, stageNumber, query.isSortASC());
		return Util.handlePagination(pairs, compare, query.getStartingRecord(), query.getNumRecords());
	}

	/**
	 * Returns a count of the number of job pairs that satisfy the requirements of the given attributes
	 *
	 * @param jobSpaceId The ID of the job space the pairs must be in
	 * @param configId The ID of the configuration the pairs must be using during the given stage
	 * @param type The "type" of the pairs as defined by the columns of the solver stats table
	 * @param stageNumber The stage number of the stage to check
	 * @return The integer number of pairs, or -1 on error
	 */
	public static int getCountOfJobPairsByConfigInJobSpaceHierarchy(
			int jobSpaceId, int configId, String type, int stageNumber
	) {
		return getCountOfJobPairsByConfigInJobSpaceHierarchy(jobSpaceId, configId, type, "", stageNumber);
	}

	/**
	 * Counts the number of job pairs that are in a given job space and use the given configuration and are also of the
	 * given "type", which here corresponds to the different columns on the solver stats table in the job details page
	 *
	 * @param jobSpaceId The ID of the job space to get pairs for
	 * @param configId The ID of the configuration we are concerned with
	 * @param type The "type", defined as in the different columns in the solver stats table
	 * @param query A query to filter the columns by
	 * @param stageNumber The stage number to check
	 * @return The total number of pairs that satisfy the given attributes, or -1 on error
	 */
	public static int getCountOfJobPairsByConfigInJobSpaceHierarchy(
			int jobSpaceId, int configId, String type, String query, int stageNumber
	) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CountJobPairsInJobSpaceHierarchyByType(?,?,?,?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, configId);
			procedure.setString(3, type);
			procedure.setString(4, query);
			procedure.setInt(5, stageNumber);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("getCountOfJobPairsByConfigInJobSpaceHierarchy", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Retrieves the job pairs necessary to fill the next page of a javascript datatable object, where all the job
	 * pairs
	 * are in the given space and were operated on by the configuration with the given config ID in the given stage
	 *
	 * @param query DataTablesQuery instance
	 * @param jobSpaceId The job space that contains the job pairs
	 * @param configId The ID of the configuration responsible for the job pairs
	 * @param type The type of the pairs, as defined by the columns of the solver stats table
	 * @param stageNumber The stage number to get data for
	 * @return A list of job pairs for the given job necessary to fill  the next page of a datatable object
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsForNextPageByConfigInJobSpaceHierarchy(
			DataTablesQuery query, int jobSpaceId, int configId, String type, int stageNumber
	) {

		return Jobs.getJobPairsForTableInJobSpaceHierarchy(jobSpaceId, query, configId, stageNumber, type);
	}

	/**
	 * If the given string is null, returns a placeholder string. Otherwise, returns the given string
	 *
	 * @param value The string to check
	 * @return The given string unless it is null, and -- otherwise
	 */
	public static String getPropertyOrPlaceholder(String value) {
		if (value == null) {
			return "--";
		}
		return value;
	}

	/**
	 * Gets all the JobPairs in a given job space that were solved by every solver/configuration pair in that space
	 *
	 * @param jobSpaceId The ID of the job space to get the pairs for
	 * @param stageNumber The stage number to get data for
	 * @param primitivesToAnonymize Object indicating which of solvers and benchmarks to anonymize
	 * @return All the job pairs in the given job space that are "synchronized" as defined above
	 */
	public static List<JobPair> getSynchronizedPairsInJobSpace(
			int jobSpaceId, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize
	) {

		HashSet<String> solverConfigPairs =
				new HashSet<>(); // will store all the solver/configuration pairs so we know how many there are
		HashMap<Integer, Integer> benchmarksCount = new HashMap<>(); //will store the number of pairs every benchmark
		// has
		try {
			//first, get all the completed pairs in the space
			List<JobPair> pairs = Jobs.getJobPairsInJobSpace(jobSpaceId, stageNumber, primitivesToAnonymize);
			pairs = JobPairs.filterPairsByType(pairs, "complete", 1); //1 because we get only one stage above

			//then, filter them down to the synced pairs
			for (JobPair p : pairs) {
				solverConfigPairs.add(p.getPrimarySolver().getId() + ":" + p.getPrimaryConfiguration().getId());
				if (benchmarksCount.containsKey(p.getBench().getId())) {
					benchmarksCount.put(p.getBench().getId(), 1 + benchmarksCount.get(p.getBench().getId()));
				} else {
					benchmarksCount.put(p.getBench().getId(), 1);
				}
			}

			//now, we exclude pairs that have benchmarks where the benchmark count is not equal to the solver/config
			// count

			List<JobPair> returnList = new ArrayList<>();
			int solverCount = solverConfigPairs.size();
			for (JobPair p : pairs) {
				if (benchmarksCount.get(p.getBench().getId()) == solverCount) {
					returnList.add(p);
				}
			}
			return returnList;
		} catch (Exception e) {
			log.error("getSynchronizedPairsInJobSpace", e);
		}
		return null;
	}

	/**
	 * Gets the JobPairs necessary to make the next page of a DataTable of synchronized job pairs in a specific job
	 * space
	 *
	 * @param query Parameters from data table describing which pairs to get in which order
	 * @param jobSpaceId The ID of the job space containing the pairs
	 * @param wallclock True if we are using wallclock time and false to use CPU time
	 * @param stageNumber The stage number to get results for
	 * @param primitivesToAnonymize Object indicating which of solvers and benchmarks to anonymize
	 * @param totals Must be a size 2 array. The first slot will have the number of results before the query, and the
	 * second slot will have the number of results after the query
	 * @return The job pairs needed to populate the page
	 */
	public static List<JobPair> getSynchronizedJobPairsForNextPageInJobSpace(
			DataTablesQuery query, int jobSpaceId, boolean wallclock, int stageNumber, int[] totals,
			PrimitivesToAnonymize primitivesToAnonymize
	) {
		List<JobPair> pairs = Jobs.getSynchronizedPairsInJobSpace(jobSpaceId, stageNumber, primitivesToAnonymize);
		return getJobPairsForNextPage(pairs, query, "all", wallclock, stageNumber, totals);
	}

	/**
	 * Given the index of a column in the job pairs table on the client side, returns the name of the SQL column we
	 * need
	 * to sort by
	 *
	 * @param orderIndex The index of the client side datatable column we are sorting on
	 * @param wallclock Whether to use wallclock time or cpu time if we are sorting on time.
	 * @return The SQL column name
	 */
	private static String getJobPairOrderColumn(int orderIndex, boolean wallclock) {
		switch (orderIndex) {
		case 0:
			return "job_pairs.bench_name";
		case 1:
			return "jobpair_stage_data.solver_name";
		case 2:
			return "jobpair_stage_data.config_name";
		case 3:
			return "jobpair_stage_data.status_code";
		case 4:
			if (wallclock) {
				return "jobpair_stage_data.wallclock";
			} else {
				return "jobpair_stage_data.cpu";
			}
		case 5:
			return "result";
		case 6:
			return "job_pairs.id";
		case 7:
			// the - sign is because we want null values last, so we reverse the ASC/ DESC sign and add a -
			return "-completion_id";
		}

		return "job_pairs.benchmark_name";
	}

	/**
	 * Gets the minimal number of Job Pairs necessary in order to service the client's request for the next page of Job
	 * Pairs in their DataTables object
	 *
	 * @param query Parameters from data table describing which pairs to get in which order
	 * @param jobSpaceId The ID of the job space containing the pairs in question
	 * @param stageNumber The stage number to get data for
	 * @param wallclock True to use wallclock time and false to use CPU time
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return a list of 10, 25, 50, or 100 Job Pairs containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */

	public static List<JobPair> getJobPairsForNextPageInJobSpace(
			DataTablesQuery query, int jobSpaceId, int stageNumber, boolean wallclock,
			PrimitivesToAnonymize primitivesToAnonymize
	) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		String searchQuery = query.getSearchQuery();
		if (searchQuery == null) {
			searchQuery = "";
		}
		int jobId = Spaces.getJobSpace(jobSpaceId).getId();
		try {
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_IN_SPACE_QUERY,
			                                                            getJobPairOrderColumn(query.getSortColumn(),
			                                                                                  wallclock
			                                                            ), query
			);
			con = Common.getConnection();
			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setString("query", searchQuery);
			procedure.setInt("stageNumber", stageNumber);
			procedure.setInt("jobSpaceId", jobSpaceId);
			results = procedure.executeQuery();
			return getJobPairsForDataTable(jobId, results, false, false, primitivesToAnonymize);
		} catch (Exception e) {
			log.error("getJobPairsForNextPageInJobSpace", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Gets benchmarks attributes with a specific key for all benchmarks used by a given job
	 *
	 * @param jobId The job in question
	 * @param attrKey The string key of the attribute to return
	 * @return A hashmap mapping benchmark ids to attribute values
	 */
	public static HashMap<Integer, String> getAllAttrsOfNameForJob(int jobId, String attrKey) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAttrsOfNameForJob(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, attrKey);
			results = procedure.executeQuery();
			HashMap<Integer, String> idsToValues = new HashMap<>();

			while (results.next()) {
				idsToValues.put(results.getInt("job_pairs.bench_id"), results.getString("attr_value"));
			}
			log.debug("found this number of attrs = " + idsToValues.size());
			return idsToValues;
		} catch (Exception e) {
			log.error("getAllAttrsOfNameForJob", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Given a list of job pairs and a ResultSet that contains stages for those pairs, populates the pairs with their
	 * stages
	 *
	 * @param pairs The pairs that have stages contained in the given result set
	 * @param results The ResultSet containing stages
	 * @param getExpectedResult True to include the expected result column and false otherwise
	 * @param primitivesToAnonymize an enum describing which (if any) primitive names should be anonymized.
	 * @return True if the pairs had their stages populated correctly and false otherwise
	 */
	public static boolean populateJobPairStages(
			List<JobPair> pairs, ResultSet results, boolean getExpectedResult,
			PrimitivesToAnonymize primitivesToAnonymize
	) {

		HashMap<Integer, Solver> solvers = new HashMap<>();
		HashMap<Integer, Configuration> configs = new HashMap<>();
		Integer id;
		Solver solve = null;
		Configuration config = null;
		HashMap<Integer, JobPair> idsToPairs = new HashMap<>();
		try {
			for (JobPair pair : pairs) {
				idsToPairs.put(pair.getId(), pair);
			}

			//every row in this resultset is a single stage
			while (results.next()) {

				JobPair jp = idsToPairs.get(results.getInt("job_pairs.id"));
				if (jp == null) {
					log.error("could not get a pair for id = " + results.getInt("job_pairs.id"));
					log.error("id found in mapping = " + idsToPairs.containsKey(results.getInt("job_pairs.id")));
					continue;
				}
				JoblineStage stage = new JoblineStage();
				stage.setStageNumber(results.getInt("stage_number"));
				stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
				stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
				stage.setStageId(results.getInt("jobpair_stage_data.stage_id"));
				stage.getStatus().setCode(results.getInt("jobpair_stage_data.status_code"));
				stage.setMaxVirtualMemory(results.getDouble("max_vmem"));
				//everything below this line is in a stage
				id = results.getInt("jobpair_stage_data.solver_id");
				//means it was null in SQL
				if (id == 0) {
					stage.setNoOp(true);
				} else {
					if (!solvers.containsKey(id)) {

						solve = new Solver();
						solve.setId(id);
						if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
							solve.setName(results.getString("anon_solver_name"));
						} else {
							solve.setName(results.getString("jobpair_stage_data.solver_name"));
						}
						solvers.put(id, solve);
					}
					stage.setSolver(solvers.get(id));


					id = results.getInt("jobpair_stage_data.config_id");

					if (!configs.containsKey(id)) {
						config = new Configuration();
						config.setId(id);
						if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
							config.setName(results.getString("anon_config_name"));
						} else {
							config.setName(results.getString("jobpair_stage_data.config_name"));
						}
						configs.put(id, config);
					}
					stage.getSolver().addConfiguration(configs.get(id));
					stage.setConfiguration(configs.get(id));

					Properties p = new Properties();
					String result = results.getString("result");
					if (result != null) {
						p.put(R.STAREXEC_RESULT, result);
					}
					if (getExpectedResult) {
						String expected = results.getString("expected");
						if (expected != null) {
							p.put(R.EXPECTED_RESULT, expected);
						}
					}

					stage.setAttributes(p);
				}


				jp.addStage(stage);
			}

			return true;
		} catch (Exception e) {
			log.error("populateJobPairStages", e);
		}

		return false;
	}

	/**
	 * Returns all of the job pairs in a given job space, populated with all the fields necessary to display in a
	 * SolverStats table. Only the given stage is returned
	 *
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param stageNumber The stage number to get data for
	 * @param primitivesToAnonymize Object indicating which of solvers and benchmarks to anonymize
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpace(
			int jobSpaceId, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize
	) {

		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called getJobPairsInJobSpace with jobSpaceId = " + jobSpaceId);
		try {
			int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();
			long a = System.currentTimeMillis();
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsInJobSpace(?,?,?)}");

			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, jobId);
			procedure.setInt(3, stageNumber);
			results = procedure.executeQuery();
			log.debug("executing query 1 took " + (System.currentTimeMillis() - a));
			List<JobPair> pairs = processStatResults(results, true, primitivesToAnonymize);
			log.debug("processing query 1 took " + (System.currentTimeMillis() - a));


			return pairs;
		} catch (Exception e) {
			log.error("getJobPairsInJobSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Returns all of the job pairs in a given job space hierarchy, populated with all the fields necessary to display
	 * in a SolverStats table. All job pair stages are obtained
	 *
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpaceHierarchy(
			int jobSpaceId, PrimitivesToAnonymize primitivesToAnonymize
	) {
		return getJobPairsInJobSpaceHierarchy(jobSpaceId, null, primitivesToAnonymize);
	}

	/**
<<<<<<< HEAD
	 * Returns all of the successfully completed job pairs in a given job space hierarchy, populated with all the fields necessary to display
=======
	 
   
>>>>>>> 028325927bb56db6ee1c5404fffb3ef4c13ffdb2
	 * in a SolverStats table. All job pair stages are obtained
	 * This alternate version is to fix the job graphs, Alexander Brown 6/21
	 *
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getSuccessfullyCompletedJobPairsInJobSpaceHierarchy(
			int jobSpaceId, PrimitivesToAnonymize primitivesToAnonymize
	) {
		return getSuccessfullyCompletedJobPairsInJobSpaceHierarchy(jobSpaceId, null, primitivesToAnonymize);
	}

	/**
	 * Returns all of the job pairs in a given job space hierarchy, populated with all the fields necessary to display
	 * in a SolverStats table. All job pair stages are obtained.
	 *
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param since If null, all pairs in the hierarchy are returned. Otherwise, only pairs that have a completion ID
	 * greater than since are returned
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns
	 */
	public static List<JobPair> getJobPairsInJobSpaceHierarchy(
			int jobSpaceId, Integer since, PrimitivesToAnonymize primitivesToAnonymize
	) {
		final String methodName = "getJobPairsInJobSpaceHierarchy";
		log.entry(methodName);
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called with jobSpaceId = " + jobSpaceId);
		log.debug(
				methodName,
				"primitivesToAnonymize equals " + AnonymousLinks.getPrimitivesToAnonymizeName(primitivesToAnonymize)
		);
		try {
			Spaces.updateJobSpaceClosureTable(jobSpaceId);

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsInJobSpaceHierarchy(?,?)}");

			procedure.setInt(1, jobSpaceId);
			if (since == null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(2, since);
			}
			results = procedure.executeQuery();

			List<JobPair> pairs = processStatResults(results, false, primitivesToAnonymize);


			Common.safeClose(procedure);
			Common.safeClose(results);
			procedure = con.prepareCall("{CALL GetJobPairStagesInJobSpaceHierarchy(?,?)}");
			procedure.setInt(1, jobSpaceId);
			if (since == null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(2, since);
			}
			results = procedure.executeQuery();
			if (populateJobPairStages(pairs, results, true, primitivesToAnonymize)) {
				return pairs;
			}
		} catch (Exception e) {
			log.error(methodName, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Returns all of the successfully completed job pairs in a given job space hierarchy, populated with all the fields necessary to display
	 * in a SolverStats table. All job pair stages are obtained.
	 *
	 * @param jobSpaceId The space ID of the space containing the solvers to get stats for
	 * @param since If null, all pairs in the hierarchy are returned. Otherwise, only pairs that have a completion ID
	 * greater than since are returned
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list of job pairs for the given job for which the solver is in the given space
	 * @author Eric Burns (wrote original), Alexander Brown
	 */
	public static List<JobPair> getSuccessfullyCompletedJobPairsInJobSpaceHierarchy(
			int jobSpaceId, Integer since, PrimitivesToAnonymize primitivesToAnonymize
	) {
		final String methodName = "getSuccessfullyCompletedJobPairsInJobSpaceHierarchy";
		log.entry(methodName);
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		log.debug("called with jobSpaceId = " + jobSpaceId);
		log.debug(
				methodName,
				"primitivesToAnonymize equals " + AnonymousLinks.getPrimitivesToAnonymizeName(primitivesToAnonymize)
		);
		try {
			Spaces.updateJobSpaceClosureTable(jobSpaceId);

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSuccessfullyCompletedJobPairsInJobSpaceHierarchy(?,?)}");

			procedure.setInt(1, jobSpaceId);
			if (since == null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(2, since);
			}
			results = procedure.executeQuery();

			List<JobPair> pairs = processStatResults(results, false, primitivesToAnonymize);


			Common.safeClose(procedure);
			Common.safeClose(results);
			procedure = con.prepareCall("{CALL GetSuccessfullyCompletedJobPairStagesInJobSpaceHierarchy(?,?)}");
			procedure.setInt(1, jobSpaceId);
			if (since == null) {
				procedure.setNull(2, java.sql.Types.INTEGER);
			} else {
				procedure.setInt(2, since);
			}
			results = procedure.executeQuery();
			if (populateJobPairStages(pairs, results, true, primitivesToAnonymize)) {
				return pairs;
			}
		} catch (Exception e) {
			log.error(methodName, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Makes all the job pair objects from a ResultSet formed from querying the database for fields needed in a job
	 * pairs table. Populates exactly 1 stage, whichever was returned by the query
	 *
	 * @param jobId The ID of the job containing all these pairs
	 * @param results
	 * @return The list of job pairs or null on failure
	 */

	private static List<JobPair> getJobPairsForDataTable(
			int jobId, ResultSet results, boolean includeExpected, boolean includeCompletion,
			PrimitivesToAnonymize primitivesToAnonymize
	) {
		List<JobPair> pairs = new ArrayList<>();
		try {
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				jp.setId(results.getInt("id"));
				JoblineStage stage = new JoblineStage();
				stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
				stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
				stage.setStageNumber(results.getInt("jobpair_stage_data.stage_number"));
				jp.addStage(stage);
				Benchmark bench = jp.getBench();
				bench.setId(results.getInt("bench_id"));

				if (AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
					bench.setName(results.getString("anon_bench_name"));
				} else {
					bench.setName(results.getString("bench_name"));
				}

				jp.getPrimarySolver().setId(results.getInt("jobpair_stage_data.solver_id"));
				jp.getPrimaryConfiguration().setId(results.getInt("jobpair_stage_data.config_id"));

				if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
					jp.getPrimarySolver().setName(results.getString("anon_solver_name"));
					jp.getPrimaryConfiguration().setName(results.getString("anon_config_name"));
				} else {
					jp.getPrimarySolver().setName(results.getString("jobpair_stage_data.solver_name"));
					jp.getPrimaryConfiguration().setName(results.getString("jobpair_stage_data.config_name"));
				}


				jp.getPrimarySolver().addConfiguration(jp.getPrimaryConfiguration());

				Status status = stage.getStatus();
				status.setCode(results.getInt("jobpair_stage_data.status_code"));


				Properties attributes = jp.getPrimaryStage().getAttributes();
				String result = results.getString("result");
				if (result != null) {
					attributes.put(R.STAREXEC_RESULT, result);
				}
				if (includeCompletion) {
					jp.setCompletionId(results.getInt("completion_id"));
				}
				if (includeExpected) {

					String expected = results.getString("expected");
					if (expected != null) {
						attributes.put(R.EXPECTED_RESULT, expected);
					}
				}

				pairs.add(jp);
			}
			return pairs;
		} catch (Exception e) {
			log.error("getJobPairsForDataTable", e);
		}
		return null;
	}

	/**
	 * Gets all the job pairs necessary to view in a datatable for a job space. All job pairs returned use the given
	 * configuration in the given stage
	 *
	 * @param jobSpaceId The id of the job_space id in question
	 * @param query a DataTablesQuery object
	 * @param configId The ID of the configuration to filter pairs by
	 * @param stageNumber The stage number to get pairs by
	 * @param type The "type" of the pairs, where type is defined by the columns of the solver stats table
	 * @return The job pairs to use in the next page of the table
	 */

	public static List<JobPair> getJobPairsForTableInJobSpaceHierarchy(
			int jobSpaceId, DataTablesQuery query, int configId, int stageNumber, String type
	) {
		final String methodName = "getJobPairsForTableInJobSpaceHierarchy";
		log.entry(methodName);
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		if (query.getSearchQuery() == null) {
			query.setSearchQuery("");
		}
		int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();
		try {

			con = Common.getConnection();

			if (query.getSortColumn() == 7) {
				query.setSortASC(!query.isSortASC());
			}

			PaginationQueryBuilder builder =
					new PaginationQueryBuilder(PaginationQueries.GET_PAIRS_IN_SPACE_HIERARCHY_QUERY,
					                           getJobPairOrderColumn(query.getSortColumn(), false), query
					);

			String constructedSQL = builder.getSQL();

			log.debug(methodName, ":jobSpaceId = " + jobSpaceId);
			log.debug(methodName, ":stageNumber = " + stageNumber);
			log.debug(methodName, ":configId = " + configId);
			log.debug(methodName, ":pairType = " + type);
			log.debug(methodName, ":query = " + query.getSearchQuery());
			log.debug(methodName, "Constructed SQL: " + constructedSQL);

			procedure = new NamedParameterStatement(con, constructedSQL);


			procedure.setString("query", query.getSearchQuery());
			procedure.setInt("jobSpaceId", jobSpaceId);
			procedure.setInt("stageNumber", stageNumber);
			procedure.setInt("configId", configId);
			procedure.setString("pairType", type);
			results = procedure.executeQuery();
			return getJobPairsForDataTable(jobId, results, false, false, PrimitivesToAnonymize.NONE);
		} catch (Exception e) {
			log.error("getJobPairsForTableInJobSpaceHierarchy", "job: " + jobId, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
			log.exit(methodName);
		}

		return null;
	}

	/**
	 * Gets job pair information necessary for populating client side graphs
	 *
	 * @param jobSpaceId The ID of the job_space in question
	 * @param configIds Configurations to get job pairs for
	 * @param primitivesToAnonymize enum designating which (if any) primitive names should be anonymized.
	 * @param stageNumber The number of the stage that we are concerned with. If <=0, the primary stage is obtained
	 * @return A list of size equal to configIds. Each element of the list will contain a list of job pairs where each
	 * job pair in the list uses the configuration at the matching position in configIds.
	 * @author Eric Burns
	 */
	public static List<List<JobPair>> getJobPairsForSolverComparisonGraph(
			int jobSpaceId, List<Integer> configIds, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize
	) {
		try {
			// we will actually not be using the alternate function calls; we found an easier way to filter out non-correct job pairs
			// List<JobPair> pairs = Jobs.getSuccessfullyCompletedJobPairsInJobSpaceHierarchy(jobSpaceId, primitivesToAnonymize);
			List<JobPair> pairs = Jobs.getJobPairsInJobSpaceHierarchy(jobSpaceId, primitivesToAnonymize);
			List<List<JobPair>> pairLists = new ArrayList<>();


			Map<Integer, Integer> configToPosition = new HashMap<>();

			for (int i = 0; i < configIds.size(); i++) {
				pairLists.add(new ArrayList<>());
				configToPosition.put(configIds.get(i), i);
			}
			for (JobPair jp : pairs) {

				JoblineStage stage = jp.getStageFromNumber(stageNumber);

				if (stage == null || stage.isNoOp()) {
					continue;
				}

				// only forward successfully completed job pairs -- Alexander Brown, 8/2021
				// int value 0 is returned when the job pair is correct
				if (JobPairs.isPairCorrect(stage) != 0) {
					continue;
				}

				int configId = stage.getConfiguration().getId();
				if (configToPosition.containsKey(configId)) {
					List<JobPair> filteredPairs = pairLists.get(configToPosition.get(configId));
					filteredPairs.add(jp);
				}
			}
			return pairLists;
		} catch (Exception e) {
			log.error("getJobPairsForSolverComparisonGraph", e);
		}
		return null;
	}

	/**
	 * Returns the count of all pairs in a job
	 *
	 * @param jobId The ID of the job to count pairs for
	 * @return The number of pairs in the job
	 */
	public static int getPairCount(int jobId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL countPairsForJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("getPairCount", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}

	/**
	 * Gets the name of the SQL column to sort on given an index of a dataTables column from the front end
	 *
	 * @param orderIndex
	 * @return
	 */
	private static String getJobOrderColumn(int orderIndex) {
		switch (orderIndex) {
		case 0:
			return "jobs.name";
		case 1:
			 // this is the same as ordering by status, as the status is determined by whether a job has pending pairs
			return "pendingPairs";
		case 2:
			return "completePairs";
		case 3:
			return "totalPairs";
		case 4:
			return "errorPairs";
		case 5:
			return "created";
		case 6:
			return "disk_size";
		}
		return "jobs.name";
	}

	/**
	 * Get next page of the jobs belong to a specific user
	 *
	 * @param query a DataTablesQuery object
	 * @param userId Id of the user we are looking for
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */
	public static List<Job> getJobsByUserForNextPage(DataTablesQuery query, int userId) {
		final String methodName = "getJobsByUserForNextPage";
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			log.debug(methodName, "Sorting on col: " + query.getSortColumn());
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_JOBS_BY_USER_QUERY,
			                                                            getJobOrderColumn(query.getSortColumn()), query
			);
			log.debug(methodName, "SQL: " + builder.getSQL());
			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setString("query", query.getSearchQuery());
			procedure.setInt("userId", userId);
			results = procedure.executeQuery();
			return getJobsForNextPage(results);
		} catch (Exception e) {
			log.error("getJobsByUserForNextPage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Get next page of the jobs belong to a space, or in the whole system if the ID is -1
	 *
	 * @param query A DataTablesQuery object
	 * @param spaceId Id of the space we are looking for. If -1, all jobs in the entire system are returned (for admin
	 * page)
	 * @return a list of Jobs belong to the user
	 * @author Ruoyu Zhang
	 */

	public static List<Job> getJobsForNextPage(DataTablesQuery query, int spaceId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_JOBS_IN_SPACE_QUERY,
			                                                            getJobOrderColumn(query.getSortColumn()), query
			);
			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setString("query", query.getSearchQuery());
			procedure.setInt("spaceId", spaceId);
			results = procedure.executeQuery();
			return getJobsForNextPage(results);
		} catch (Exception e) {
			log.error("getJobsForNextPage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets the minimal number of Jobs necessary in order to service the client's request for the next page of Jobs in
	 * their DataTables objects
	 *
	 * @return a list of 10, 25, 50, or 100 Jobs containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	private static List<Job> getJobsForNextPage(ResultSet results) {

		final Map<Integer, org.starexec.data.to.Queue> queues = new HashMap<>();

		try {

			List<Job> jobs = new LinkedList<>();

			while (results.next()) {

				// Grab the relevant job pair statistics; this prevents a secondary set of queries
				// to the database in RESTHelpers.java
				HashMap<String, Integer> liteJobPairStats = new HashMap<>();
				liteJobPairStats.put("totalPairs", results.getInt("totalPairs"));
				liteJobPairStats.put("completePairs", results.getInt("completePairs"));
				liteJobPairStats.put("pendingPairs", results.getInt("pendingPairs"));
				liteJobPairStats.put("errorPairs", results.getInt("errorPairs"));

				Integer completionPercentage =
						Math.round(100f * results.getInt("completePairs") / results.getInt("totalPairs"));
				liteJobPairStats.put("completionPercentage", completionPercentage);

				Integer errorPercentage =
						Math.round(100f * results.getInt("errorPairs") / results.getInt("totalPairs"));
				liteJobPairStats.put("errorPercentage", errorPercentage);

				Job j = new Job();

				j.setId(results.getInt("id"));
				j.setName(results.getString("name"));
				j.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					j.setName(j.getName() + " (deleted)");
				}
				j.setDeleted(results.getBoolean("deleted"));
				j.setDescription(results.getString("description"));
				j.setCreateTime(results.getTimestamp("created"));
				j.setCompleteTime(results.getTimestamp("completed"));
				j.setDiskSize(results.getLong("disk_size"));
				j.setLiteJobPairStats(liteJobPairStats);

				try {
					final int queueId = results.getInt("queue_id");
					if (!queues.containsKey(queueId)) {
						queues.put(queueId, Queues.get(queueId));
					}
					j.setQueue(queues.get(queueId));
				} catch (Exception e) {
				}

				jobs.add(j);
			}
			return jobs;
		} catch (Exception e) {
			log.error("getJobsForNextPageSays", e);
		}
		return null;
	}

	/**
	 * Gets a list of Incomplete Jobs for admins
	 *
	 * @return a List of incomplete Jobs
	 **/
	public static List<Job> getIncompleteJobs() throws SQLException {
		return Common.query("{CALL GetIncompleteJobs()}", procedure -> {}, Jobs::getJobsForNextPage);
	}

	/**
	 * Attempts to retrieve cached SolverStats objects from the database. Returns an empty list if the stats have not
	 * already been cached.
	 *
	 * @param jobSpaceId The ID of the root job space for the stats
	 * @param stageNumber The number of the stage to get data for
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @return A list of the relevant SolverStats objects in this space
	 * @author Eric Burns
	 */

	public static List<SolverStats> getCachedJobStatsInJobSpaceHierarchy(
			int jobSpaceId, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize
	) {
		log.debug("calling GetJobStatsInJobSpace with jobspace = " + jobSpaceId + " and stage = " + stageNumber);
		int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobStatsInJobSpace(?,?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, jobId);
			procedure.setInt(3, stageNumber);
			results = procedure.executeQuery();
			List<SolverStats> stats = new ArrayList<>();
			while (results.next()) {
				SolverStats s = new SolverStats();
				s.setCompleteJobPairs(results.getInt("complete"));
				s.setConflicts(results.getInt("conflicts"));
				s.setIncompleteJobPairs(results.getInt("incomplete"));
				s.setWallTime(results.getDouble("wallclock"));
				s.setCpuTime(results.getDouble("cpu"));
				s.setFailedJobPairs(results.getInt("failed"));
				s.setIncorrectJobPairs(results.getInt("incorrect"));
				s.setCorrectJobPairs(results.getInt("correct"));
				s.setResourceOutJobPairs(results.getInt("resource_out"));
				s.setStageNumber(results.getInt("stage_number"));
				Solver solver = new Solver();
				Configuration c = new Configuration();
				if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
					solver.setName(results.getString("anonymous_solver_names.anonymous_name"));
					c.setName(results.getString("anonymous_config_names.anonymous_name"));
				} else {
					solver.setName(results.getString("solver.name"));
					c.setName(results.getString("config.name"));
				}
				solver.setId(results.getInt("solver.id"));
				c.setId(results.getInt("config.id"));
				c.setDeleted(results.getInt("config_deleted")); // Alexander Brown, 9/7/2020
				solver.addConfiguration(c);
				s.setSolver(solver);
				s.setConfiguration(c);
				stats.add(s);
			}
			return stats;
		} catch (Exception e) {
			log.error("getCachedJobStatsInJobSpaceHierarchy", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Attempts to retrieve cached SolverStats objects from the database. Returns an empty list if the stats have not
	 * already been cached.
	 *
	 * This version uses a stored procedure that includes configs marked as deleted. Used to construct the solver
	 * summary table in the job space view
	 * Alexander Brown 9/20
	 *
	 * @param jobSpaceId The ID of the root job space for the stats
	 * @param stageNumber The number of the stage to get data for
	 * @param primitivesToAnonymize PrimitivesToAnonymize instance
	 * @param includeUnknown if pairs with unknown results are included. 
	 * @return A list of the relevant SolverStats objects in this space
	 * @author Eric Burns
	 */

	public static List<SolverStats> getCachedJobStatsInJobSpaceHierarchyIncludeDeletedConfigs(
			int jobSpaceId, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize, boolean includeUnknown
	) {
		log.debug("calling GetJobStatsInJobSpace with jobspace = " + jobSpaceId + " and stage = " + stageNumber);
		int jobId = Spaces.getJobSpace(jobSpaceId).getJobId();
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobStatsInJobSpaceIncludeDeletedConfigs(?,?,?,?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, jobId);
			procedure.setInt(3, stageNumber);
			procedure.setBoolean(4, includeUnknown);
			results = procedure.executeQuery();
			List<SolverStats> stats = new ArrayList<>();
			while (results.next()) {
				SolverStats s = new SolverStats();
				s.setCompleteJobPairs(results.getInt("complete"));
				s.setConflicts(results.getInt("conflicts"));
				s.setIncompleteJobPairs(results.getInt("incomplete"));
				s.setWallTime(results.getDouble("wallclock"));
				s.setCpuTime(results.getDouble("cpu"));
				s.setFailedJobPairs(results.getInt("failed"));
				s.setIncorrectJobPairs(results.getInt("incorrect"));
				s.setCorrectJobPairs(results.getInt("correct"));
				s.setResourceOutJobPairs(results.getInt("resource_out"));
				s.setStageNumber(results.getInt("stage_number"));
				Solver solver = new Solver();
				Configuration c = new Configuration();
				if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
					solver.setName(results.getString("anonymous_solver_names.anonymous_name"));
					c.setName(results.getString("anonymous_config_names.anonymous_name"));
				} else {
					solver.setName(results.getString("solver.name"));
					c.setName(results.getString("config.name"));
				}
				solver.setId(results.getInt("solver.id"));
				c.setId(results.getInt("config.id"));
				c.setDeleted(results.getInt("config.deleted")); // Alexander Brown, 9/20
				solver.addConfiguration(c);
				s.setSolver(solver);
				s.setConfiguration(c);
				stats.add(s);

				// print status
				log.debug( "in Jobs.getCachedJobStatsInJobSpaceHierarchyIncludeDeletedConfigs:\n" +
						"config.deleted: " + results.getInt( "config.deleted" ) + "\n" +
						"c.getDeleted(): " + c.getDeleted() + "\n" +
						"s.getConfigDeleted(): " + s.getConfigDeleted() );
			}
			return stats;
		} catch (Exception e) {
			log.error("getCachedJobStatsInJobSpaceHierarchyIncludeDeletedConfigs: " + e.getMessage());
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * @param jobId ID of job to return
	 * @return A job populated with all details and pairs
	 */
	public static Job getJobForMatrix(int jobId) {
		return getDetailed(jobId, 0, false);
	}

	private static List<JobPair> getAllPairs(int jobId) {
		final String methodName = "getAllPairs";
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();

			log.debug(methodName, "Getting all detailed pairs for job " + jobId);

			procedure = con.prepareCall("{CALL GetAllJobPairsByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			return getPairsDetailed(jobId, results, false);
		} catch (Exception e) {
			log.error(methodName, e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all job pairs for the given job that have been completed after a given point and also populates its
	 * resource
	 * TOs. Gets only the primary stage
	 *
	 * @param jobId The id of the job to get pairs for
	 * @param since The completed ID after which to get all jobs
	 * @return A list of job pair objects representing all job pairs completed after "since" for a given job
	 * @author Eric Burns
	 */
	public static List<JobPair> getNewCompletedPairsDetailed(int jobId, int since) {
		Connection con = null;

		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			log.info("getting detailed pairs for job " + jobId);

			procedure = con.prepareCall("{CALL GetNewCompletedJobPairsByJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, since);
			results = procedure.executeQuery();
			List<JobPair> pairs = getPairsDetailed(jobId, results, true);
			HashMap<Integer, HashMap<Integer, Properties>> props = Jobs.getNewJobAttributes(con, jobId, since);

			for (JobPair jp : pairs) {
				if (props.containsKey(jp.getId())) {
					HashMap<Integer, Properties> pairInfo = props.get(jp.getId());
					if (pairInfo.containsKey(jp.getPrimaryStage().getStageNumber())) {
						jp.getPrimaryStage().setAttributes(pairInfo.get(jp.getPrimaryStage().getStageNumber()));
					}
				}
				// Add the pair's benchmark's expected result to the pair's attributes.
				TreeMap<String, String> jpBenchProps = Benchmarks.getSortedAttributes(jp.getBench().getId());
				// Make sure the benchmark properties has an expected result
				if (jpBenchProps.containsKey(R.EXPECTED_RESULT)) {
					String expectedResult = jpBenchProps.get(R.EXPECTED_RESULT);
					List<JoblineStage> jpStages = jp.getStages();
					for (JoblineStage stage : jpStages) {
						// Set all the stages expected results to the benchmark's expected result.
						stage.getAttributes().setProperty(R.EXPECTED_RESULT, expectedResult);
					}
				}
			}


			return pairs;
		} catch (Exception e) {
			log.error("getNewCompletedPairsDetailed", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * For a given job, gets every job pair with the minimal amount of information required to find the job pair output
	 * on disk. Only the primary stage is required
	 *
	 * @param jobId The ID of the job to get pairs for
	 * @param since Only gets pairs that were finished after "completion ID"
	 * @return A list of JobPair objects
	 */
	public static List<JobPair> getNewCompletedPairsShallow(int jobId, int since) {
		Connection con = null;

		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			log.debug("getting shallow pairs for job " + jobId);
			//otherwise, just get the completed ones that were completed later than lastSeen
			procedure = con.prepareCall("{CALL GetNewJobPairFilePathInfoByJob(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, since);
			results = procedure.executeQuery();
			List<JobPair> pairs = new ArrayList<>();
			while (results.next()) {
				JobPair pair = new JobPair();
				JoblineStage stage = new JoblineStage();
				stage.setStageNumber(results.getInt("primary_jobpair_data"));
				pair.setPrimaryStageNumber(results.getInt("primary_jobpair_data"));
				pair.setJobId(jobId);
				pair.setId(results.getInt("id"));
				pair.setPath(results.getString("path"));
				stage.getSolver().setName(results.getString("solver_name"));
				stage.getConfiguration().setName(results.getString("config_name"));
				pair.getBench().setName(results.getString("bench_name"));
				pair.setCompletionId(results.getInt("completion_id"));
				pair.addStage(stage);
				pair.getStatus().setCode(results.getInt("job_pairs.status_code"));

				pairs.add(pair);
			}
			return pairs;
		} catch (Exception e) {
			log.error("getNewCompletedPairsShallow", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets attributes for all pairs with completion IDs greater than completionId
	 *
	 * @param con The open connection to make the query on
	 * @param jobId The ID of the job in question
	 * @param completionId The completion ID after which the pairs are relevant
	 * @return A HashMap mapping job pair IDs to attributes
	 * @author Eric Burns
	 */

	protected static HashMap<Integer, HashMap<Integer, Properties>> getNewJobAttributes(
			Connection con, int jobId, Integer completionId
	) {
		CallableStatement procedure = null;
		ResultSet results = null;
		log.debug("Getting all new attributes for job with ID = " + jobId);
		try {
			procedure = con.prepareCall("{CALL GetNewJobAttrs(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, completionId);
			results = procedure.executeQuery();
			return processAttrResults(results);
		} catch (Exception e) {
			log.error("getNewJobAttributes", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all attributes for every job pair associated with the given job completed after "completionId"
	 *
	 * @param jobId The ID of the job in question
	 * @param completionId The completion ID after which the pairs are relevant
	 * @return A HashMap mapping integer job-pair IDs to Properties objects representing their attributes
	 * @author Eric Burns
	 */
	public static HashMap<Integer, HashMap<Integer, Properties>> getNewJobAttributes(int jobId, int completionId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getNewJobAttributes(con, jobId, completionId);
		} catch (Exception e) {
			log.error("getNewJobAttributes", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all job pairs for the given job non-recursively (simple version) (Worker node, benchmark and solver will
	 * NOT
	 * be populated) only populates status code id, bench id and config id
	 *
	 * @param con The connection to make the query on
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Julio Cervantes
	 */
	protected static List<JobPair> getPairsSimple(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetJobPairsByJobSimple(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> returnList = new LinkedList<>();

			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setJobId(jobId);
				JoblineStage stage = new JoblineStage();
				stage.setStageNumber(results.getInt("stage_number"));
				jp.setPrimaryStageNumber(results.getInt("stage_number"));
				Configuration c = new Configuration();
				Solver s = new Solver();
				stage.setConfiguration(c);
				s.addConfiguration(c);
				stage.setSolver(s);
				jp.addStage(stage);
				jp.setId(results.getInt("id"));
				jp.setJobSpaceId(results.getInt("job_pairs.job_space_id"));
				jp.getStatus().setCode(results.getInt("job_pairs.status_code"));
				jp.getBench().setId(results.getInt("job_pairs.bench_id"));
				jp.getBench().setName(results.getString("job_pairs.bench_name"));
				c.setId(results.getInt("jobpair_stage_data.config_id"));
				c.setName(results.getString("jobpair_stage_data.config_name"));
				s.setId(results.getInt("jobpair_stage_data.solver_id"));
				s.setName(results.getString("jobpair_stage_data.solver_name"));
				jp.getSpace().setName(results.getString("name"));
				jp.getSpace().setId(results.getInt("job_spaces.id"));
				jp.setPath(results.getString("path"));
				int pipeId = results.getInt("pipeline_id");
				if (pipeId > 0) {
					SolverPipeline pipe = new SolverPipeline();
					pipe.setName(results.getString("solver_pipelines.name"));
					jp.setPipeline(pipe);
				} else {
					jp.setPipeline(null);
				}
				returnList.add(jp);
			}
			Common.safeClose(results);
			return returnList;
		} catch (Exception e) {
			log.error("getPairsSimple", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all job pairs for the given job non-recursively (simple version to test job xml bug) (Worker node, status,
	 * benchmark and solver will NOT be populated)
	 *
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Julio Cervantes
	 */
	public static List<JobPair> getPairsSimple(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return getPairsSimple(con, jobId);
		} catch (Exception e) {
			log.error("getPairsSimple", e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Gets all job pairs for the given job and also populates its used resource TOs (Worker node, status, benchmark
	 * and
	 * solver WILL be populated) Only the primary stage is populated
	 *
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Eric Burns
	 */

	public static List<JobPair> getPairsPrimaryStageDetailed(int jobId) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			log.info("getting detailed pairs for job " + jobId);

			procedure = con.prepareCall("{CALL GetJobPairsPrimaryStageByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<JobPair> pairs = getPairsDetailed(jobId, results, false);
			HashMap<Integer, HashMap<Integer, Properties>> props = Jobs.getJobAttributes(con, jobId);
			for (JobPair jp : pairs) {
				if (props.containsKey(jp.getId())) {
					HashMap<Integer, Properties> pairInfo = props.get(jp.getId());
					if (pairInfo.containsKey(jp.getPrimaryStage().getStageNumber())) {
						jp.getPrimaryStage().setAttributes(pairInfo.get(jp.getPrimaryStage().getStageNumber()));
					}
				}
			}
			return pairs;
		} catch (Exception e) {
			log.error("getPairsPrimaryStageDetailed", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets either all job pairs for the given job and also populates its used resource TOs or only the job pairs that
	 * have been completed after the argument "since" Only primary stages are populated (Worker node, status, benchmark
	 * and solver WILL be populated)
	 *
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Tyler Jensen, Benton Mccune, Eric Burns
	 */

	private static List<JobPair> getPairsDetailed(int jobId, ResultSet results, boolean getCompletionId) {
		log.debug("starting the getPairsDetailed function");
		try {
			List<JobPair> returnList = new ArrayList<>();

			//instead of setting up the solvers, configs, etc. every time, we just set them
			//up once and then save them
			Hashtable<Integer, Solver> discoveredSolvers = new Hashtable<>();
			Hashtable<Integer, Configuration> discoveredConfigs = new Hashtable<>();
			Hashtable<Integer, Benchmark> discoveredBenchmarks = new Hashtable<>();
			Hashtable<Integer, WorkerNode> discoveredNodes = new Hashtable<>();
			int curNode, curBench, curConfig, curSolver;
			while (results.next()) {
				JobPair jp = JobPairs.resultToPair(results);

				Status s = new Status();
				s.setCode(results.getInt("status_code"));

				jp.setStatus(s);

				//set the completion ID if it exists-- it only exists if we are getting new job pairs
				if (getCompletionId) {
					jp.setCompletionId(results.getInt("complete.completion_id"));
				}
				jp.setJobSpaceName(results.getString("jobSpace.name"));
				returnList.add(jp);
				curNode = results.getInt("node_id");
				curBench = results.getInt("bench_id");
				curConfig = results.getInt("config_id");
				curSolver = results.getInt("config.solver_id");
				JoblineStage stage = JobPairs.resultToStage(results);
				if (!discoveredSolvers.containsKey(curSolver)) {
					Solver solver = Solvers.resultSetToSolver(results, R.SOLVER);
					stage.setSolver(solver);
					discoveredSolvers.put(curSolver, solver);
				}
				stage.setSolver(discoveredSolvers.get(curSolver));

				if (!discoveredBenchmarks.containsKey(curBench)) {
					Benchmark b = Benchmarks.resultToBenchmarkWithPrefix(results, "bench");
					jp.setBench(b);
					discoveredBenchmarks.put(curBench, b);
				}
				jp.setBench(discoveredBenchmarks.get(curBench));

				if (!discoveredConfigs.containsKey(curConfig)) {
					Configuration c = new Configuration();
					c.setId(results.getInt("config.id"));
					c.setName(results.getString("config.name"));
					c.setSolverId(results.getInt("config.solver_id"));
					c.setDescription(results.getString("config.description"));
					discoveredConfigs.put(curConfig, c);
				}
				stage.setConfiguration(discoveredConfigs.get(curConfig));
				stage.getSolver().addConfiguration(discoveredConfigs.get(curConfig));
				if (!discoveredNodes.containsKey(curNode)) {
					WorkerNode node = new WorkerNode();
					node.setName(results.getString("node.name"));
					node.setId(results.getInt("node.id"));
					node.setStatus(results.getString("node.status"));
					discoveredNodes.put(curNode, node);
				}
				jp.addStage(stage);
				jp.setNode(discoveredNodes.get(curNode));
			}
			log.info("returning " + returnList.size() + " detailed pairs for job " + jobId);
			return returnList;
		} catch (Exception e) {
			log.error("getPairsDetailed", "jobId: " + jobId, e);
		}

		return null;
	}

	/**
	 * Counts the pairs that would be rerun if the user decided to rerun all timeless pairs
	 *
	 * @param jobId The id of the job to count for
	 * @return The count on success or -1 on failure
	 */

	public static int countTimelessPairs(int jobId) {
		int c1 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.STATUS_COMPLETE.getVal());
		int c2 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_CPU.getVal());
		int c3 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_FILE_WRITE.getVal());
		int c4 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_MEM.getVal());
		int c5 = Jobs.countTimelessPairsByStatus(jobId, StatusCode.EXCEED_RUNTIME.getVal());

		//on failure
		if (c1 == -1 || c2 == -1 || c3 == -1 || c4 == -1 || c5 == -5) {
			return -1;
		}

		return c1 + c2 + c3 + c4 + c5;
	}

	/**
	 * Sets job pairs with wallclock time 0 back to pending. Only pairs that are complete or had a resource out are
	 * reset
	 *
	 * @param jobId The ID of the job to perform the operation for
	 * @return True on success and false otherwise
	 */

	public static boolean setTimelessPairsToPending(int jobId) {
		try {
			boolean success = true;
			//only continue if we could actually clear the job stats
			Set<Integer> ids = new HashSet<>();
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId, StatusCode.STATUS_COMPLETE.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId, StatusCode.EXCEED_CPU.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId, StatusCode.EXCEED_FILE_WRITE.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId, StatusCode.EXCEED_MEM.getVal()));
			ids.addAll(Jobs.getTimelessPairsByStatus(jobId, StatusCode.EXCEED_RUNTIME.getVal()));


			for (Integer jp : ids) {
				success = success && Jobs.rerunPair(jp);
			}

			return success;
		} catch (Exception e) {
			log.error("setTimelessPairsToPending", e);
		}
		return false;
	}

	/**
	 * Sets every pair in a job back to pending, allowing all pairs to be rerun
	 *
	 * @param jobId The ID of the job to reset the pairs for
	 * @return True on success and false otherwise
	 */
	public static boolean setAllPairsToPending(int jobId) {
		if (Jobs.isReadOnly(jobId)) return false;
		try {
			List<JobPair> pairs = Jobs.getPairsSimple(jobId);
			boolean success = true;
			for (JobPair jp : pairs) {
				success = success && Jobs.rerunPair(jp.getId());
			}
			return success;
		} catch (Exception e) {
			log.error("setAllPairsToPending", e);
		}
		return false;
	}

	/**
	 * Begins the process of rerunning a single pair by removing it from the completed table (if applicable) killing it
	 * (also if applicable), and setting it back to pending
	 *
	 * @param pairId The ID of the pair to rerun
	 * @return True on success and false otherwise
	 */

	public static boolean rerunPair(int pairId) {
		try {
			log.debug("got a request to rerun pair id = " + pairId);
			boolean success = true;
			JobPair p = JobPairs.getPair(pairId);
			if (Jobs.isReadOnly(p.getJobId())) return false;
			Status status = p.getStatus();
			//no rerunning for pairs that are still pending
			if (status.getCode().getVal() == StatusCode.STATUS_PENDING_SUBMIT.getVal()) {
				return true;
			}
			if (status.getCode().getVal() < StatusCode.STATUS_COMPLETE.getVal()) {
				JobPairs.killPair(pairId, p.getBackendExecId());
			}
			JobPairs.setJobPairDiskSizeToZero(pairId);
			JobPairs.removePairFromCompletedTable(pairId);
			JobPairs.setPairStatus(pairId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
			JobPairs.setAllPairStageStatus(pairId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
			// the cache must be cleared AFTER changing the pair status code!
			success = success && Jobs.removeCachedJobStats(p.getJobId());

			return success;
		} catch (Exception e) {
			log.error("rerunPair", e);
		}

		return false;
	}

	/**
	 * Returns all job pairs in the given job with the given status code that have a run time of 0 for any stage
	 *
	 * @param jobId the ID of the job to get pairs for
	 * @param statusCode The status code of pairs to search for
	 * @return A list of job pair IDs, where each pair has at least one stage with a run time of 0 and also has the
	 * given status.
	 */

	public static List<Integer> getTimelessPairsByStatus(int jobId, int statusCode) {
		Connection con = null;

		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetTimelessJobPairsByStatus(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}

			return ids;
		} catch (Exception e) {
			log.error("getTimelessPairsByStatus", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Gets all job pair IDs of pairs that have the given status code in the given job, ordered by ID
	 *
	 * @param jobId The ID of the job in question
	 * @param statusCode The ID of the Status to get the pairs of
	 * @return A list of job pair IDs, or null on error
	 */
	public static List<Integer> getPairsByStatus(int jobId, int statusCode) {
		Connection con = null;

		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobPairsByStatus(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}

			return ids;
		} catch (Exception e) {
			log.error("getPairsByStatus", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Sets all the job pairs of a given status code and job to pending. Used to rerun pairs that didn't work in an
	 * initial job run
	 *
	 * @param jobId The id of the job in question
	 * @param statusCode The status code of pairs that should be rerun
	 * @return true on success and false otherwise
	 * @author Eric Burns
	 */
	public static boolean setPairsToPending(int jobId, int statusCode) {
		if (Jobs.isReadOnly(jobId)) return false;
		try {
			boolean success = true;
			List<Integer> pairs = Jobs.getPairsByStatus(jobId, statusCode);
			for (Integer id : pairs) {
				success = success && Jobs.rerunPair(id);
			}
			return success;
		} catch (Exception e) {
			log.error("setPairsToPending", e);
		}
		return false;
	}

	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used
	 * resource TOs (Worker node, status, benchmark and solver WILL be populated). Gets all stages (except noops)
	 *
	 * @param con The connection to make the query on
	 * @param j The job to get pairs for. Must have id and using_dependencies set.
	 * @return A list of job pair objects that belong to the given job.
	 * @author TBebnton
	 */
	protected static List<JobPair> getPendingPairsDetailed(Connection con, Job j, int limit) {

		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetPendingJobPairsByJob(?,?)}");
			procedure.setInt(1, j.getId());
			procedure.setInt(2, limit);
			results = procedure.executeQuery();
			//we map ID's to  primitives so we don't need to query the database repeatedly for them
			HashMap<Integer, JobPair> pairs = new HashMap<>();
			HashMap<Integer, String> solverIdsToTimestamps = new HashMap<>();
			while (results.next()) {

				try {
					int currentJobPairId = results.getInt("job_pairs.id");

					JobPair jp = null;
					// we have already seen this pair and are getting another stage
					if (pairs.containsKey(currentJobPairId)) {
						jp = pairs.get(currentJobPairId);
					} else {
						//we have never seen this pair and are getting it for the first time
						jp = JobPairs.resultToPair(results);
						Status s = new Status();
						s.setCode(results.getInt("job_pairs.status_code"));
						jp.setStatus(s);
						Benchmark b = Benchmarks.resultToBenchmarkWithPrefix(results, "benchmarks");
						b.setUsesDependencies(results.getInt("dependency_count") > 0);
						jp.setBench(b);

						if (j.isUsingDependencies()) {
							jp.setBenchInputPaths(JobPairs.getJobPairInputPaths(jp.getId(), con));
						} else {
							jp.setBenchInputPaths(new ArrayList<>());
						}
						pairs.put(currentJobPairId, jp);
					}

					JoblineStage stage = new JoblineStage();
					stage.setStageNumber(results.getInt("stage_number"));
					stage.setStageId(results.getInt("stage_id"));
					jp.addStage(stage);
					//we need to check to see if the benchId and configId are null, since they might
					//have been deleted while the the job is still pending

					int configId = results.getInt("jobpair_stage_data.config_id");
					String configName = results.getString("jobpair_stage_data.config_name");
					Configuration c = new Configuration();
					c.setId(configId);
					c.setName(configName);
					stage.setConfiguration(c);

					Solver s = Solvers.resultSetToSolver(results, "solvers");
					stage.setSolver(s /* could be null, if Solver s above was null */);
					if (s != null) {
						if (!solverIdsToTimestamps.containsKey(s.getId())) {
							solverIdsToTimestamps.put(s.getId(), Solvers.getMostRecentTimestamp(con, s.getId()));
						}
						s.setMostRecentUpdate(solverIdsToTimestamps.get(s.getId()));
					}
				} catch (Exception e) {
					log.error("getPendingPairsDetailed", "there was an error making a single job pair object", e);
				}
			}

			Common.safeClose(results);

			for (JobPair jp : pairs.values()) {
				if (j.isUsingDependencies()) {
					//populate all the dependencies for the pair
					HashMap<Integer, List<PipelineDependency>> deps =
							Pipelines.getDependenciesForJobPair(jp.getId(), con);
					for (JoblineStage stage : jp.getStages()) {
						if (deps.containsKey(stage.getStageId())) {
							stage.setDependencies(deps.get(stage.getStageId()));
						}
					}
				}
				//make sure all stages are in order

				jp.sortStages();
			}
			List<JobPair> returnList = new ArrayList<>();
			returnList.addAll(pairs.values());
			return returnList;
		} catch (Exception e) {
			log.error("getPendingPairsDetailed", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Returns all the benchmark inputs for all pairs in this job. Format is a HashMap that maps job pair IDs to
	 * ordered
	 * lists of benchmark IDs, where the order is the input order of the benchmarks
	 *
	 * @param jobId the ID of the job in question
	 * @param con The open connection to make the call on
	 * @return A mapping from jobpair IDs to lists of benchmark IDs, where the benchmark IDs are ordered according to
	 * their input order for the job pairs
	 */
	public static HashMap<Integer, List<Integer>> getAllBenchmarkInputsForJob(int jobId, Connection con) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL GetAllJobPairBenchmarkInputsByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			HashMap<Integer, List<Integer>> inputs = new HashMap<>();
			while (results.next()) {
				int pairId = results.getInt("jobpair_id");
				int benchId = results.getInt("bench_id");
				if (!inputs.containsKey(pairId)) {
					inputs.put(pairId, new ArrayList<>());
				}
				inputs.get(pairId).add(benchId);
			}
			return inputs;
		} catch (Exception e) {
			log.error("getAllBenchmarkInputsForJob", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Returns all the benchmark inputs for all pairs in this job. Format is a HashMap that maps job pair IDs to
	 * ordered
	 * lists of benchmark IDs, where the order is the input order of the benchmarks
	 *
	 * @param jobId The ID of the job to get the benchmark inputs for
	 * @return A HashMap that maps job pair IDs to ordered lists of benchmark IDs where the list is all the benchmark
	 * inputs for that pair in their proper order. Null on error.
	 */
	public static HashMap<Integer, List<Integer>> getAllBenchmarkInputsForJob(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();

			return getAllBenchmarkInputsForJob(jobId, con);
		} catch (Exception e) {
			log.error("getAllBenchmarkInputsForJob", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Gets all job pairs that are pending or were rejected (up to limit) for the given job and also populates its used
	 * resource TOs (Worker node, status, benchmark and solver WILL be populated). All stages are retrieved
	 *
	 * @param j The job to get pairs for. Must have id and using_dependencies set.
	 * @param limit The maximum number of pairs to return. Used for efficiency
	 * @return A list of job pair objects that belong to the given job.
	 * @author Benton McCune
	 */
	public static List<JobPair> getPendingPairsDetailed(Job j, int limit) {
		Connection con = null;

		try {
			con = Common.getConnection();
			return getPendingPairsDetailed(con, j, limit);
		} catch (Exception e) {
			log.error("getPendingPairsDetailed", e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Gets all job pairs that are  running for the given job
	 *
	 * @param con The connection to make the query on
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that belong to the given job.
	 * @author Wyatt Kaiser
	 */
	protected static List<JobPair> getRunningPairs(Connection con, int jobId) throws Exception {
		return getPairsHelper(con, "{CALL GetRunningJobPairsByJob(?)}", jobId);
	}

	/**
	 * Gets all job pairs that are running for the given job. Populates only the pair IDs and the SGE Ids
	 *
	 * @param jobId The id of the job to get pairs for
	 * @return A list of job pair objects that are running.
	 * @author Wyatt Kaiser
	 */
	public static List<JobPair> getRunningPairs(int jobId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			return Jobs.getRunningPairs(con, jobId);
		} catch (Exception e) {
			log.error("getRunningPairs", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/**
	 * Returns the count of pairs with the given status code in the given job where either cpu or wallclock is 0
	 *
	 * @param jobId
	 * @param statusCode
	 * @return The count or -1 on failure
	 */
	private static int countTimelessPairsByStatus(int jobId, int statusCode) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CountTimelessPairsByStatusByJob(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("countTimelessPairsByStatus", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return -1;
	}

	/**
	 * Returns the count of pairs with the given status code in the given job
	 *
	 * @param jobId The ID of the job to get pairs for
	 * @param statusCode The status to count pairs of
	 * @return The count or -1 on failure
	 */
	public static int countPairsByStatus(int jobId, int statusCode) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL CountPairsByStatusByJob(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, statusCode);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("count");
			}
		} catch (Exception e) {
			log.error("countPairsByStatus", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return -1;
	}

	/**
	 * Counts the number of pairs a job has that are in the processing status
	 *
	 * @param jobId The ID of the job to count pairs for
	 * @return The integer number of paris
	 */
	public static int countProcessingPairsByJob(int jobId) {
		return countPairsByStatus(jobId, StatusCode.STATUS_PROCESSING.getVal());
	}

	/**
	 * Returns whether the given job has any pairs that are currently waiting to be re post processed.
	 *
	 * @param jobId The ID of the job ot check
	 * @return True / false as expected, and null on error
	 */
	public static Boolean hasProcessingPairs(int jobId) {
		int count = countProcessingPairsByJob(jobId);
		if (count < 0) {
			return null;
		}
		return count > 0;
	}

	/**
	 * Determines whether the given job is in a good state to be post-processed
	 *
	 * @param jobId The ID of the job to check
	 * @return True if the job can be processed, false otherwise
	 * @author Eric Burns
	 */

	public static boolean canJobBePostProcessed(int jobId) {
		try {
			return getJobStatus(jobId) == JobStatus.COMPLETE;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Counts the number of pairs a job has that are not complete (status between 1 and 6)
	 *
	 * @param jobId The ID of the job to count pairs for
	 * @return The number of pairs in the job that are not complete
	 */

	public static int countIncompletePairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal()) +
				Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_ENQUEUED.getVal()) +
				Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_RUNNING.getVal());
	}

	/**
	 * Returns the number of job pairs that are pending for the current job
	 *
	 * @param jobId The ID of the job in question
	 * @return The integer number of pending pairs. -1 is returned on error
	 */
	public static int countPendingPairs(int jobId) {
		return Jobs.countPairsByStatus(jobId, Status.StatusCode.STATUS_PENDING_SUBMIT.getVal());
	}

	/**
	 * Gets the status of the given job
	 *
	 * @param jobId The ID of the job to check
	 * @return A JobStatus object
	 */
	public static JobStatus getJobStatus(int jobId) throws SQLException {
		return Common.query("SELECT GetJobStatusDetail(?);", procedure -> procedure.setInt(1, jobId),
		                    JobStatus::fromResultSet
		);
	}

	/**
	 * Determines whether the job with the given ID is complete
	 *
	 * @param jobId The ID of the job in question
	 * @return True if the job is complete, false otherwise (includes the possibility of error)
	 * @author Eric Burns
	 */
	public static boolean isJobComplete(int jobId) {
		try {
			return getJobStatus(jobId) == JobStatus.COMPLETE;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Checks whether the given job is set to "deleted" in the database
	 *
	 * @param con The open connection to make the call on
	 * @param jobId The ID of the job in question
	 * @return True if the job exists in the database with the deleted flag set to true, false otherwise
	 * @author Eric Burns
	 */

	public static boolean isJobDeleted(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL IsJobDeleted(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			boolean deleted = false;
			if (results.next()) {
				deleted = results.getBoolean("jobDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isJobDeleted", e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Determines whether the job with the given ID exists in the database with the column "deleted" set to true
	 *
	 * @param jobId The ID of the job in question
	 * @return True if the job exists in the database and has the deleted flag set to true
	 * @author Eric Burns
	 */

	public static boolean isJobDeleted(int jobId) {
		Connection con = null;

		try {
			con = Common.getConnection();

			return isJobDeleted(con, jobId);
		} catch (Exception e) {
			log.error("isJobDeleted", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Determines whether the job with the given ID exists in the database with the column "killed" set to true
	 *
	 * @param jobId The ID of the job in question
	 * @return True if the job is killed (i.e. the killed flag is set to true), false otherwise
	 * @author Wyatt Kaiser
	 */

	public static boolean isJobKilled(int jobId) {
		return isJobPausedOrKilled(jobId) == 2;
	}

	/**
	 * Determines whether the job with the given ID exists in the database with the column "paused" set to true
	 *
	 * @param jobId The ID of the job in question
	 * @return True if the job is paused (i.e. the paused flag is set to true), false otherwise
	 * @author Wyatt Kaiser
	 */

	public static boolean isJobPaused(int jobId) {
		return (isJobPausedOrKilled(jobId) == 1 || isJobPausedOrKilled(jobId) == 3);
	}

	/**
	 * Determines whether the given job is either paused, admin paused, or killed
	 *
	 * @param con The open connection to make the query on
	 * @param jobId The ID of the job in question
	 * @return 0 if the job is neither paused nor killed 1 if the job is paused 2 if the job has been killed 3 if the
	 * job has been admin paused
	 * @author Eric Burns
	 */
	public static int isJobPausedOrKilled(Connection con, int jobId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsJobPausedOrKilled(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			boolean paused = false;
			boolean killed = false;
			if (results.next()) {
				paused = results.getBoolean("paused");
				if (paused) {
					return 1;
				}
				killed = results.getBoolean("killed");
				if (killed) {
					return 2;
				}
			}
			return 0;
		} catch (Exception e) {
			log.error("isJobPausedOrKilled", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Determines whether the job with the given ID has either the paused or killed column set to true
	 *
	 * @param jobId The ID of the job in question
	 * @return 0 if the job is neither paused nor killed (or error) 1 if the job is paused (i.e. the paused flag is set
	 * to true), 2 if the job is killed
	 * @author Eric Burns
	 */

	public static int isJobPausedOrKilled(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();

			return isJobPausedOrKilled(con, jobId);
		} catch (Exception e) {
			log.error("isJobPausedOrKilled", e);
		} finally {
			Common.safeClose(con);
		}
		return 0;
	}

	/**
	 * Returns the IDs of all jobs in the system
	 *
	 * @return
	 */
	public static List<Integer> getAllJobIds() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllJobIds()}");
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("getAllJobIds", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Returns whether the job is public. A job is public if it was run by the public user or if it is in any public
	 * space
	 *
	 * @param jobId The ID of the job in question
	 * @return True if the job is public, and false if not or there was an error
	 */

	public static boolean isPublic(int jobId) {

		Job j = Jobs.get(jobId);
		if (j == null) {
			return false;
		}
		//if the public user made a job, then that job must be public
		if (Users.isPublicUser(j.getUserId())) {
			log.debug("Public User for Job Id" + jobId);
			return true;
		}
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL JobInPublicSpace(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			int count = 0;
			if (results.next()) {
				count = (results.getInt("spaceCount"));
			}

			if (count > 0) {
				return true;
			}
		} catch (Exception e) {
			log.error("isPublic", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	/**
	 * kills a running/paused job, and also sets the killed property to true in the database.
	 *
	 * @param jobId The ID of the job to kill
	 * @author Wyatt Kaiser
	 */
	public static void kill(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			kill(jobId, con);
		} catch (Exception e) {
			log.error("kill", e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * kills a running/paused job, and also sets the killed property to true in the database.
	 *
	 * @param jobId The ID of the job to kill
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	protected static boolean kill(int jobId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL KillJob(?)}");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();

			log.debug("Killing of job id = " + jobId + " was successful");

			List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);
			for (JobPair jp : jobPairsEnqueued) {
				JobPairs.killPair(jp.getId(), jp.getBackendExecId());
			}

			log.debug("deletion of killed job pairs from the queue was successful");
			return true;
		} catch (Exception e) {
			log.error("kill", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Kill all jobs belonging to a user.
	 *
	 * @param userId Id of user whose jobs are to be killed.
	 * @throws StarExecDatabaseException if a database related exception occurs.
	 * @author Albert Giegerich
	 */
	protected static void killUsersJobs(int userId) throws StarExecDatabaseException {
		Connection con = null;
		try {
			con = Common.getConnection();
			List<Job> usersJobs = Jobs.getByUserId(userId);
			for (Job job : usersJobs) {
				int jobId = job.getId();
				// Kill any jobs that are still running.
				if (!Jobs.isJobComplete(jobId)) {
					Jobs.kill(jobId, con);
				}
			}
		} catch (Exception e) {
			throw new StarExecDatabaseException(
					"Database error while deleting all jobs owned by user with id=" + userId, e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Pauses a job, and also sets the paused property to true in the database.
	 *
	 * @param jobId The ID of the job to pause
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean pause(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return pause(jobId, con);
		} catch (Exception e) {
			log.error("pause", e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * pauses a running job, and also sets the paused to true in the database.
	 *
	 * @param jobId The ID of the job to pause
	 * @param con An open database connection
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */

	protected static boolean pause(int jobId, Connection con) {
		log.info("Pausing job " + jobId);
		CallableStatement procedure = null;
		try {
			int numPairs = 0;
			final int paused = StatusCode.STATUS_PAUSED.getVal();
			final StopWatch timer = new StopWatch();
			timer.start();

			procedure = con.prepareCall("{CALL PauseJob(?)}");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();

			log.debug("Pausing of job with id = " + jobId + " was successful");

			//Get the enqueued job pairs and remove them
			List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(con, jobId);
			killPairs(jobPairsEnqueued);
			numPairs += jobPairsEnqueued.size();

			//Get the running job pairs and remove them
			List<JobPair> jobPairsRunning = Jobs.getRunningPairs(con, jobId);
			if (jobPairsRunning != null) {
				killPairs(jobPairsRunning);
				numPairs += jobPairsRunning.size();
			}

			timer.stop();
			log.info("Pause job with " + numPairs + " pairs took " + timer.getTime() + " milliseconds");

			log.debug("Deletion of paused job pairs from queue was successful");
			Analytics.JOB_PAUSE.record();
			return true;
		} catch (Exception e) {
			log.error("pause", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	private static void killPairs(List<JobPair> pairs) {
		for (JobPair jp : pairs) {
			int execId = jp.getBackendExecId();
			int pairId = jp.getId();
			R.BACKEND.killPair(execId);
			JobPairs.setStatusForPairAndStages(pairId, StatusCode.STATUS_PAUSED.getVal());
		}
	}

	/**
	 * Pauses all jobs owned by the given user
	 *
	 * @param userId
	 */
	public static void pauseAllUserJobs(int userId) {
		boolean success = true;
		for (Integer i : Jobs.getRunningJobs(userId)) {
			success = success && Jobs.pause(i);
		}
	}

	/**
	 * pauses all running jobs (via admin page), and also sets the paused & paused_admin to true in the database.
	 *
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */

	public static boolean pauseAll() {
		Connection con = null;
		CallableStatement procedure = null;
		log.info("Pausing all jobs");
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL PauseAll()}");
			procedure.executeUpdate();
			log.debug("Pause of system was successful");
			R.BACKEND.killAll();
			List<Integer> jobs = Jobs.getRunningJobs();
			if (jobs != null) {
				for (Integer jobId : jobs) {
					//Get the enqueued job pairs and remove them
					try {
						List<JobPair> jobPairsEnqueued = Jobs.getEnqueuedPairs(jobId);

						for (JobPair jp : jobPairsEnqueued) {
							JobPairs.UpdateStatus(jp.getId(), 1);
						}
					} catch (SQLException e) {
						log.warn("Caught SQLException while getting enqueued pairs.");
					}

					//Get the running job pairs and remove them
					List<JobPair> jobPairsRunning = Jobs.getRunningPairs(jobId);
					log.debug("JPR = " + jobPairsRunning);
					if (jobPairsRunning != null) {
						for (JobPair jp : jobPairsRunning) {
							JobPairs.UpdateStatus(jp.getId(), 1);
						}
					}
					log.debug("Deletion of paused job pairs from queue was successful");
				}
			}


			return true;
		} catch (Exception e) {
			log.error("pauseAll", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Changes the queue that the given job is running on
	 *
	 * @param jobId The ID of the job to change the queue for
	 * @param queueId The ID of the new queue
	 * @return True on success and false otherwise
	 */
	public static boolean changeQueue(int jobId, int queueId) {
		if (Jobs.isReadOnly(jobId)) return false;
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL ChangeQueue(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, queueId);
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("changeQueue", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Update the name of a job
	 *
	 * @param jobId The ID of the job to update
	 * @param newName The name to assign
	 * @throws StarExecDatabaseException
	 */
	public static void setJobName(int jobId, String newName) throws StarExecDatabaseException {
		final String method = "setJobName";
		log.entry(method);
		Connection connection = null;
		CallableStatement procedure = null;
		try {
			connection = Common.getConnection();
			procedure = connection.prepareCall("{CALL SetJobName(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, newName);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error("Caught exception.", e);
			throw new StarExecDatabaseException("Could not save job name to database.", e);
		} finally {
			Common.safeClose(connection);
			Common.safeClose(procedure);
			log.exit(method);
		}
	}

	/**
	 * Update the description of a job
	 *
	 * @param jobId The ID of the job to edit
	 * @param newDescription The description to assign
	 * @throws StarExecDatabaseException
	 */
	public static void setJobDescription(int jobId, String newDescription) throws StarExecDatabaseException {
		final String method = "setJobDescription";
		log.entry(method);
		Connection connection = null;
		CallableStatement procedure = null;
		try {
			connection = Common.getConnection();
			procedure = connection.prepareCall("{CALL SetJobDescription(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setString(2, newDescription);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error("Caught exception.", e);
			throw new StarExecDatabaseException("Could not save job description to database.", e);
		} finally {
			Common.safeClose(connection);
			Common.safeClose(procedure);
			log.exit(method);
		}
	}

	/**
	 * Given a set of pairs and a mapping from pair IDs, to stage numbers to properties, loads the properties into the
	 * appropriate pairs
	 *
	 * @param pairs The job pairs to load attributes into
	 * @param attrs A HashMap that maps job pair IDs to a second map that goes from stage numbers to Properties.
	 */
	public static void loadPropertiesIntoPairs(
			List<JobPair> pairs, HashMap<Integer, HashMap<Integer, Properties>> attrs
	) {
		for (JobPair jp : pairs) {
			HashMap<Integer, Properties> stageAttrs = attrs.get(jp.getId());
			if (stageAttrs != null) {
				for (JoblineStage stage : jp.getStages()) {
					if (stageAttrs.containsKey(stage.getStageNumber())) {
						stage.setAttributes(stageAttrs.get(stage.getStageNumber()));
					}
				}
			}
		}
	}

	/**
	 * Given a resultset containing the results of a query for job pair attrs, returns a hashmap mapping job pair
	 * ids to
	 * maps of stage number to properties
	 *
	 * @param results The ResultSet containing the attrs
	 * @return A mapping from pair ids to Properties
	 * @author Eric Burns
	 */
	private static HashMap<Integer, HashMap<Integer, Properties>> processAttrResults(ResultSet results) {
		try {
			HashMap<Integer, HashMap<Integer, Properties>> props = new HashMap<>();
			int id;
			int stageNumber;
			while (results.next()) {
				id = results.getInt("pair.id");
				stageNumber = results.getInt("attr.stage_number");
				if (!props.containsKey(id)) {
					props.put(id, new HashMap<>());
				}
				HashMap<Integer, Properties> pairMap = props.get(id);
				if (!pairMap.containsKey(stageNumber)) {
					pairMap.put(stageNumber, new Properties());
				}
				String key = results.getString("attr.attr_key");
				String value = results.getString("attr.attr_value");
				if (key != null && value != null) {
					props.get(id).get(stageNumber).put(key, value);
				}
			}
			return props;
		} catch (Exception e) {
			log.error("processAttrResults", e);
		}
		return null;
	}

	private static void addStageToSolverStats(SolverStats stats, JoblineStage stage, boolean includeUnknown) {
		StatusCode statusCode = stage.getStatus().getCode();

		if (statusCode.failed()) {
			stats.incrementFailedJobPairs();
		}
		if (statusCode.resource()) {
			stats.incrementResourceOutPairs();
		}
		if (statusCode.incomplete()) {
			stats.incrementIncompleteJobPairs();
		}
		if (statusCode.statComplete()) {
			stats.incrementCompleteJobPairs();
		}

		int correct = JobPairs.isPairCorrect(stage);
		if (correct == 0) {

			stats.incrementWallTime(stage.getWallclockTime());
			stats.incrementCpuTime(stage.getCpuTime());
			stats.incrementCorrectJobPairs();
		} else if (correct == 1) {
			stats.incrementIncorrectJobPairs();
		}
		else if (correct == 2) {
			//if the pair has unknown status, 
			if (includeUnknown) {
				stats.incrementWallTime(stage.getWallclockTime());
				stats.incrementCpuTime(stage.getCpuTime());
			}

		}
	}

	/**
	 * Given a list of JobPairs, compiles them into SolverStats objects.
	 *
	 * @param pairs The JobPairs with their relevant fields populated
	 * @param includeUnknown do we include pairs with unknown status 
	 * @return A list of SolverStats objects to use in a datatable
	 * @author Eric Burns
	 */
	public static Collection<SolverStats> processPairsToSolverStats(int jobId, List<JobPair> pairs, boolean includeUnknown) {
		final String methodName = "processPairsToSolverStats";
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try {
			//Map<String, Integer> solverIdToNumberOfConflicts = new HashMap<>();
			//solverIdToNumberOfConflicts = buildSolverIdToNumberOfConflictsMap(jobId);

			Hashtable<String, SolverStats> stats = new Hashtable<>();
			String key;
			for (JobPair jp : pairs) {

				for (JoblineStage stage : jp.getStages()) {

					//we need to exclude noOp stages
					if (stage.isNoOp()) {
						continue;
					}

					//entries in the stats table determined by stage/configuration pairs
					key = getStageConfigHashKey(stage);
					log.trace("Got solver stats key: " + key);
					int configId = stage.getConfiguration().getId();

					// // print status
					// log.debug( "in Jobs.processPairsToSolverStats(): current configId = " + stage.getConfiguration().getId() +
					// 		"; current deleted status = " + stage.getConfiguration().getDeleted() );

					int stageNumber = stage.getStageNumber();
					Integer conflicts = null;
					if (!stats.containsKey(key)) { // current stats entry does not yet exist
						SolverStats newSolver = new SolverStats();
						newSolver.setStageNumber(stage.getStageNumber());
						newSolver.setSolver(stage.getSolver());
						newSolver.setConfiguration(stage.getConfiguration());
						// Compute the number of conflicts and save them in variable in case we need to use them again.
						conflicts = Solvers.getConflictsForConfigInJobWithStage(jobId, configId, stageNumber);
						newSolver.setConflicts(conflicts);
						stats.put(key, newSolver);
					}


					//update stats info for entry that current job-pair belongs to
					SolverStats curSolver = stats.get(key);
					addStageToSolverStats(curSolver, stage, includeUnknown);
					if (stage.getStageNumber().equals(jp.getPrimaryStageNumber())) {
						//if we get here, we need to add this stage to the primary stats as well
						key = 0 + ":" + String.valueOf(stage.getConfiguration().getId());
						if (!stats.containsKey(key)) { // current stats entry does not yet exist
							SolverStats newSolver = new SolverStats();
							newSolver.setStageNumber(0);
							newSolver.setSolver(stage.getSolver());
							newSolver.setConfiguration(stage.getConfiguration());
							if (conflicts == null) {
								conflicts = Solvers.getConflictsForConfigInJobWithStage(jobId, configId, stageNumber);
							}
							newSolver.setConflicts(conflicts);
							stats.put(key, newSolver);
						}


						//update stats info for entry that current job-pair belongs to
						curSolver = stats.get(key);
					}
				}
			}

			stopWatch.stop();
			log.debug(
					methodName,
					"Time taken to process job pairs to stats for job with " + Jobs.getPairCount(jobId) + " pairs: " +
							stopWatch.toString()
			);
			return stats.values();
		} catch (Exception e) {
			log.error("processPairsToSolverStats", e);
		}
		return null;
	}

	/*
	 * Gets all of the benchmarks in a job for which a job pair gave a result that conflicted with another job pair on
	 * the same benchmark.
	 * @param jobId The id of the job to get conflicting benchmarks for.
	 * @return A set of all the benchmark ids that are conflicting.
	 * @throws SQLException if there is a problem with the database.
	 *
	public static Set<Integer> getConflictingBenchmarksForJob(int jobId) throws SQLException {
		final String methodName = "getConflictingBenchmarksForJob";
		Set<Integer> conflictingBenchmarkIds = new HashSet<>();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		log.debug("Calling Benchmarks.getByJob("+jobId+")");
		List<Benchmark> benchmarksInJob = Benchmarks.getByJob(jobId);
		log.debug("Benchmarks found in job while searching for conflicting benchmarks: " + benchmarksInJob.size());

		benchmarkLoop:
		for (Benchmark benchmarkInJob : benchmarksInJob) {
			// Loop through all the job pairs containing the benchmark. If two gave different results then the
			benchmark
			// is conflicting.
			String firstResultFound = null;
			List<JobPair> jobPairsInJobContainingBenchmark = JobPairs.getPairsInJobContainingBenchmark(jobId,
			benchmarkInJob.getId());
			for (JobPair jobPair : jobPairsInJobContainingBenchmark) {
				for (JoblineStage stage: jobPair.getStages()) {
					if (stageCantCountTowardsConflicts(stage)) {
						continue;
					}

					if (firstResultFound == null) {
						firstResultFound = stage.getStarexecResult();
					} else if (!firstResultFound.equals(stage.getStarexecResult())) {
						// Since there were two different results, add the benchmark to conflicting benchmarks and
						// continue to the next benchmark.
						conflictingBenchmarkIds.add(benchmarkInJob.getId());
						continue benchmarkLoop;
					}
				}
			}
		}
		stopWatch.stop();
		log.debug("Time taken to get conflicting benchmarks for job with "+Jobs.getPairCount(jobId)+" pairs:
		"+stopWatch.toString());
		return conflictingBenchmarkIds;
	}*/

//	private static Boolean stageCantCountTowardsConflicts(JoblineStage stage) {
//		return stage.isNoOp() || stage.getStarexecResult().equals(R.STAREXEC_UNKNOWN);
//	}

	private static String getStageConfigHashKey(JoblineStage stage) {
		return stage.getStageNumber() + ":" + String.valueOf(stage.getConfiguration().getId());
	}

	/**
	 * Given the result set from a SQL query containing job pair info, produces a list of job pairs for which all the
	 * necessary fields for solver stat production have been created
	 *
	 * @param results A resultset containing SQL data
	 * @return A list of job pairs
	 * @throws Exception
	 * @author Eric Burns
	 */

	private static List<JobPair> processStatResults(
			ResultSet results, boolean includeSingleStage, PrimitivesToAnonymize primitivesToAnonymize
	) {

		try {
			List<JobPair> returnList = new ArrayList<>();

			HashMap<Integer, Solver> solvers = new HashMap<>();
			HashMap<Integer, Configuration> configs = new HashMap<>();
			Integer id;


			Benchmark bench = null;
			while (results.next()) {
				JobPair jp = new JobPair();
				jp.setPrimaryStageNumber(results.getInt("primary_jobpair_data"));
				// these are the solver and configuration defaults. If any jobpair_stage_data
				// entry has null for a stage_id, then these are the correct primitives.


				Status s = new Status();

				s.setCode(results.getInt("status_code"));
				jp.setStatus(s);
				jp.setId(results.getInt("job_pairs.id"));
				jp.setPath(results.getString("job_pairs.path"));
				bench = new Benchmark();
				bench.setId(results.getInt("bench_id"));
				if (AnonymousLinks.areBenchmarksAnonymized(primitivesToAnonymize)) {
					bench.setName(results.getString("anon_bench_name"));
				} else {
					bench.setName(results.getString("bench_name"));
				}
				jp.setBench(bench);

				jp.setCompletionId(results.getInt("completion_id"));


				if (includeSingleStage) {
					//If we are here, we are populating exactly 1 stage for purposes of filling up a table.
					//so, we simply set the primary stage of this pair to the first stage for the time being
					jp.setPrimaryStageNumber(1);
					JoblineStage stage = new JoblineStage();
					stage.setStageNumber(1);
					stage.setCpuUsage(results.getDouble("jobpair_stage_data.cpu"));
					stage.setWallclockTime(results.getDouble("jobpair_stage_data.wallclock"));
					stage.setStageId(results.getInt("jobpair_stage_data.stage_id"));
					stage.getStatus().setCode(results.getInt("jobpair_stage_data.status_code"));
					//everything below this line is in a stage
					id = results.getInt("jobpair_stage_data.solver_id");
					//means it was null in SQL
					if (id == 0) {
						stage.setNoOp(true);
						stage.setSolver(null);
						stage.setConfiguration(null);
					} else {
						if (!solvers.containsKey(id)) {

							Solver solve = new Solver();
							solve.setId(id);
							if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
								solve.setName(results.getString("anon_solver_name"));
							} else {
								solve.setName(results.getString("jobpair_stage_data.solver_name"));
							}
							solvers.put(id, solve);
						}
						stage.setSolver(solvers.get(id));

						id = results.getInt("jobpair_stage_data.config_id");


						if (!configs.containsKey(id)) {
							Configuration config = new Configuration();
							config.setId(id);
							if (AnonymousLinks.areSolversAnonymized(primitivesToAnonymize)) {
								config.setName(results.getString("anon_config_name"));
							} else {
								config.setName(results.getString("jobpair_stage_data.config_name"));
							}
							configs.put(id, config);
						}
						stage.getSolver().addConfiguration(configs.get(id));
						stage.setConfiguration(configs.get(id));
					}


					Properties p = new Properties();
					String result = results.getString("result");
					if (result != null) {
						p.put(R.STAREXEC_RESULT, result);
					}


					stage.setAttributes(p);
					jp.addStage(stage);
				}

				returnList.add(jp);
			}

			return returnList;
		} catch (Exception e) {
			log.error("processStatResults", e);
		}
		return null;
	}

	/**
	 * Resumes a paused job, and also sets the paused property to false in the database.
	 *
	 * @param jobId The ID of the job to resume
	 * @return True on success, false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean resume(int jobId) {
		if (Jobs.isReadOnly(jobId)) {
			log.info("resume", "Cannot resume Read Only job: " + jobId);
			return false;
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			return resume(jobId, con);
		} catch (Exception e) {
			log.error("resume", e);
		} finally {
			Common.safeClose(con);
		}

		return false;
	}

	/**
	 * Resumes a paused job
	 *
	 * @param jobId The ID of the paused job
	 * @param con The open connection to make the call on
	 * @return true on success, false otherwise
	 */

	protected static boolean resume(int jobId, Connection con) {
		if (Jobs.isReadOnly(jobId)) {
			log.info("resume", "Cannot resume Read Only job: " + jobId);
			return false;
		}
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL ResumeJob(?)}");
			procedure.setInt(1, jobId);
			procedure.executeUpdate();

			log.debug("Resume of job id = " + jobId + " was successful");
			Analytics.JOB_RESUME.record();
			return true;
		} catch (Exception e) {
			log.error("resume", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * resumeAll sets global pause to false, which allows job pairs to be sent to the grid engine again
	 *
	 * @return true on success and false otherwise
	 * @author Wyatt Kaiser
	 */
	public static boolean resumeAll() {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL ResumeAll()}");
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("resumeAll", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Sets the given job up to be post processed by adding all of its pairs to the processing_job_pairs table
	 *
	 * @param jobId The ID of the the job to process
	 * @param processorId The ID of the post-processor to use
	 * @param stageNumber The ID of the state to reprocess
	 * @return True if the operation was successful, false otherwise.
	 * @author Eric Burns
	 */
	public static boolean prepareJobForPostProcessing(int jobId, int processorId, int stageNumber) {
		if (Jobs.isReadOnly(jobId)) return false;
		if (!Jobs.canJobBePostProcessed(jobId)) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			if (!Jobs.removeCachedJobStats(jobId, con)) {
				throw new Exception("Couldn't clear out the cache of job stats");
			}

			procedure = con.prepareCall("{CALL PrepareJobForPostProcessing(?,?,?,?,?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, processorId);
			procedure.setInt(3, StatusCode.STATUS_COMPLETE.getVal());
			procedure.setInt(4, StatusCode.STATUS_PROCESSING.getVal());
			procedure.setInt(5, stageNumber);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			Common.doRollback(con);
			log.error("prepareJobForPostProcessing", e);
		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * If the job is not yet complete, does nothing, as we don't want to store stats for incomplete jobs.
	 *
	 * @param jobId The ID of the job we are storing stats for
	 * @param stats The stats, which should have been compiled already
	 * @author Eric Burns
	 */
	public static void saveStats(int jobId, Collection<SolverStats> stats, boolean includeUnknown) {

		if (!isJobComplete(jobId)) {
			log.debug("stats for job with id = " + jobId + " were not saved because the job is incomplete");
			return; //don't save stats if the job is not complete
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			for (SolverStats s : stats) {

				if (!saveStats(s, con, includeUnknown)) {
					throw new Exception("saving stats failed, rolling back connection");
				}
			}
		} catch (Exception e) {
			log.error("caught exception in save stats: " + e.getMessage());
			Common.doRollback(con);
		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
		}
	}

	/**
	 * Given a SolverStats object, saves it in the database so that it does not need to be generated again This
	 * function
	 * is currently called only when the job is complete, as we do not want to cache stats for incomplete jobs.
	 *
	 * @param stats The stats object to save
	 * @param con The open connection to make the update on
	 * @return True if the save was successful, false otherwise
	 * @author Eric Burns
	 */

	private static boolean saveStats(SolverStats stats, Connection con, boolean includeUnknown) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL AddJobStats(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
			procedure.setInt(1, stats.getJobSpaceId());
			procedure.setInt(2, stats.getConfiguration().getId());
			procedure.setInt(3, stats.getCompleteJobPairs());
			procedure.setInt(4, stats.getCorrectJobPairs());
			procedure.setInt(5, stats.getIncorrectJobPairs());
			procedure.setInt(6, stats.getFailedJobPairs());
			procedure.setInt(7, stats.getConflicts());
			procedure.setDouble(8, stats.getWallTime());
			procedure.setDouble(9, stats.getCpuTime());
			procedure.setInt(10, stats.getResourceOutJobPairs());
			procedure.setInt(11, stats.getIncompleteJobPairs());
			procedure.setInt(12, stats.getStageNumber());
			procedure.setBoolean(13, includeUnknown);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("caught an exception while trying to save a single stat: " + e.getMessage());
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates the primary space of a job. This should only be necessary when changing the primary space of an older
	 * job
	 * from nothing to its new job space
	 *
	 * @param jobId The ID of the job in question
	 * @param jobSpaceId The new job space ID
	 * @param con the open connection to make the call on
	 * @author Eric Burns
	 */
	private static void updatePrimarySpace(int jobId, int jobSpaceId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL UpdatePrimarySpace(?, ?)}");
			procedure.setInt(1, jobId);
			procedure.setInt(2, jobSpaceId);
			procedure.executeUpdate();
		} catch (Exception e) {
			log.error("updatePrimarySpace", e);
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Deletes cached job stats in a job for the given configurations.
	 *
	 * @param jobId the id of the job to delete job stats from.
	 * @param configIds the configurations for which to delete job stats.
	 * @author Albert Giegerich
	 */
	public static void removeCachedJobStatsForConfigs(int jobId, Set<Integer> configIds) throws SQLException {
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			Job job = Jobs.get(jobId);

			// get all the job spaces in the job
			int rootSpaceId = job.getPrimarySpace();
			List<JobSpace> jobSpacesInJob = Spaces.getSubSpacesForJob(rootSpaceId, true);
			jobSpacesInJob.add(Spaces.getJobSpace(rootSpaceId));

			for (JobSpace jobSpace : jobSpacesInJob) {
				for (int cid : configIds) {
					removeCachedJobStatsForConfigAndJobSpace(con, jobSpace.getId(), cid);
				}
			}
		} finally {
			Common.endTransaction(con);
			Common.safeClose(con);
		}
	}

	private static void removeCachedJobStatsForConfigAndJobSpace(Connection con, int jobSpaceId, int configId)
			throws SQLException {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL RemoveJobStatsInJobSpaceForConfig(?, ?)}");
			procedure.setInt(1, jobSpaceId);
			procedure.setInt(2, configId);
			procedure.executeUpdate();
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Removes job stats for every job_space belonging to this job
	 *
	 * @param jobId The ID of the job to remove the stats of
	 * @param con The open Connection to make the database call on
	 * @return True on success and false otherwise
	 */
	public static boolean removeCachedJobStats(int jobId, Connection con) {
		CallableStatement procedure = null;
		try {
			Job j = Jobs.get(jobId);
			if (j == null) {
				return false; //could not find the job
			}
			List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob(j.getPrimarySpace(), true);
			jobSpaces.add(Spaces.getJobSpace(j.getPrimarySpace()));

			for (JobSpace s : jobSpaces) {
				procedure = con.prepareCall("{CALL RemoveJobStatsInJobSpace(?)}");
				procedure.setInt(1, s.getId());
				procedure.executeUpdate();
				Common.safeClose(procedure);
			}
			return true;
		} catch (Exception e) {
			log.error("removeCachedJobStats", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Completely clears the cache of all job stats from the database
	 *
	 * @param con The open connection to make the call on
	 * @return True on success and false otherwise
	 */

	public static boolean removeAllCachedJobStats(Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL RemoveAllJobStats()}");
			procedure.executeUpdate();
			Common.safeClose(procedure);

			return true;
		} catch (Exception e) {
			log.error("removeAllCachedJobStats", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Removes the cached job results for every job
	 *
	 * @return True if successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean removeAllCachedJobStats() {
		Connection con = null;
		try {
			con = Common.getConnection();
			return removeAllCachedJobStats(con);
		} catch (Exception e) {
			log.error("removeAllCachedJobStats", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Removes the cached job results for every job space associated with this job
	 *
	 * @param jobId The ID of the job to remove the cached stats for
	 * @return True if successful, false otherwise
	 * @author Eric Burns
	 */
	public static boolean removeCachedJobStats(int jobId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return removeCachedJobStats(jobId, con);
		} catch (Exception e) {
			log.error("removeCachedJobStats", "jobId: " + jobId, e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Returns the number of jobs that are currently paused on the system
	 *
	 * @return The integer number of jobs, or -1 on error
	 */
	public static int getPausedJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPausedJobCount()}");
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error("getPausedJobCount", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}

	/**
	 * Gets the number of Running Jobs in the whole system
	 *
	 * @return The integer number of running jobs
	 * @author Wyatt Kaiser
	 */

	public static int getRunningJobCount() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetRunningJobCount()}");
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("jobCount");
			}
		} catch (Exception e) {
			log.error("getRunningJobCount", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return 0;
	}

	/**
	 * Returns all jobs owned by the given user that have pairs either running or pending
	 *
	 * @param userId The ID of the user to search for
	 * @return The list of distinct job IDs
	 */
	public static List<Integer> getRunningJobs(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetRunningJobsByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			List<Integer> jobs = new LinkedList<>();
			while (results.next()) {
				jobs.add(results.getInt("id"));
			}
			return jobs;
		} catch (Exception e) {
			log.error("getRunningJobs", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Gets all the jobs on the system that currently have pairs pending or running and which are not currently paused
	 * or killed
	 *
	 * @return A list of Job ids for the running jobs. Pairs are not populated
	 */
	public static List<Integer> getRunningJobs() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetRunningJobs()}");
			results = procedure.executeQuery();

			List<Integer> jobs = new LinkedList<>();
			while (results.next()) {
				jobs.add(results.getInt("id"));
			}
			return jobs;
		} catch (Exception e) {
			log.error("getRunningJobs", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Checks to see if the global pause is enabled on the system
	 *
	 * @return True if the system is paused or false if it is not
	 */
	public static boolean isSystemPaused() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsSystemPaused()}");
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getBoolean("paused");
			}
			//if no results exist, the system is not globally paused
			return false;
		} catch (Exception e) {
			log.error("isSystemPaused", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Gets the ID of every job a user owns that is orphaned
	 *
	 * @param userId The ID of the user to get orphaned jobs for
	 * @return A list of job IDs, or null on error
	 */
	public static List<Integer> getOrphanedJobs(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		List<Integer> ids = new ArrayList<>();
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetOrphanedJobIds(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("getOrphanedJobs", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * Deletes all of the jobs a user has that are not in any spaces
	 *
	 * @param userId The ID of the user who will have their solvers recycled
	 * @return True on success and false otherwise
	 */
	public static boolean deleteOrphanedJobs(int userId) {
		List<Integer> ids = getOrphanedJobs(userId);
		try {
			for (Integer id : ids) {
				Jobs.delete(id);
			}
			return true;
		} catch (Exception e) {
			log.error("deleteOrphanedJobs", e);
		}
		return false;
	}

	/**
	 * Given a ResultSet that is currently pointing to a row containing data for a StageAttributes object, generates
	 * the
	 * object
	 *
	 * @param results The results, which must be pointing to a row with a StageAttributes object
	 * @return The StageAttributes, or null on error
	 */
	public static StageAttributes resultsToStageAttributes(ResultSet results) {
		try {
			StageAttributes attrs = new StageAttributes();

			attrs.setCpuTimeout(results.getInt("cpuTimeout"));
			attrs.setJobId(results.getInt("job_id"));
			attrs.setMaxMemory(results.getLong("maximum_memory"));
			attrs.setSpaceId(results.getInt("space_id"));
			if (attrs.getSpaceId() == 0) {
				attrs.setSpaceId(null);
			}
			attrs.setStageNumber(results.getInt("stage_number"));
			attrs.setWallclockTimeout(results.getInt("clockTimeout"));
			attrs.setBenchSuffix(results.getString("bench_suffix"));
			attrs.setResultsInterval(results.getInt("results_interval"));
			attrs.setStdoutSaveOption(SaveResultsOption.valueOf(results.getInt("stdout_save_option")));
			attrs.setExtraOutputSaveOption(SaveResultsOption.valueOf(results.getInt("extra_output_save_option")));
			return attrs;
		} catch (Exception e) {
			log.error("resultsToStageAttributes", e);
		}
		return null;
	}

	/**
	 * Gets all the stage attributes for the given job
	 *
	 * @param jobId The job in question
	 * @param con An open connection to make the call on
	 * @return A list of StageAttributes objects or null on error
	 */
	public static List<StageAttributes> getStageAttrsForJob(int jobId, Connection con) {
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL getStageParamsByJob(?)}");
			procedure.setInt(1, jobId);
			results = procedure.executeQuery();
			List<StageAttributes> attrs = new ArrayList<>();

			while (results.next()) {
				StageAttributes a = resultsToStageAttributes(results);

				a.setPostProcessor(Processors.get(results.getInt("post_processor")));
				a.setPreProcessor(Processors.get(results.getInt("pre_processor")));
				attrs.add(a);
			}

			return attrs;
		} catch (Exception e) {
			log.error("getStageAttrsForJob", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}

	/**
	 * This function takes all job pairs that 1) Have status code 2-5, meaning they should be enqueued or running 2)
	 * Are
	 * not currently listed in the backend and sets them to status code 9. This basically takes pairs that have somehow
	 * gotten stuck in a bad state and applies an error status to them.
	 *
	 * @param backend The Backend instance being used to run pairs on the system
	 * @throws IOException
	 */
	public static void setBrokenPairsToErrorStatus(Backend backend) throws IOException {
		// It is important that we read the database first and the backend second
		// Otherwise, any pairs enqueued between these two lines would get marked
		// as broken
		List<JobPair> runningPairs = JobPairs.getPairsInBackend();
		Set<Integer> backendIDs = backend.getActiveExecutionIds();
		for (JobPair p : runningPairs) {
			// if SGE does not think this pair should be running, kill it
			// the kill only happens if the pair's status has not been changed
			// since getPairsInBackend() was called
			if (!backendIDs.contains(p.getBackendExecId())) {
				if (Jobs.get(p.getJobId()).isBuildJob()) {
					Solver s = p.getPrimarySolver();
					int status = SolverBuildStatusCode.BUILD_FAILED.getVal();
					Solvers.setSolverBuildStatus(s, status);
				}
				JobPairs.setBrokenPairStatus(p);
			}
		}
	}

	private static boolean setJobDiskSize(int jobId, long diskSize) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateJobDiskSize(?,?)}");
			procedure.setInt(1, jobId);
			procedure.setLong(2, diskSize);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setJobDiskSize", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	public static boolean doesJobCopyBackIncrementally(int jobId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		Boolean jobCopiesBackResultsIncrementally = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DoesJobCopyBackIncrementally(?,?)}");
			procedure.setInt(1, jobId);
			procedure.registerOutParameter(2, java.sql.Types.BOOLEAN);
			results = procedure.executeQuery();
			jobCopiesBackResultsIncrementally = procedure.getBoolean(2);
		} catch (Exception e) {
			log.error("doesJobCopyBackIncrementally", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return jobCopiesBackResultsIncrementally;
	}

	public static List<String> getJobAttributeValues(int jobSpaceId) throws SQLException {
		return Common.query("{CALL GetJobAttributesTableHeaders(?)}", procedure -> procedure.setInt(1, jobSpaceId),
		                    results -> {
			                    List<String> headers = new ArrayList<>();
			                    while (results.next()) {
				                    headers.add(results.getString("attr_value"));
			                    }
			                    return headers;
		                    }
		);
	}

	public static List<String> getJobAttributesTableHeader(int jobSpaceId) throws SQLException {
		return getJobAttributeValues(jobSpaceId);
	}

	/**
	 * Sets a job to be low priority meaning the user's other jobs should be run first.
	 *
	 * @param jobId the job to set as low priority.
	 */
	public static void setAsLowPriority(final int jobId) throws SQLException {
		Common.update("{CALL SetHighPriority(?,?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setBoolean(2, false);
		});
	}

	/**
	 * Sets a job to be high priority meaning this job should run before other's jobs of the same user.
	 *
	 * @param jobId the job to make high priority.
	 */
	public static void setAsHighPriority(final int jobId) throws SQLException {
		Common.update("{CALL SetHighPriority(?,?)}", procedure -> {
			procedure.setInt(1, jobId);
			procedure.setBoolean(2, true);
		});
	}

	/**
	 * Gets the slots in a job's queue if the backend is SGE, otherwise just returns the default number of slots.
	 *
	 * @param job the jobs to get the queue from.
	 * @return
	 */
	public static String getSlotsInJobQueue(Job job) {
		final String methodName = "getSlotsInJobQueue";
		if (R.BACKEND_TYPE.equals(R.SGE_TYPE)) {
			GridEngineBackend backend = new GridEngineBackend();
			try {
				Integer slots = backend.getSlotsInQueue(job.getQueue().getName());
				return slots.toString();
			} catch (IOException e) {
				log.error(methodName, "Caught IOException while trying to get number of slots in queue.", e);
			} catch (StarExecException e) {
				log.error(
						methodName, "Could not get number of slots from backend.getSlotsInQueue. " +
								"SGE may not have returned an integer when queried.", e);
			}
		}
		return R.DEFAULT_QUEUE_SLOTS;
	}

	public static List<AttributesTableData> getJobAttributesTable(int jobSpaceId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		List<AttributesTableData> tableEntries = new ArrayList<>();
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetJobAttributesTable(?)}");
			procedure.setInt(1, jobSpaceId);
			results = procedure.executeQuery();
			while (results.next()) {
				Integer solverId = results.getInt("solver_id");
				String solverName = results.getString("solver_name");
				Integer configId = results.getInt("config_id");
				String configName = results.getString("config_name");
				Integer attrCount = results.getInt("attr_count");
				String attrValue = results.getString("attr_value");
				Double wallclockSum = results.getDouble("wallclock_sum");
				Double cpuSum = results.getDouble("cpu_sum");
				tableEntries
						.add(new AttributesTableData(solverId, solverName, configId, configName, attrValue, attrCount,
						                             wallclockSum, cpuSum
						));
			}
			return tableEntries;
		} catch (Exception e) {
			log.error("getJobAttributesTable", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * @param jobspaceId the jobspace to get attribute count totals in.
	 * @return list of attribute count totals for the jobspace sorted by attr_value.
	 * @throws SQLException
	 */
	public static List<Triple<String, Integer, TimePair>> getJobAttributeTotals(int jobspaceId) throws SQLException {
		return Common
				.query("{CALL GetSumOfJobAttributes(?)}", procedure -> procedure.setInt(1, jobspaceId), results -> {
					List<Triple<String, Integer, TimePair>> valueCounts = new ArrayList<>();
					while (results.next()) {
						valueCounts.add(new ImmutableTriple<>(results.getString("attr_value"),
						                                      results.getInt("attr_count"), new TimePair(
								String.format("%.4f", results.getDouble("wallclock")),
								String.format("%.4f", results.getDouble("cpu"))
						)
						));
					}
					return valueCounts;
				});
	}
}
