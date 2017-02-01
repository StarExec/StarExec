package org.starexec.app;

import java.io.File;
import java.io.IOException;
import java.lang.SecurityException;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.starexec.backend.*;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.Status;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecException;
import org.starexec.jobs.JobManager;
import org.starexec.jobs.ProcessingManager;
import org.starexec.test.integration.TestManager;
import org.starexec.util.ConfigUtil;
import org.starexec.util.Mail;
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
    // private Session session; // GridEngine session
	
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

		    R.BACKEND.destroyIf();
		    // Wait for the task scheduler to finish
		    taskScheduler.awaitTermination(10, TimeUnit.SECONDS);
		    taskScheduler.shutdownNow();
		    log.info("The task scheduler reports it was "+(taskScheduler.isTerminated() ? "" : "not ") 
			     +"terminated successfully.");
		    log.info("StarExec successfully shutdown");
		} catch (Exception e) {
		    log.error(e.getMessage(),e);
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
		try {
			log.info("Starexec running as "+Util.executeCommand("whoami"));
		} catch (IOException e1) {
			log.error("unable to execute whoami");
		}
		// Setup the path to starexec's configuration files
		R.CONFIG_PATH = new File(R.STAREXEC_ROOT, "/WEB-INF/classes/org/starexec/config/").getAbsolutePath();

		// Load all properties from the starexec-config file
		ConfigUtil.loadProperties(new File(R.CONFIG_PATH, "starexec-config.xml"));
		
		R.RUNSOLVER_PATH= new File(R.getSolverPath(),"runsolver").getAbsolutePath();

		try {
			FileUtils.copyFile(new File(R.CONFIG_PATH, "sge/runsolver"), new File(R.RUNSOLVER_PATH));
			Util.chmodDirectory(R.RUNSOLVER_PATH, false);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		try {
			R.BACKEND = R.getBackendFromType();
			log.info("backend = "+R.BACKEND.getClass());
		} catch (StarExecException e) {
			log.error(e.getMessage(),e);
		}
		
		// Initialize the datapool after properties are loaded
		Common.initialize();
		
		// Initialize the validator (compile regexes) after properties are loaded
		Validator.initialize();		
		
		
		if (R.IS_FULL_STAREXEC_INSTANCE) {
		    R.BACKEND.initialize(R.BACKEND_ROOT);
		}
		R.PUBLIC_USER_ID=Users.get("public").getId();
		
		System.setProperty("http.proxyHost",R.HTTP_PROXY_HOST);
		System.setProperty("http.proxyPort",R.HTTP_PROXY_PORT);

		// Schedule necessary periodic tasks to run
		this.scheduleRecurringTasks();		
		
		// Set any application variables to be used on JSP's with EL
		event.getServletContext().setAttribute("buildVersion", ConfigUtil.getBuildVersion());
		event.getServletContext().setAttribute("buildDate", ConfigUtil.getBuildDate());
		event.getServletContext().setAttribute("buildUser", ConfigUtil.getBuildUser());
		event.getServletContext().setAttribute("contactEmail", R.CONTACT_EMAIL);		
	}


	/**
	 * Creates and schedules periodic tasks to be run.
	 */
	private void scheduleRecurringTasks() {
		//created directories expected by the system to exist
		Util.initializeDataDirectories();
		
		TestManager.initializeTests();
		//Schedule the recurring tasks above to be run every so often


		Set<PeriodicTasks.PeriodicTask> periodicTasks = EnumSet.allOf(PeriodicTasks.PeriodicTask.class);

		for (PeriodicTasks.PeriodicTask task : periodicTasks) {
			if ( R.IS_FULL_STAREXEC_INSTANCE || !task.fullInstanceOnly ) {
				taskScheduler.scheduleAtFixedRate(task.task, task.delay, task.period, task.unit);
			}
		}
		try {
			PaginationQueries.loadPaginationQueries();
		} catch (Exception e) {
			log.error("unable to correctly load pagination queries");
			log.error(e.getMessage(),e);
		}
	}
}
