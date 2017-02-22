package org.starexec.data.to.tuples;

/**
 * Created by agieg on 9/19/2016.
 */
public class TimePair {
    private final String wallclock;
    private final String cpu;

    public TimePair(String wallclock, String cpu) {
        this.wallclock = wallclock;
        this.cpu = cpu;
    }

    public String getCpu() {
        return cpu;
    }

    public String getWallclock() {

        return wallclock;
    }
}
