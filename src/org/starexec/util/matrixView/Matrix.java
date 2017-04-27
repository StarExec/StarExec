package org.starexec.util.matrixView;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.starexec.constants.R;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.*;
import org.starexec.data.to.compare.NameableComparators;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;

import java.util.*;


/**
 * Represents the matrix on the details/jobMatrixView page.
 * @author Albert Giegerich
 */
public class Matrix {

	private ArrayList<Benchmark> benchmarksByRow;
	private ArrayList<Pair<Solver,Configuration>> solverConfigsByColumn;
	private ArrayList<ArrayList<MatrixElement>> matrix;
	private boolean hasMultipleStages;
	private String jobSpaceName;
	private Integer jobSpaceId;


	private static final StarLogger log = StarLogger.getLogger(Matrix.class);


	/**
	 * Builds a Matrix for the job matrix display page given a Job and a stageNumber.
	 * @param jobPairs the job pairs to build the matrix display with.
	 * @param stageNumber filter the job pairs to view by this stage.
	 * @param jobSpaceId the job space to filter the pairs by
	 * @author Albert Giegerich
	 */
	private Matrix(List<JobPair> jobPairs, final Integer jobSpaceId, final int stageNumber) {
		final String method = "Matrix constructor";
		log.entry(method);
		try {
			log.debug(method, "Found "+jobPairs.size()+" job pairs.");
			String jobSpaceName = Spaces.getJobSpace(jobSpaceId).getName();
			initializeMatrixFields(jobSpaceName, jobSpaceId);


			Set<Benchmark> uniqueBenchmarks = new HashSet<Benchmark>();
			Set<Pair<Solver,Configuration>> uniqueSolverConfigs = new HashSet<Pair<Solver,Configuration>>();

			// Maps benchmark and solver-configuration vectors to the data that should appear in the cell where the two
			// vectors intersect.
			HashMap<Pair<Benchmark,Pair<Solver,Configuration>>,MatrixElement> vectorIntersectionToCellDataMap =
					new HashMap<Pair<Benchmark,Pair<Solver,Configuration>>,MatrixElement>();

			// Build the sets and the map that were just defined.
			for (JobPair pair : jobPairs) {
				addToUniqueVectorListsAndIntersectionMap(uniqueBenchmarks, uniqueSolverConfigs, vectorIntersectionToCellDataMap, pair, stageNumber);
				if (!this.hasMultipleStages) {
					this.hasMultipleStages = testForMultipleStages(pair);
				}
			}


			log.debug(method,"Sorting benchmarks and solver-config pairs.");
			// Sort the benchmarks alphabetically by name ignoring case.
			ArrayList<Benchmark> uniqueBenchmarkList = new ArrayList<Benchmark>(uniqueBenchmarks);
			Collections.sort(uniqueBenchmarkList, NameableComparators.getCaseInsensitiveAlphabeticalComparator());
			
			ArrayList<Pair<Solver,Configuration>> uniqueSolverConfigList = 
					new ArrayList<Pair<Solver,Configuration>>(uniqueSolverConfigs);
			// Names of solver config will be "solver (config)", sort the solverConfigs
			// alphabetically by name, ignore case.
			Collections.sort(uniqueSolverConfigList, new Comparator<Pair<Solver,Configuration>>() {
				@Override
				public int compare(Pair<Solver,Configuration> sc1, Pair<Solver,Configuration> sc2) {
					String solverName1 = sc1.getLeft().getName();
					String solverName2 = sc2.getLeft().getName();
					String configName1 = sc1.getRight().getName();
					String configName2 = sc2.getRight().getName();
					String sc1Name = String.format("%s (%s)", solverName1, configName1);
					String sc2Name = String.format("%s (%s)", solverName2, configName2);
					return sc1Name.compareToIgnoreCase(sc2Name);
				}
			});

			// Populate the matrix.
			populateRowAndColumnHeaders(uniqueBenchmarkList, uniqueSolverConfigList); 
			populateMatrixData(uniqueBenchmarkList, uniqueSolverConfigList, vectorIntersectionToCellDataMap);
			log.exit(method);
		} catch (Exception e) {
			log.warn(method, "Error in constructing matrix for matrix view page." + e.getMessage());
		}
	}


