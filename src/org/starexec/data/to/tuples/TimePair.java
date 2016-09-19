package org.starexec.data.to.tuples;

/**
 * Created by agieg on 9/19/2016.
 */
public class TimePair {
    private final double wallclock;
    private final double cpu;

    public TimePair(double wallclock, double cpu) {
        this.wallclock = wallclock;
        this.cpu = cpu;
    }

    public double getWallclock() {
        return wallclock;
    }

    public double getCpu() {
        return cpu;
    }
}
