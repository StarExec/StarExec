package org.starexec.data.to.tuples;

// Simple tuple to count number of high priority jobs and all jobs.
public class JobCount {
    public int all;
    public int highPriority;

    public JobCount(int all, int highPriority) {
        this.all = all;
        this.highPriority = highPriority;
    }
}
