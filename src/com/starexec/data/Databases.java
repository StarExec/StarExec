package com.starexec.data;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.starexec.constants.R;

/**
 * Manages a pool of database objects that the application can use to interface with the database
 * 
 * @author Tyler Jensen
 */
public class Databases {
	private static final Logger log = Logger.getLogger(Databases.class);
	private static ConcurrentLinkedQueue<Database> dataPool = null;	
	
	public Databases() throws Exception{
		throw new Exception("Cannot instantiate class because it is static.");
	}
	
	static {
		log.info("Initializing database connection pool");
		
		// Create a new concurrent queue
		dataPool = new ConcurrentLinkedQueue<Database>();
		
		// Get the start time so we can log performance
		long start = System.currentTimeMillis();
		
		for(int i = 0; i < R.CONNECTION_POOL_SIZE; i++){
			// Initialize the number of connections specified by the pool size property
			dataPool.add(new Database());
		}
		
		// Get the end time to log performance
		long end = System.currentTimeMillis();
		
		log.info(String.format("Database connection pool set up in [%d ms] with [%d] connections.", end - start, dataPool.size()));		
	}	
	
	/**
	 * Gets the next available database connection in the queue
	 */
	public static Database next(){
		// TODO: Can get into some nasty conditions here (we could try to remove from an empty thread) but we want to keep this method quick
		// The quick fix would be to synchronize the entire method but we want to avoid that since it will be high-traffic!

		// Get the next database in the queue and remove it (this is thread safe)
		Database nextDatabase = dataPool.poll();
		
		// Add the same database back to the queue to be used later (this is thread safe)
		dataPool.add(nextDatabase);
		
		// Return the database that was at the front
		return nextDatabase;
	}
}