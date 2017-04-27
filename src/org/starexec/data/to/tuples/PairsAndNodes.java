package org.starexec.data.to.tuples;

import com.google.common.collect.ImmutableSet;


public class PairsAndNodes {

    public final ImmutableSet<PairIdJobId> jobPairIds;
    public final ImmutableSet<Integer> nodeIds;

    public PairsAndNodes(ImmutableSet<PairIdJobId> jobPairIds, ImmutableSet<Integer> nodeIds) {
        this.jobPairIds = jobPairIds;
        this.nodeIds = nodeIds;
    }
}
