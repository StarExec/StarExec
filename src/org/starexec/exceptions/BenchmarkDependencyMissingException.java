package org.starexec.exceptions;

import org.starexec.data.database.Benchmarks;
import org.starexec.data.to.Benchmark;

import java.sql.SQLException;
import java.util.List;

public class BenchmarkDependencyMissingException extends StarExecException {
    public BenchmarkDependencyMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BenchmarkDependencyMissingException(String message) {
        super(message);
    }
    
    private static String getMissingDepsMessage(int benchId) {
        try {
            List<Benchmark> missingDependencies = Benchmarks.getBrokenBenchDependencies(benchId);
            String missingDeps = "";
            for(Benchmark bench : missingDependencies) {
                missingDeps = bench.getName() + ", ";
            }
            return "Missing dependencies: " + missingDeps;
        } catch (SQLException e) {
            return "Database Error: Could not retrieve missing dependencies";
        }

    }
    
    public BenchmarkDependencyMissingException(int benchId) {
        super(getMissingDepsMessage(benchId));
    }
}

