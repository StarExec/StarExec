package com.starexec.app;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.starexec.constants.R;


public class AppListener implements ServletContextListener {
	
	private static final Logger log = Logger.getLogger(AppListener.class);
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
	}

	/**
	 * When the application starts, this method is called. Perform any initializations here
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {		
		checkTestPath();		
	}
	
	/**
	 * Checks if the application is running on a local dev machine and sets up
	 * the appropriate paths to test locally.
	 */
	private void checkTestPath(){
		File testDir = new File("C:\\Users\\Tyler\\Documents\\Docs\\University of Iowa\\Research\\STAREXEC\\Benchmarks\\");
		
		if(testDir.exists()) {
		    R.SOLVER_PATH = "C:\\Users\\Tyler\\Documents\\Docs\\University of Iowa\\Research\\STAREXEC\\Solvers\\";			// The directory in which to save the solver file(s)
		    R.BENCHMARK_PATH = "C:\\Users\\Tyler\\Documents\\Docs\\University of Iowa\\Research\\STAREXEC\\Benchmarks\\";	// The directory in which to save the benchmark file(s)
		    		    
		    log.debug("Tyler local development paths set");
		}
	}
}
