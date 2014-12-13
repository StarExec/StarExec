package org.starexec.util;

/**
 * This is a simple class for timing events, basically like a stopwatch
 * @author Eric
 *
 */

public class Timer {
	
	private long startTime;
	/**
	 * Creates a new Timer object that immediately starts timing
	 */
	public Timer() {
		startTime=System.currentTimeMillis();
	}
	
	/**
	 * Returns the number of milliseconds that have elapsed since timing started
	 * @return
	 */
	public long getTime() {
		return (System.currentTimeMillis()-startTime);
	}
	
	/**
	 * Resets the time on this Timer so that timing restarts from 0. (meaning,
	 * getTime() would return 0 if called instantly after this method was called).
	 */
	public void reset() {
		startTime=System.currentTimeMillis();
	}
}
