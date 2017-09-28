package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Created by agieg on 8/26/2016.
 */
public class Conflict {
	@Expose private final Solver firstSolver;
	@Expose private final Solver secondSolver;
	@Expose private final Benchmark benchmark;
	@Expose private final String firstResult;
	@Expose private final String secondResult;

	public Conflict(
			Solver firstSolver, Solver secondSolver, Benchmark benchmark, String firstResult, String secondResult
	) {
		this.firstSolver = firstSolver;
		this.secondSolver = secondSolver;
		this.benchmark = benchmark;
		this.firstResult = firstResult;
		this.secondResult = secondResult;
	}
}
