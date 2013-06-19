package org.starexec.util;

import org.apache.log4j.Logger;

public abstract class RobustRunnable implements Runnable {
    private static final Logger log = Logger.getLogger(RobustRunnable.class);	 
    
    protected String name;

    abstract protected void dorun();

    public RobustRunnable(String _name) {
	name = _name;
    }

    public void run() {
	try {
	    dorun();
	}
	catch (Throwable e) {
	    log.warn(name+" caught Throwable: "+e,e);
	}
    }
}
	

