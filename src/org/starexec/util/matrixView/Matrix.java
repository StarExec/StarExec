package org.starexec.util.matrixView;

import java.lang.Iterable;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import org.starexec.constants.R;
import org.starexec.data.database.JobPairs;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.compare.NameableComparators;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.util.Util;


/**
 * Represents the matrix on the details/jobMatrixView page.
 * @author Albert Giegerich
 */
public class Matrix {

	private ArrayList<Benchmark> benchmarksByRow;
	private ArrayList<Pair<Solver,Configuration>> solverConfigsByColumn;
	private ArrayList<ArrayList<MatrixElement>> matrix;
	private boolean hasMultipleStages;

	private static final Logger log = Logger.getLogger(Matrix.class);

	/**
	 * Builds a Matrix for the job matrix display page given a Job and a stageNumber.
	 * @param job the job to build a matrix display of.
	 * @param stageNumber filter the job pairs to view by this stage.
	 * @author Albert Giegerich
	 */
	public Matrix(Job job, int stageNumber) {
		try {
			log.debug("Entering Matrix constructor.");
			initializeMatrixFields();

			List<JobPair> jobPairs = job.getJobPairs();

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


			log.debug("(Matrix) sorting benchmarks and solver-config pairs.");
			// Sort the benchmarks alphabetically by name ignoring case.
			ArrayList<Benchmark> uniqueBenchmarkList = new ArrayList<Benchmark>(uniqueBenchmarks);
			Collections.sort(uniqueBenchmarkList, NameableComparators.getCaseInsensitiveAlphabeticalComparator());
			
			ArrayList<Pair<Solver,Configuration>> uniqueSolverConfigList = 
					new ArrayList<Pair<Solver,Configuration>>(uniqueSolverConfigs);
			// Names of solver config will be "solver (config)", sort the solverConfigs
			// alphabetically by name, ignore case.
			Collections.sort(uniqueSolverConfigList, new Comparator<Pair<Solver,Configuration>>() {
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
			log.debug("Leaving matrix constructor.");
		} catch (Exception e) {
			log.warn("Error in constructing matrix for matrix view page.", e);
		}
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
	public ArrayList<Benchmark> getBenchmarksByRow() {
		return benchmarksByRow;
	}

	/**
	 * Gets all the headers for the columns of the Matrix.
	 * @return The column headers for the Matrix.
	 * @author Albert Giegerich
	 */
	public ArrayList<Pair<Solver,Configuration>> getSolverConfigsByColumn() {
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
	private String truncateAddEllipsisIfGreaterThanMax(String original) {
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
	private void addToUniqueVectorListsAndIntersectionMap(
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

			MatrixElement jobPairCellData = getCellDataFromStageAndJobPairId(stage, pair.getId());

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
	private MatrixElement getCellDataFromStageAndJobPairId(JoblineStage stage, Integer jobPairId) {
		// Replace spaces with _ to make the status usable as a css class.
		String status = getStatusFromStage(stage);
		String cpuTime = String.valueOf(stage.getCpuTime());
		String wallclock = String.valueOf(stage.getWallclockTime()); 
		String memUsage = String.valueOf(stage.getMaxVirtualMemory());

		if (jobPairId == null) {
			log.warn("(getCellDataFromStageAndJobPairId) jobPairId should not be null.");
		}

		MatrixElement displayData = new MatrixElement(status, cpuTime, memUsage, wallclock, jobPairId);

		return displayData;
	}

	/**
	 * Gets a status description of a stage.
	 * @param stage the stage to get the status code from.
	 * @return the status code of the input stage.
	 * @author Albert Giegerich
	 */
	private String getStatusFromStage(JoblineStage stage) {
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
	 * Initializes the matrices private fields.
	 * @author Albert Giegerich
	 */
	private void initializeMatrixFields() {
		// Initialize the Matrices' fields
		matrix = new ArrayList<ArrayList<MatrixElement>>();
		benchmarksByRow = new ArrayList<Benchmark>();
		solverConfigsByColumn = new ArrayList<Pair<Solver,Configuration>>();
		hasMultipleStages = false;
	}

	/**
	 *
	 */ 
	private boolean testForMultipleStages(JobPair pair) {
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

}
