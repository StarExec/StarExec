package org.starexec.data.to.tuples;

// Simple tuple that contains a wallclock time and a cpu time.
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
