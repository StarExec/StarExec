package org.starexec.util.matrixView;

import java.lang.Iterable;
import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.compare.NameableComparators;


/**
 * Represents the matrix on the details/jobMatrixView page.
 * @author Albert Giegerich
 */
public class Matrix {
	private ArrayList<String> rowHeaders;
	private ArrayList<String> columnHeaders;
	private ArrayList<ArrayList<MatrixElement>> matrix;


	// for testing purposes
	// TODO remove
	private Matrix() {
		matrix = new ArrayList<ArrayList<MatrixElement>>();
		rowHeaders = new ArrayList<String>();
		columnHeaders = new ArrayList<String>();
		int benchmarkNumber = 50;
		int solverNumber = 20;

		for (int i = 0; i < benchmarkNumber; i++) {
			matrix.add(new ArrayList<MatrixElement>());
			for (int j = 0; j < solverNumber; j++) {
				String success = "success";
				if ((i+j)%5 == 0) {
					success = "failure";
				}
				matrix.get(i).add(new MatrixElement(success, "0.0s", "0KB", "0.0s"));
			}
		}

		for (int i = 0; i < solverNumber; i++) {
			columnHeaders.add("Solver" + String.valueOf(i));	
		}

		for (int i = 0; i < benchmarkNumber; i++) {
			rowHeaders.add("Benchmark" + String.valueOf(i));
		}
	}

	/**
	 * Private constructor that builds the Matrix based on data from a HashMap
	 * @author Albert Giegerich
	 */
	private Matrix(HashMap<Pair<Solver, Benchmark>, MatrixElement> solverBenchmarkToMatrixElementMap) {
		Set<Pair<Solver, Benchmark>> keys = solverBenchmarkToMatrixElementMap.keySet();

		// Build a set of each unique solver and a set of each unique benchmark.
		Set<Solver> uniqueSolvers = new HashSet();
		Set<Benchmark> uniqueBenchmarks = new HashSet();
		for (Pair<Solver, Benchmark> solverBenchmark : keys) {
			uniqueSolvers.add(solverBenchmark.getLeft());
			uniqueBenchmarks.add(solverBenchmark.getRight());
		}

		// Convert uniqueSolvers to a sorted ArrayList, sorted alphabetically insensitive to case.
		// NOTE: new Solver[0] is only specifying the type of array to create.
		Solver[] uniqueSolverArray = uniqueSolvers.toArray(new Solver[0]);
		ArrayList<Solver> orderedUniqueSolvers = new ArrayList<Solver>(Arrays.asList(uniqueSolverArray));
		Collections.sort(orderedUniqueSolvers, NameableComparators.caseInsensitiveAlphabeticalComparator()); 

		// Convert uniqueBenchmarks to a sorted ArrayList, sorted alphabetically insensitive to case.
		Benchmark[] uniqueBenchmarkArray = uniqueBenchmarks.toArray(new Benchmark[0]);
		ArrayList<Benchmark> orderedUniqueBenchmarks = new ArrayList<Benchmark>(Arrays.asList(uniqueBenchmarkArray));
		Collections.sort(orderedUniqueBenchmarks, NameableComparators.caseInsensitiveAlphabeticalComparator()); 


		initializeMatrixFields();

		buildMatrixHeadersAndElements(orderedUniqueSolvers, orderedUniqueBenchmarks, solverBenchmarkToMatrixElementMap);

	}

	private void initializeMatrixFields() {
		// Initialize the Matrices' fields
		matrix = new ArrayList<ArrayList<MatrixElement>>();
		rowHeaders = new ArrayList<String>();
		columnHeaders = new ArrayList<String>();
	}

	private void buildMatrixHeadersAndElements(List<Solver> solvers, List<Benchmark> benchmarks, 
		HashMap<Pair<Solver, Benchmark>, MatrixElement> solverBenchmarkToMatrixElementMap) {

		// Build the column headers using each solver's name.
		for (Solver solver : solvers) {
			columnHeaders.add(solver.getName());
		}

		// Build the row headers using each benchmark's name.
		for (Benchmark benchmark : benchmarks) {
			rowHeaders.add(benchmark.getName());
		}


		for (int i = 0; i < solvers.size(); i++) {
			matrix.add(new ArrayList<MatrixElement>());

			ArrayList<MatrixElement> currentRow = matrix.get(i);

			Solver solver = solvers.get(i);
			for (int j = 0; j < benchmarks.size(); j++) {
				Benchmark benchmark = benchmarks.get(j);
				Pair<Solver, Benchmark> solverBenchmark = new ImmutablePair(solver, benchmark);
				if (solverBenchmarkToMatrixElementMap.containsKey(solverBenchmark)) {
					currentRow.add(solverBenchmarkToMatrixElementMap.get(solverBenchmark));
				} else {
					currentRow.add(null);
				}
			}
		}	
	}

	/**
	 * Gets all the headers for the rows of the Matrix.
	 * @return The row headers for the Matrix.
	 * @author Albert Giegerich
	 */
	public ArrayList<String> getRowHeaders() {
		return rowHeaders;
	}

	/**
	 * Gets all the headers for the columns of the Matrix.
	 * @return The column headers for the Matrix.
	 * @author Albert Giegerich
	 */
	public ArrayList<String> getColumnHeaders() {
		return columnHeaders;
	}

	/**
	 * Builds a Matrix based on the JobPairs in a given Job.
	 * @return The new Matrix.
	 * @author Albert Giegerich
	 */
	public static Matrix buildMatrixFromJob(Job job) {
		List<JobPair> jobPairs = job.getJobPairs();
		// Build a mapping of solver-benchmarks pairs to MatrixElement data so we can quickly retrieve the
		// data when we need to fill the Matrix.
		HashMap<Pair<Solver, Benchmark>, MatrixElement> pairToDataMap = buildPairToDataMap(jobPairs);
		Matrix newMatrix = new Matrix(pairToDataMap);
		return newMatrix;
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
	 * 
	 */
	private static HashMap<Pair<Solver, Benchmark>, MatrixElement> buildPairToDataMap(List<JobPair> jobPairs) {
		// return value
		HashMap<Pair<Solver, Benchmark>, MatrixElement> outputPairToDataMap = new HashMap<Pair<Solver, Benchmark>, MatrixElement>();

		for (JobPair pair : jobPairs) {
			Solver solver = pair.getPrimarySolver();
			Benchmark benchmark = pair.getBench();
			Pair<Solver, Benchmark> solverBenchmark = new ImmutablePair<Solver, Benchmark>(solver, benchmark);

			// build a set of all unique solver names and all unique benchmark names
			if (!outputPairToDataMap.containsKey(solverBenchmark)) {
				String status = pair.getStatus().getStatus();
				String runningTime = String.valueOf(pair.getEndTime().getTime() - pair.getStartTime().getTime());  
				String memUsage = String.valueOf(pair.getPrimaryMaxVirtualMemory());
				String wallclockTime = String.valueOf(pair.getPrimaryWallclockTime());
				MatrixElement pairData = new MatrixElement(status, runningTime, memUsage, wallclockTime);
				outputPairToDataMap.put(solverBenchmark, pairData);
			}
		}

		return outputPairToDataMap;
	}
}
