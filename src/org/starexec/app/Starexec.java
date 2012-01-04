package org.starexec.app;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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
	private static String ROOT_APPLICATION_PATH = "";	
	
	// Path of the starexec config and log4j files which are needed at compile time to load other resources
	private static String CONFIG_PATH = "/WEB-INF/classes/org/starexec/config/starexec-config.xml";
	private static String LOG4J_PATH = "/WEB-INF/classes/org/starexec/config/log4j.properties";	
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// Called when the application ends
		log.info("Starexec Shutdown");
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
		
		// On a new thread, load the worker node data (this may take some time)
		Runnable r = new Runnable() {			
			@Override
			public void run() {
				GridEngineUtil.loadWorkerNodes();
				GridEngineUtil.loadQueues();
			}
		};		
		new Thread(r).start();		
	}	
}