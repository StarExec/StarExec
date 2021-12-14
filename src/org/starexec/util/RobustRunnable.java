package org.starexec.util;

import org.starexec.util.Timer;
import org.starexec.logger.StarLogger;

public abstract class RobustRunnable implements Runnable {
	private static final StarLogger log = StarLoggerFactory.getLogger(RobustRunnable.class);

	protected final String name;

	abstract protected void dorun();

	public RobustRunnable(String _name) {
		name = _name;
	}

	@Override
	public void run() {
		Timer timer = new Timer();
		try {
			log.info(name + " (periodic)");
			dorun();
		} catch (Throwable e) {
			log.warn(name + " caught throwable: " + e, e);
		} finally {
			log.info(name + " completed one periodic execution in " + timer.getTime() + " milliseconds.");
		}
	}
}