	/**
	 * Gets a matrix for a jobspace from a job and a stage number.
	 * @author Albert Giegerich
	 */
	public static Matrix getMatrixForJobSpaceFromJobAndStageNumber(Job job, int jobSpaceId, int stageNumber) throws StarExecException {
		final String method = "getMatricesByJobSpaceFromJobStage";
		log.entry(method);
		log.debug(method, "Found "+job.getJobPairs().size()+" job pairs.");
		/*
		List<Matrix> matricesByJobSpace = new LinkedList<Matrix>();
		*/
		Matrix matrixForJobSpace;
		List<JobPair> jobPairsAssociatedWithJobSpaceId = getJobPairsForJobSpace(job, jobSpaceId);
		if (jobPairsAssociatedWithJobSpaceId.size() > R.MAX_MATRIX_JOBPAIRS) {
			throw new StarExecException("Matrix supports up to " + R.MAX_MATRIX_JOBPAIRS + " job pairs.");
		}
		try {
			matrixForJobSpace = new Matrix(jobPairsAssociatedWithJobSpaceId, jobSpaceId, stageNumber);
		} catch (Exception e) {
			log.warn("Error encountered while attempting to generate matrices for job matrix display.", e);
			throw new StarExecException("Error encountered while attempting to generate matrices for job matrix display.");
		}
		return matrixForJobSpace;
	}

	/** 
	 * Gets all the job pairs associated with a jobspace in a job.
	 * @param job the job to get job pairs from
	 * @param jobSpaceId the id of the jobspace to filter the jobpairs by
	 * @return a list of jobpairs in the given job associated with the given jobspace
	 * @author Albert Giegerich
	 */
	private static List<JobPair> getJobPairsForJobSpace(Job job, int jobSpaceId) {
		final String method = "getJobPairsForJobSpace";
		List<JobPair> jobPairs = job.getJobPairs();
		List<JobPair> jobPairsInJobSpace = new ArrayList<JobPair>();
		for (JobPair pair : jobPairs) {
			Integer pairJobSpaceId = pair.getJobSpaceId();
			log.debug(method, "job space id="+pairJobSpaceId);
			// Filter by jobSpaceId
			if (pairJobSpaceId == jobSpaceId) {
				jobPairsInJobSpace.add(pair);	
			} 
		}
		return jobPairsInJobSpace;
	}

	/**
	 * Takes a Job and creates a HashMap that maps all of the ids of the job Spaces in the map to a list
	 * of the JobPairs in that job Space.
	 * @param job the job to build a map from.
	 * @return a HashMap that maps all the job pairs associated with a job space ID to a list in the map.
	 * @author Albert Giegerich
	 * 
	private static HashMap<Integer, List<JobPair>> getJobPairsBySpaceIdMapFromJob(Job job) {
		HashMap<Integer, List<JobPair>> jobPairsBySpaceIdMap = new HashMap<Integer, List<JobPair>>();
		List<JobPair> jobPairs = job.getJobPairs();
		for (JobPair pair : jobPairs) {
			Integer jobSpaceId = pair.getJobSpaceId();
			if (jobPairsBySpaceIdMap.containsKey(jobSpaceId)) {
				// If the map already contains a list of job pairs associated with this id then add this
				// pair to the list.
				List<JobPair> jobPairsAssociatedWithJobSpaceId = jobPairsBySpaceIdMap.get(jobSpaceId);
				jobPairsAssociatedWithJobSpaceId.add(pair);
			} else {
				// Otherwise add a new list associated with the id to the map containing the pair.
				List<JobPair> newListOfJobPairs = new LinkedList<JobPair>();
				newListOfJobPairs.add(pair);
				jobPairsBySpaceIdMap.put(jobSpaceId, newListOfJobPairs);
			}
		}
		return jobPairsBySpaceIdMap;
	}
	*/

	/**
	 * Gets the job space name associated with the matrix. 
	 * @return the job space name associated with the matrix.
	 * @author Albert Giegerich
	 */
	public String getJobSpaceName() {
		return jobSpaceName;
	}	

	/**
	 * Gets the job space id associated with the matrix.
	 * @return the job space id associated with the matrix.
	 * @author Albert Giegerich
	 */
	public Integer getJobSpaceId() {
		return jobSpaceId;
	}


	/**
	 * Getter for hasMultipleStagesField
	 * @return true if any job pairs in the matrix have multiple stages.
	 * @author Albert Giegerich
	 */
	public boolean hasMultipleStages() {
		return hasMultipleStages;
	}

	/**
	 * Gets a column header and truncates and adds an ellipsis to the end depending on the
	 * max size of matrix headers in R.java
	 * 
	 * @author Albert Giegerich
	 */
	public String getTruncatedColumnHeader(int column) {
		String solverName = solverConfigsByColumn.get(column).getLeft().getName();
		String truncatedSolverName = truncateAddEllipsisIfGreaterThanMax(solverName);

		String configName = solverConfigsByColumn.get(column).getRight().getName();
		String truncatedConfigName = truncateAddEllipsisIfGreaterThanMax(configName);

		return String.format("%s (%s)", truncatedSolverName, truncatedConfigName);
	}

