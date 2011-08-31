package com.starexec.app;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;
import com.starexec.constants.*;
import com.starexec.data.*;


public class AppListener implements ServletContextListener {
	
	private static final Logger log = Logger.getLogger(AppListener.class);
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		
	}

	/**
	 * When the application starts, this method is called. Perform any initializations here
	 */
	@SuppressWarnings("unused")
	@Override
	public void contextInitialized(ServletContextEvent event) {		
		log.info("Application started");		
		R.loadStarexecResources();
		
		// Get the next database to force database connection pool initialization
		Database d = Databases.next();
	}
	
	
}
