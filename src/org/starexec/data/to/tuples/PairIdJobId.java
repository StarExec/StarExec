package org.starexec.data.to.tuples;

import java.util.Objects;


public class PairIdJobId {

    public final int pairId;
    public final int jobId;

    public PairIdJobId(int pairId, int jobId) {
        this.pairId = pairId;
        this.jobId = jobId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PairIdJobId)) {
            return false;
        }

        PairIdJobId castedOther = (PairIdJobId)other;

        return castedOther.pairId == this.pairId && castedOther.jobId == this.jobId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pairId, jobId);
    }
}