	/**
	 * Adds a new element or replaces an old one at the given location.
	 * This method will fill in all empty spaces with null if the matrix
	 * is smaller than the specified dimensions.
	 * @param row the row to insert the new element.
	 * @param column the column to insert the new element.
	 * @param element the new element.
	 * @author Albert Giegerich
	 */
	public void set(int row, int column, MatrixElement element) {
		// Make sure there are enough rows to accommodate the new row.
		while (matrix.size() <= row) {
			matrix.add(new ArrayList<MatrixElement>());
		}
		// Make sure there are enough columns to accommodate the new column.
		// Fill in all ragged edges as well.
		for (ArrayList<MatrixElement> currentRow : matrix) {
			while (currentRow.size() <= column) {
				currentRow.add(null);
			}
		}
		matrix.get(row).set(column, element);
	}


	/**
	 * Gets all the headers for the rows of the Matrix.
	 * @return The row headers for the Matrix.
	 * @author Albert Giegerich
	 */
	public List<Benchmark> getBenchmarksByRow() {
		return benchmarksByRow;
	}

	/**
	 * Gets all the headers for the columns of the Matrix.
	 * @return The column headers for the Matrix.
	 * @author Albert Giegerich
	 */
	public List<Pair<Solver,Configuration>> getSolverConfigsByColumn() {
		return solverConfigsByColumn;
	}

	/**
	 * Gets the internal ArrayList<ArrayList> representation of the matrix for use with the JSP foreach element.
	 * @return the matrix field of the Matrix.
	 * @author Albert Giegerich
	 */
	public ArrayList<ArrayList<MatrixElement>> getInternalMatrixRepresentation() {
		return matrix;
	}

	/**
	 * Populates the row and column headers from two given lists.
	 * @author Albert Giegerich
	 */
	private void populateRowAndColumnHeaders(List<Benchmark> rowElements, List<Pair<Solver,Configuration>> columnElements) {
		for (Benchmark bench : rowElements) {
			this.benchmarksByRow.add(bench);
		}

		for (Pair<Solver,Configuration> solverConfig : columnElements) {
			this.solverConfigsByColumn.add(solverConfig);
		}
	}

	/**
	 * Truncate a String if it it greater than R.MATRIX_VIEW_COLUMN_HEADER and adds
	 * and ellipsis.
	 * @author Albert Giegerich
	 */
	private static String truncateAddEllipsisIfGreaterThanMax(String original) {
		boolean originalLongerThanMax = original.length() > R.MATRIX_VIEW_COLUMN_HEADER;
		String truncatedName = StringUtils.left(original, R.MATRIX_VIEW_COLUMN_HEADER);
		if (originalLongerThanMax) {
			truncatedName += "...";
		}
		return truncatedName;
	}	

	/**
	 * Builds a Set of Benchmarks and a Set of solver-configs and a mapping form benchmark-solverConfig pairs
	 * to the the data associated with them.
	 * @author Albert Giegerich
	 */
	private static void addToUniqueVectorListsAndIntersectionMap(
		Set<Benchmark> uniqueBenchmarks, 
		Set<Pair<Solver,Configuration>> uniqueSolverConfigs, 
		HashMap<Pair<Benchmark,Pair<Solver,Configuration>>,MatrixElement> vectorIntersectionToCellDataMap, 
		JobPair pair,
		Integer stageNumber) 
	{
		// Will get the primary stage if stageNumber <= 0.
		JoblineStage stage = pair.getStageFromNumber(stageNumber);
		// Filter out all job pairs that do not have the given stage.
		if (stage != null) {
			// Get the benchmark solver and config associated with the stage.
			Benchmark bench = pair.getBench();
			Solver solver = stage.getSolver();
			Configuration config = stage.getConfiguration();

			// Keep a list of the benchmarks and solver-configs pairs that have been found.
			uniqueBenchmarks.add(bench);

			Pair<Solver,Configuration> solverConfig = new ImmutablePair<Solver,Configuration>(solver,config);
			uniqueSolverConfigs.add(solverConfig);

			Pair<Benchmark,Pair<Solver,Configuration>> vectorIntersectionPoint =
			   new ImmutablePair<Benchmark,Pair<Solver,Configuration>>(bench, solverConfig);	

			MatrixElement jobPairCellData = getCellDataFromStageAndJobPairId(bench, stage, pair.getId());

			// Associate the benchmark-solverConfig intersection point with the data that should appear
			// at that point.
			vectorIntersectionToCellDataMap.put(vectorIntersectionPoint, jobPairCellData);
		}
	}

