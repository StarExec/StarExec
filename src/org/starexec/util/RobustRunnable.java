package org.starexec.util;


import org.starexec.logger.StarLogger;

public abstract class RobustRunnable implements Runnable {
    private static final StarLogger log = StarLogger.getLogger(RobustRunnable.class);
    
    protected String name;

    abstract protected void dorun();



    public RobustRunnable(String _name) {
    	name = _name;
    }

    @Override
    public void run() {
    	try {
			log.info(name + " (periodic)");
			dorun();
    	}
    	catch (Throwable e) {
    		log.warn(name+" caught throwable: "+e,e);
    	}
	finally {
	    log.info(name + " completed one periodic execution.");
	}
    }
}
	

