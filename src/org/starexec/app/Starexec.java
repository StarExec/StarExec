package org.starexec.app;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ggf.drmaa.Session;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Common;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.jobs.JobManager;
import org.starexec.jobs.ProcessingManager;
import org.starexec.servlets.ProcessorManager;
import org.starexec.test.TestManager;
import org.starexec.util.ConfigUtil;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.RobustRunnable;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Class which listens for application events (mainly startup/shutdown)
 * and does any required setup/teardown.
 * 
 * @author Tyler Jensen
 */
public class Starexec implements ServletContextListener {
    private Logger log;
    private ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(10);	
    private Session session; // GridEngine session
	
	// Path of the starexec config and log4j files which are needed at compile time to load other resources
	private static String LOG4J_PATH = "/WEB-INF/classes/org/starexec/config/log4j.properties";
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
		    log.info("Initiating shutdown of StarExec.");
		    // Stop the task scheduler since it freezes in an unorderly shutdown...
		    log.debug("Stopping starexec task scheduler...");
		    taskScheduler.shutdown();
			
		    // Make sure to clean up database resources
		    log.debug("Releasing database connections...");
		    Common.release();
			
		    log.debug("Releasing Util threadpool...");
		    Util.shutdownThreadPool();
			
		    log.debug("session = " + session);
			
