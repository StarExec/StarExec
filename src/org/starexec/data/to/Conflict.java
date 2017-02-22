package org.starexec.data.to;

import com.google.gson.annotations.Expose;


/**
 * Created by agieg on 8/26/2016.
 */
public class Conflict {
    @Expose private Solver firstSolver;
    @Expose private Solver secondSolver;
    @Expose private Benchmark benchmark;
    @Expose private String firstResult;
    @Expose private String secondResult;

    public Conflict(Solver firstSolver, Solver secondSolver, Benchmark benchmark, String firstResult, String secondResult) {
        this.firstSolver = firstSolver;
        this.secondSolver = secondSolver;
        this.benchmark = benchmark;
        this.firstResult = firstResult;
        this.secondResult = secondResult;
    }
}
