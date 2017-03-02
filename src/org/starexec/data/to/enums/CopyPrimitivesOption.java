package org.starexec.data.to.enums;

/**
 * Created by agieg on 2/28/2017.
 */
public enum CopyPrimitivesOption {
    COPY,
    LINK,
    NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS;

    public boolean shouldCopySolvers() {
        return this == COPY;
    }

    public boolean shouldCopyBenchmarks() {
        return this == COPY;
    }

    public boolean shouldLinkSolvers() {
        return this == LINK || this == NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS;

    }

    public boolean shouldLinkAllBenchmarks() {
        return this == LINK;
    }

    public boolean shouldLinkSampleOfBenchmarks() {
        return this == NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS;
    }

    public boolean shouldLinkJobs() {
        return this != NO_JOBS_LINK_SOLVERS_SAMPLE_BENCHMARKS;
    }
}
