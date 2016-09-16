package org.starexec.data.to.tuples;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by agieg on 9/15/2016.
 */
public class SolverConfig {

    public final Integer solverId;
    public Optional<String> solverName = Optional.empty();
    public final Integer configId;
    public Optional<String> configName = Optional.empty();

    public SolverConfig(Integer solverId, Integer configId) {
        this.solverId = solverId;
        this.configId = configId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SolverConfig)) {
            return false;
        } else if (this == obj) {
            return true;
        }

        SolverConfig sc = (SolverConfig)obj;
        return (this.solverId.equals(sc.solverId) && this.configId.equals(sc.configId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(solverId, configId);
    }
}