		    if (!session.toString().contains("drmaa")) {
			log.debug("Shutting down the session..." + session);
			GridEngineUtil.destroySession(session);
		    }
		    // Wait for the task scheduler to finish
		    taskScheduler.awaitTermination(10, TimeUnit.SECONDS);
		    taskScheduler.shutdownNow();
		    log.info("The task scheduler reports it was "+(taskScheduler.isTerminated() ? "" : "not ") 
			     +"terminated successfully.");
		    log.info("StarExec successfully shutdown");
		} catch (Exception e) {
		    log.error(e);
		    log.error("StarExec unclean shutdown");
		}		
	}

	/**
	 * When the application starts, this method is called. Perform any initializations here
	 */	
	@Override
	public void contextInitialized(ServletContextEvent event) {				
		// Remember the application's root so we can load properties from it later
		R.STAREXEC_ROOT = event.getServletContext().getRealPath("/");
		// Before we do anything we must configure log4j!
		PropertyConfigurator.configure(new File(R.STAREXEC_ROOT, LOG4J_PATH).getAbsolutePath());
										
		log = Logger.getLogger(Starexec.class);
		
		log.info(String.format("StarExec started at [%s]", R.STAREXEC_ROOT));

		// Setup the path to starexec's configuration files
		R.CONFIG_PATH = new File(R.STAREXEC_ROOT, "/WEB-INF/classes/org/starexec/config/").getAbsolutePath();
		
		// Load all properties from the starexec-config file
		ConfigUtil.loadProperties(new File(R.CONFIG_PATH, "starexec-config.xml"));
		
		// Initialize the datapool after properties are loaded
		Common.initialize();
		
		// Initialize the validator (compile regexes) after properties are loaded
		Validator.initialize();		
		
		if (R.RUN_PERIODIC_SGE_TASKS) {
		    session = GridEngineUtil.createSession();
		    JobManager.setSession(session);
		    log.info("Created GridEngine session");
		}

		System.setProperty("http.proxyHost",R.HTTP_PROXY_HOST);
		System.setProperty("http.proxyPort",R.HTTP_PROXY_PORT);

		// Schedule necessary periodic tasks to run
		this.scheduleRecurringTasks();		
		
		// Set any application variables to be used on JSP's with EL
		event.getServletContext().setAttribute("buildVersion", ConfigUtil.getBuildVersion());
		event.getServletContext().setAttribute("buildDate", ConfigUtil.getBuildDate());
		event.getServletContext().setAttribute("buildUser", ConfigUtil.getBuildUser());
		event.getServletContext().setAttribute("contactEmail", R.CONTACT_EMAIL);		
		event.getServletContext().setAttribute("starexecRoot", R.STAREXEC_APPNAME);		
		event.getServletContext().setAttribute("isProduction", ConfigUtil.getConfigName().equals("production"));
		
	}	
	
	/**
	 * Creates and schedules periodic tasks to be run.
	 */
	private void scheduleRecurringTasks() {
		// Create a task that updates the cluster usage info (this may take some time)
		final Runnable updateClusterTask = new RobustRunnable("updateClusterTask") {			
			@Override
			protected void dorun() {
			    log.info("updateClusterTask (periodic)");
			    GridEngineUtil.loadWorkerNodes();
			    GridEngineUtil.loadQueues();
			}
		};	
		
		
		// Create a task that submits jobs that have pending/rejected job pairs
		final Runnable submitJobsTask = new RobustRunnable("submitJobTasks") {			
			@Override
			protected void dorun() {
			    log.info("submitJobsTask (periodic)");
			    try {
			    	JobManager.checkPendingJobs();
			    }
			    catch(Exception e) {
				log.warn("submitJobsTask caught exception: "+e,e);
			    }
			}
		};
		
		// Create a task that submits jobs that have pending/rejected job pairs
		final Runnable postProcessJobsTask = new RobustRunnable("postProcessJobsTask") {			
			@Override
			protected void dorun() {
			    log.info("checkProcessJobsTask (periodic)");
			    try {
			    	ProcessingManager.checkProcessingPairs();
			    }
			    catch(Exception e) {
				log.warn("postProcessJobsTask caught exception: "+e,e);
			    }
			}
		};

		// Create a task that deletes download files older than 1 day
		final Runnable clearDownloadsTask = new RobustRunnable("clearDownloadsTask") {			
			@Override
			protected void dorun() {
			    log.info("clearDownloadsTask (periodic)");
				Util.clearOldFiles(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR).getAbsolutePath(), 1,false);
				Util.clearOldCachedFiles(14);
				//even though we're clearing unused cache files, they still might build up for a variety
				//of reasons. To stay robust, we should probably still clear out very old ones
				Util.clearOldFiles(new File(R.STAREXEC_ROOT,R.CACHED_FILE_DIR).getAbsolutePath(), 60,false);
			}
		};	
		
		/*  Create a task that deletes job logs older than 3 days */
		final Runnable clearJobLogTask = new RobustRunnable("clearJobLogTask") {			
			@Override
			protected void dorun() {
			    log.info("clearJobLogTask (periodic)");
				Util.clearOldFiles(R.JOB_LOG_DIR, 30,true);
			}
		};
		/*  Create a task that deletes job logs older than 3 days */
		final Runnable clearJobScriptTask = new RobustRunnable("clearJobScriptTask") {			
			@Override
			protected void dorun() {
			    log.info("clearJobScriptTask (periodic)");
				Util.clearOldFiles(R.JOB_INBOX_DIR,1,false);
				Util.clearOldFiles(R.JOBPAIR_INPUT_DIR, 1,false);
			}
		};
		/**
		 * Removes solvers and benchmarks from the database that are both orphaned (unaffiliated
		 * with any spaces or job pairs) AND have already been deleted on disk.
		 */
		final Runnable cleanDatabaseTask = new RobustRunnable("cleanDatabaseTask") {
			@Override
			protected void dorun() {
				log.info("cleanDatabaseTask (periodic)");
				Solvers.cleanOrphanedDeletedSolvers();
				Benchmarks.cleanOrphanedDeletedBenchmarks();
				Jobs.cleanOrphanedDeletedJobs();
			}
		};
		
		final Runnable checkQueueReservations = new RobustRunnable("checkQueueReservations") {
			@Override
			protected void dorun() {
				log.info("checkQueueReservationsTask (periodic)");
				GridEngineUtil.checkQueueReservations();
			}
		};
		
		//created directories expected by the system to exist
		Util.initializeDataDirectories();
		
		TestManager.initializeTests();
		//Schedule the recurring tasks above to be run every so often
		if (R.RUN_PERIODIC_SGE_TASKS) {
		    taskScheduler.scheduleAtFixedRate(updateClusterTask, 0, R.CLUSTER_UPDATE_PERIOD, TimeUnit.SECONDS);	
		    taskScheduler.scheduleAtFixedRate(submitJobsTask, 0, R.JOB_SUBMISSION_PERIOD, TimeUnit.SECONDS);
		    taskScheduler.scheduleAtFixedRate(clearDownloadsTask, 0, 1, TimeUnit.HOURS);
		    taskScheduler.scheduleAtFixedRate(clearJobLogTask, 0, 7, TimeUnit.DAYS);
		    taskScheduler.scheduleAtFixedRate(clearJobScriptTask, 0, 12, TimeUnit.HOURS);

		    // taskScheduler.scheduleAtFixedRate(cleanDatabaseTask, 0, 7, TimeUnit.DAYS);

		    // this task seems to have a number of problems currently, such as leaving jobs paused after it runs
		    //taskScheduler.scheduleAtFixedRate(checkQueueReservations, 0, 30, TimeUnit.SECONDS);

		    taskScheduler.scheduleAtFixedRate(postProcessJobsTask,0,45,TimeUnit.SECONDS);
		}
		//THIS IS A ONE TIME TASK! It initializes the job space closure table
		//Jobs.addAllJobSpaceClosureEntries();
		

	}
	
}
