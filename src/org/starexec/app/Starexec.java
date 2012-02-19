package org.starexec.app;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.starexec.constants.R;
import org.starexec.data.database.Common;
import org.starexec.util.ConfigUtil;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Validator;

/**
 * Class which listens for application events (mainly startup/shutdown)
 * and does any required setup/teardown.
 * 
 * @author Tyler Jensen
 */
public class Starexec implements ServletContextListener {
	private static final Logger log = Logger.getLogger(Starexec.class);
	private static final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(2);
	
	private static String ROOT_APPLICATION_PATH = "";	
	
	// Path of the starexec config and log4j files which are needed at compile time to load other resources
	private static String CONFIG_PATH = "/WEB-INF/classes/org/starexec/config/starexec-config.xml";
	private static String LOG4J_PATH = "/WEB-INF/classes/org/starexec/config/log4j.properties";
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			// Stop the task scheduler since it freezes in an unorderly shutdown...
			log.debug("Stopping starexec task scheduler...");
			taskScheduler.shutdown();
			
			// Make sure to clean up database resources
			log.debug("Releasing database connections...");
			Common.release();
			
			// Wait for the task scheduler to finish
			taskScheduler.awaitTermination(10, TimeUnit.SECONDS);
			log.info("Starexec successfully shutdown");
		} catch (Exception e) {
			log.error(e);
			log.error("Starexec unclean shutdown");
		}		
	}

	/**
	 * When the application starts, this method is called. Perform any initializations here
	 */	
	@Override
	public void contextInitialized(ServletContextEvent event) {				
		// Remember the application's root so we can load properties from it later
		Starexec.ROOT_APPLICATION_PATH = event.getServletContext().getRealPath("/");
		log.info(String.format("Application started at [%s]", ROOT_APPLICATION_PATH));
		
		// Before we do anything we must configure log4j!
		PropertyConfigurator.configure(new File(ROOT_APPLICATION_PATH, LOG4J_PATH).getAbsolutePath());
										
		// Load all properties from the starexec-config file
		ConfigUtil.loadProperties(new File(ROOT_APPLICATION_PATH, CONFIG_PATH));
		
		// Initialize the datapool after properties are loaded
		Common.initialize();
		
		// Initialize the validator (compile regexes) after properties are loaded
		Validator.initialize();		
		
		// Create a task that updates the cluster usage info (this may take some time)
		final Runnable updateClusterTask = new Runnable() {			
			@Override
			public void run() {
				GridEngineUtil.loadWorkerNodes();
				GridEngineUtil.loadQueues();
			}
		};	
		
		// Create a task that updates statistics of jobs that are finished
		final Runnable processJobStatsTask = new Runnable() {			
			@Override
			public void run() {
				if(GridEngineUtil.isAvailable()) {
					GridEngineUtil.processStatistics();
				}
			}
		};		
		
		// Schedule the cluster update task to be run every so often as specified in the config file
		taskScheduler.scheduleAtFixedRate(updateClusterTask, 0, R.CLUSTER_UPDATE_PERIOD, TimeUnit.SECONDS);
		
		// Schedule statistics processing to run every so often
		taskScheduler.scheduleAtFixedRate(processJobStatsTask, 0, R.SGE_STATISTICS_PERIOD, TimeUnit.SECONDS);
		
		// Set any application variables to be used on JSP's with EL
		event.getServletContext().setAttribute("buildVersion", ConfigUtil.getBuildVersion());
		event.getServletContext().setAttribute("buildDate", ConfigUtil.getBuildDate());
		event.getServletContext().setAttribute("buildUser", ConfigUtil.getBuildUser());
		event.getServletContext().setAttribute("contactEmail", R.CONTACT_EMAIL);		
		event.getServletContext().setAttribute("isProduction", ConfigUtil.getConfigName().equals("production"));
	}	
}