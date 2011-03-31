package app;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import constants.R;

public class AppListener implements ServletContextListener {
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
		File testDir = new File("C:\\Users\\Tyler\\Desktop\\");
		
		if(testDir.exists()) {
		    R.SOLVER_PATH = "C:\\Users\\Tyler\\Desktop\\SOLVERS\\";			// The directory in which to save the solver file(s)
		    R.BENCHMARK_PATH = "C:\\Users\\Tyler\\Desktop\\Benchmarks\\";	// The directory in which to save the benchmark file(s)		    
		}
	}
}
