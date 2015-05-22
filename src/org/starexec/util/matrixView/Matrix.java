package org.starexec.util.matrixView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;


public class Matrix {
	private int rows;
	private int cols;
	private int index;
	private ArrayList<String> rowHeaders;
	private ArrayList<String> columnHeaders;
	private ArrayList<ArrayList<MatrixElement>> matrix;


	/**
	 * Private default constructor
	 */
	private Matrix() {
		rows = 0;
		cols = 0;
		index = 0;
		matrix = new ArrayList<ArrayList<MatrixElement>>();
		rowHeaders = new ArrayList<String>();
		columnHeaders = new ArrayList<String>();
		int benchmarkNumber = 50;
		int solverNumber = 20;

		for (int i = 0; i < solverNumber; i++) {
			matrix.add(new ArrayList<MatrixElement>());
		}


		for (int i = 0; i < solverNumber; i++) {
			for (int j = 0; j < benchmarkNumber; j++) {
				String success = "success";
				if ((i+j)%5 == 0) {
					success = "failure";
				}
				matrix.get(i).add(new MatrixElement(success, "0.0s", "0KB", "0.0s"));
			}
		}

		for (int i = 0; i < solverNumber; i++) {
			rowHeaders.add("Solver" + String.valueOf(i));	
		}

		for (int i = 0; i < benchmarkNumber; i++) {
			columnHeaders.add("Benchmark" + String.valueOf(i));
		}
	}

	public MatrixElement getElement(int row, int column) {
		return matrix.get(row).get(column);
	}

	public ArrayList<String> getRowHeaders() {
		return rowHeaders;
	}

	public ArrayList<String> getColumnHeaders() {
		return columnHeaders;
	}

	public int getNumberOfRows() {
		return rows;
	}

	public int getNumberOfColumns() {
		return cols;
	}

	public static Matrix buildMatrixFromJob(Job job) {
		Matrix matrix = new Matrix();
		return matrix;
	}

	public static HashMap<Pair<Solver, Benchmark>, MatrixElement> buildPairToDataMap(List<JobPair> jobPairs) {
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
				MatrixElement pairData = new MatrixElement("a", "b", "c", "d");
				/*
				outputPairToDataMap.put(solverBenchmark, pairData);
				*/
			}
		}

		return outputPairToDataMap;
	}
}
