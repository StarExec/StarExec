package org.starexec.app;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Logger;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.database.Analytics;
import org.starexec.data.database.Common;
import org.starexec.data.database.Users;
import org.starexec.exceptions.StarExecException;
import org.starexec.logger.StarLogger;
import org.starexec.test.integration.TestManager;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// import java.lang.reflect.Field;

/**
 * Class which listens for application events (mainly startup/shutdown)
 * and does any required setup/teardown.
 *
 * @author Tyler Jensen
 */
public class Starexec implements ServletContextListener {
	private StarLogger log;
	private final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(10);
	// private Session session; // GridEngine session

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			log.info("Initiating shutdown of StarExec.");

			StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
			String stackString = "";
			for ( StackTraceElement element : stacktrace ) {
				// stackString += element.toString()+"\t"+ClassLoader.findClass(element.getClassName()).getResource(".class")+"\n";
				stackString += element.toString()+"\n";
			}
			log.debug( "\n\ncontextDestroyed() stackString:\n"+stackString+"\n" );

			log.debug( "\n\nServletContext info:\ngetInitParameterNames(): "+arg0.getServletContext().getInitParameterNames()+
					"\ntoString(): "+arg0.toString()+"\n" );



			// Stop the task scheduler since it freezes in an unorderly shutdown...
			log.debug("Stopping starexec task scheduler...");
			taskScheduler.shutdown();

			// Save cached Analytics events to DB
			Analytics.saveToDB();

			// Make sure to clean up database resources
			log.debug("Releasing database connections...");
			Common.release();

			log.debug("Releasing Util threadpool...");
			Util.shutdownThreadPool();

			R.BACKEND.destroyIf();
			// Wait for the task scheduler to finish
			taskScheduler.awaitTermination(10, TimeUnit.SECONDS);
			taskScheduler.shutdownNow();
			log.info("The task scheduler reports it was " + (taskScheduler.isTerminated() ? "" : "not ") +
			         "terminated successfully.");
			log.info("StarExec successfully shutdown");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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

		log = StarLogger.getLogger(Starexec.class);

		/* uncomment to debug what is happening with logback configuration:
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		ch.qos.logback.core.util.StatusPrinter.print(context); */

		// HERE
		log.debug("\n\nHERE: Java Version: "+System.getProperty("java.version")+"\n\n");

		// Log info on the initialization stack
		/*
		log.info( "\n\nstarting Starexec.contextInitialized()\n" );
		StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		String stackString = "";
		for ( StackTraceElement element : stacktrace ) {
			stackString += element.toString()+"\n";
		}
		log.debug( "\n\ncontextInitialized() stackString:\n"+stackString+"\n" );
		
		log.debug( "\n\nR.STAREXEC_ROOT: "+R.STAREXEC_ROOT+"\nenvvar SGE_ROOT: "+System.getenv("SGE_ROOT")+"\nR.BACKEND_ROOT: "+R.BACKEND_ROOT+"\n" );
		*/

		log.info(String.format("StarExec started at [%s]", R.STAREXEC_ROOT));
		try {
			log.info("Starexec running as " + Util.executeCommand("whoami"));
		} catch (IOException e1) {
			log.error("unable to execute whoami");
		}
		// Setup the path to starexec's configuration files
		R.CONFIG_PATH = new File(R.STAREXEC_ROOT, "/WEB-INF/classes/org/starexec/config/").getAbsolutePath();
		R.RUNSOLVER_PATH = new File(R.getSolverPath(), "runsolver").getAbsolutePath();

		// Initialize the datapool after properties are loaded
		Common.initialize();

		R.logProperties();

		try {
			FileUtils.copyFile(new File(R.CONFIG_PATH, "sge/runsolver"), new File(R.RUNSOLVER_PATH));
			Util.chmodDirectory(R.RUNSOLVER_PATH, false);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		// Initialize the validator (compile regexes) after properties are loaded
		Validator.initialize();


		if (R.IS_FULL_STAREXEC_INSTANCE) {
			// log.debug( "\n\nR.BACKEND_ROOT: "+R.BACKEND_ROOT+"\n" );

			R.BACKEND.initialize(R.BACKEND_ROOT);
		}

		try {
			R.PUBLIC_USER_ID = Users.get("public").getId();
		} catch (Exception e) {
			log.fatal("!!! No public user found !!! Cannot continue !!!", e);
			throw e;
		}

		System.setProperty("http.proxyHost", R.HTTP_PROXY_HOST);
		System.setProperty("http.proxyPort", R.HTTP_PROXY_PORT);

		org.starexec.data.security.GeneralSecurity.test();

		// Schedule necessary periodic tasks to run
		this.scheduleRecurringTasks();

		// Set any application variables to be used on JSP's with EL
		event.getServletContext().setAttribute("buildVersion", R.buildVersion);
		event.getServletContext().setAttribute("buildDate", R.buildDate);
		event.getServletContext().setAttribute("buildUser", R.buildUser);
		event.getServletContext().setAttribute("contactEmail", R.CONTACT_EMAIL);

		Analytics.STAREXEC_DEPLOY.record();

		log.info( "finishing Starexec.contextInitialized()" );
	}


	/**
	 * Creates and schedules periodic tasks to be run.
	 */
	private void scheduleRecurringTasks() {
		//created directories expected by the system to exist
		Util.initializeDataDirectories();

		TestManager.initializeTests();

		// Gets all the periodic tasks and runs them.
		// If you need to create a new periodic task, add another enum instance to PeriodicTasks.PeriodicTask
		Set<PeriodicTasks.PeriodicTask> periodicTasks = EnumSet.allOf(PeriodicTasks.PeriodicTask.class);
		for (PeriodicTasks.PeriodicTask task : periodicTasks) {
			if (R.IS_FULL_STAREXEC_INSTANCE || !task.fullInstanceOnly) {
				taskScheduler.scheduleWithFixedDelay(task.task, task.delay, task.period.get(), task.unit);
			}
		}

		try {
			PaginationQueries.loadPaginationQueries();
		} catch (Exception e) {
			log.error("unable to correctly load pagination queries");
			log.error(e.getMessage(), e);
		}
	}
}
