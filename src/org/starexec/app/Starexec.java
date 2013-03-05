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
import org.starexec.jobs.JobManager;
import org.starexec.util.ConfigUtil;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Class which listens for application events (mainly startup/shutdown)
 * and does any required setup/teardown.
 * 
 * @author Tyler Jensen
 */
public class Starexec implements ServletContextListener {
	private static final Logger log = Logger.getLogger(Starexec.class);
	private static final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(5);	
	
	// Path of the starexec config and log4j files which are needed at compile time to load other resources
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
			
			log.debug("Releasing grid engine util threadpool...");
			GridEngineUtil.shutdown();
			
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
		R.STAREXEC_ROOT = event.getServletContext().getRealPath("/");
		log.info(String.format("Application started at [%s]", R.STAREXEC_ROOT));
		// Before we do anything we must configure log4j!
		PropertyConfigurator.configure(new File(R.STAREXEC_ROOT, LOG4J_PATH).getAbsolutePath());
										
		// Setup the path to starexec's configuration files
		R.CONFIG_PATH = new File(R.STAREXEC_ROOT, "/WEB-INF/classes/org/starexec/config/").getAbsolutePath();
		
		// Load all properties from the starexec-config file
		ConfigUtil.loadProperties(new File(R.CONFIG_PATH, "starexec-config.xml"));
		
		// Initialize the datapool after properties are loaded
		Common.initialize();
		
		// Initialize the validator (compile regexes) after properties are loaded
		Validator.initialize();		
		
		// Schedule necessary periodic tasks to run
		this.scheduleRecurringTasks();		
		
		// Set any application variables to be used on JSP's with EL
		event.getServletContext().setAttribute("buildVersion", ConfigUtil.getBuildVersion());
		event.getServletContext().setAttribute("buildDate", ConfigUtil.getBuildDate());
		event.getServletContext().setAttribute("buildUser", ConfigUtil.getBuildUser());
		event.getServletContext().setAttribute("contactEmail", R.CONTACT_EMAIL);		
		event.getServletContext().setAttribute("isProduction", ConfigUtil.getConfigName().equals("production"));
	}	
	
	/**
	 * Creates and schedules periodic tasks to be run.
	 */
	private void scheduleRecurringTasks() {
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
					GridEngineUtil.processResults();
					//log.info("Skipping ALL Results Processing...");
					//Common.getDataPoolData();
				}
			}
		};	
		
		// Create a task that submits jobs that have pending/rejected job pairs
		final Runnable submitJobsTask = new Runnable() {			
			@Override
			public void run() {
				if(GridEngineUtil.isAvailable()) {
					JobManager.checkPendingJobs();
					//log.info("Skipping ALL Results Processing...");
					//Common.getDataPoolData();
				}
			}
		};

		// Create a task that deletes download files older than 1 day
		final Runnable clearDownloadsTask = new Runnable() {			
			@Override
			public void run() {
				Util.clearOldFiles(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR).getAbsolutePath(), 1);
			}
		};	
		
		// Create a task that deletes job logs older than 30 days
		final Runnable clearJobLogTask = new Runnable() {			
			@Override
			public void run() {
				Util.clearOldFiles(R.JOB_LOG_DIR, 30);
			}
		};	
		
		/* Schedule the recurring tasks above to be ran every so often
		taskScheduler.scheduleAtFixedRate(updateClusterTask, 0, R.CLUSTER_UPDATE_PERIOD, TimeUnit.SECONDS);	
		taskScheduler.scheduleAtFixedRate(processJobStatsTask, 0, R.SGE_STATISTICS_PERIOD, TimeUnit.SECONDS);
		taskScheduler.scheduleAtFixedRate(submitJobsTask, 0, R.JOB_SUBMISSION_PERIOD, TimeUnit.SECONDS);
		taskScheduler.scheduleAtFixedRate(clearDownloadsTask, 0, 1, TimeUnit.HOURS);
		taskScheduler.scheduleAtFixedRate(clearJobLogTask, 0, 12, TimeUnit.HOURS);*/
	
	}
}