	/**
	 * Takes a JoblineStage and uses it's fields to build a matrix element.
	 * @param stage the stage 
	 * @author Albert Giegerich
	 */
	public static MatrixElement getCellDataFromStageAndJobPairId(Benchmark benchmark, JoblineStage stage, Integer jobPairId) {
		// Replace spaces with _ to make the status usable as a css class.
		String status = Jobs.getStatusFromStage(stage);
		String cpuTime = String.valueOf(stage.getCpuTime());
		String wallclock = String.valueOf(stage.getWallclockTime()); 
		String memUsage = String.valueOf(stage.getMaxVirtualMemory());

		if (jobPairId == null) {
			log.warn("(getCellDataFromStageAndJobPairId) jobPairId should not be null.");
		}

		Solver solver = stage.getSolver();
		Configuration config = stage.getConfiguration();
		String uniqueIdentifier = String.format(R.MATRIX_ELEMENT_ID_FORMAT, benchmark.getName(), benchmark.getId(), solver.getName(),solver.getId(), 
				config.getName(), config.getId());
		MatrixElement displayData = new MatrixElement(status, cpuTime, memUsage, wallclock, jobPairId, uniqueIdentifier);

		return displayData;
	}


	/**
	 * Initializes the matrices private fields.
	 * @author Albert Giegerich
	 */
	private void initializeMatrixFields(String jobSpaceName, Integer jobSpaceId) {
		// Initialize the Matrices' fields
		this.jobSpaceName = jobSpaceName;
		this.jobSpaceId = jobSpaceId;
		matrix = new ArrayList<ArrayList<MatrixElement>>();
		benchmarksByRow = new ArrayList<Benchmark>();
		solverConfigsByColumn = new ArrayList<Pair<Solver,Configuration>>();
		hasMultipleStages = false;
	}

	/**
	 * Takes a job pair and returns true if it has more than one stage.
	 * @author Albert Giegerich
	 */ 
	private static boolean testForMultipleStages(JobPair pair) {
		if (pair.getStages().size() > 1) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 * Populates the matrix by checking if each benchmark - solver-config intersection has cell data associated with it
	 * and adding it to the matrix if it does. 
	 * @author Albert Giegerich
	 */
	private void populateMatrixData(List<Benchmark> uniqueBenchmarkList, List<Pair<Solver,Configuration>> uniqueSolverConfigList, 
			HashMap<Pair<Benchmark,Pair<Solver,Configuration>>, MatrixElement> vectorIntersectionToCellDataMap) 
	{
		for (int i = 0; i < uniqueBenchmarkList.size(); i++) {
			Benchmark bench = uniqueBenchmarkList.get(i);
			for (int j = 0; j < uniqueSolverConfigList.size(); j++) {
				Pair<Solver,Configuration> solverConfig = uniqueSolverConfigList.get(j);
				Pair<Benchmark,Pair<Solver,Configuration>> vectorIntersectionPoint =
					new ImmutablePair<Benchmark,Pair<Solver,Configuration>>(bench, solverConfig);	
				MatrixElement cellData = vectorIntersectionToCellDataMap.get(vectorIntersectionPoint);
				this.set(i, j, cellData);
			}
		}
	}

	/**
	 * Gets a list of Pairs with the name and ID of each jobspace that the given job
	 * has in it.
	 * @author Albert Giegerich
	 */
	private static List<Pair<String, Integer>> getSpacesInJobOrderedAlphabetically(Job job) {
		final String method = "getSpacesInJobOrderedAlphabetically";
		log.debug("Entering method "+method);
		List<JobPair> jobPairs = job.getJobPairs();

		Set<Pair<String,Integer>> uniqueSpaces = new HashSet<Pair<String,Integer>>();

		// Build a set of all the spaces that exist for this job.
		for (JobPair pair : jobPairs) {
			String jobSpaceName = pair.getJobSpaceName();
			Integer jobSpaceId = pair.getJobSpaceId();
			Pair<String,Integer> jobSpaceNameAndId = new ImmutablePair<String,Integer>(jobSpaceName, jobSpaceId);
			uniqueSpaces.add(jobSpaceNameAndId);	
		}

		List<Pair<String, Integer>> orderedSpaces = new ArrayList<Pair<String,Integer>>(uniqueSpaces);
		Collections.sort(orderedSpaces, (jobSpaceA, jobSpaceB) -> {
			// Try sorting alphabetically insensitive to case.
			String jobSpaceNameA = jobSpaceA.getLeft();
			String jobSpaceNameB = jobSpaceB.getLeft();
			int alphabeticalComparison = jobSpaceNameA.compareToIgnoreCase(jobSpaceNameB);

			if (alphabeticalComparison != 0) {
				return alphabeticalComparison;
			}

			// If two elements are equal sort them by the value of their ID.
			Integer jobSpaceIdA = jobSpaceA.getRight();
			Integer jobSpaceIdB = jobSpaceB.getRight();
			return jobSpaceIdA.compareTo(jobSpaceIdB);
		});

		log.debug("(getSpacesInJobOrderedAlphabetically) Number of spaces in job = "+orderedSpaces.size());
		for (Pair<String, Integer> jobSpace : uniqueSpaces) {
			log.debug("    Name="+jobSpace.getLeft()+"  ID="+jobSpace.getRight());
		}

		return orderedSpaces;
	}

}
