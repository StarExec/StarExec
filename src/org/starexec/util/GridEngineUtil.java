package org.starexec.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Common;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Queues;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Status.StatusCode;

/**
 * Contains methods for interacting with the sun grid engine. This class is NOT operating system independent
 * and may require environmental set up to function properly in Windows.
 * @author Tyler Jensen
 */
public class GridEngineUtil {
	private static final Logger log = Logger.getLogger(GridEngineUtil.class);

	// The regex patterns used to parse SGE output
	private static Pattern nodeKeyValPattern;
	private static Pattern queueKeyValPattern;
	private static Pattern queueAssocPattern;
	private static ExecutorService threadPool = null;

	@SuppressWarnings("unused")
	private static String testString = "queuename                      qtype resv/used/tot. load_avg arch          states\r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n001.star.cs.uiowa.edu   BIP   0/0/8          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n002.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n003.star.cs.uiowa.edu   BIP   0/0/8          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"all.q@n004.star.cs.uiowa.edu   BIP   0/0/8          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n001.q@n001.star.cs.uiowa.edu  BIP   0/0/1          0.05     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n002.q@n002.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n003.q@n003.star.cs.uiowa.edu  BIP   0/0/1          0.07     lx24-amd64    \r\n" + 
	"---------------------------------------------------------------------------------\r\n" + 
	"n004.q@n004.star.cs.uiowa.edu  BIP   0/0/1          0.02     lx24-amd64    ";

	static {
		// Compile the SGE output parsing patterns when this class is loaded
		nodeKeyValPattern = Pattern.compile(R.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueKeyValPattern = Pattern.compile(R.QUEUE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
		queueAssocPattern = Pattern.compile(R.QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);


		threadPool = Executors.newCachedThreadPool();		
	}

	/**
	 * Shuts down the reserved threadpool this util uses.
	 */
	public static void shutdown() throws Exception {
		threadPool.shutdown();
		threadPool.awaitTermination(10, TimeUnit.SECONDS);
	}

	public static Session createSession() {
		try {
			log.debug("createSession() loading class."); 	
			// Try to load the class, if it does not exist this will cause an exception instead of an error			
			Class.forName("com.sun.grid.drmaa.SessionImpl");
		} catch (Exception e) {
			log.error("Error loading com.sun.grid.drmaa.SessionImpl");
		}

		Session s = SessionFactory.getFactory().getSession();
		try {
			s.init("");
		} catch (Exception e) {
			log.error("Error initializing the SGE session.");
		}
		return s;
	}

	public static void destroySession(Session s) {
		try {
			s.exit();
		}
		catch (Exception e) {
			log.error("Problem destroying session: "+e,e);
		}
	}

	/**
	 * Gets the queue list from SGE and adds them to the database if they don't already exist. This must
	 * be done AFTER nodes are loaded as the queues will make associations to the nodes. This also loads
	 * attributes for the queue as well as its current usage.
	 */
	public static synchronized void loadQueues() {
		GridEngineUtil.loadQueueDetails();
		//		GridEngineUtil.loadQueueUsage();
	}

	/**
	 * Loads the list of active queues on the system, loads their attributes into the database
	 * as well as their associations to worker nodes that belong to each queue.
	 */
	private static void loadQueueDetails() {
		BufferedReader queueResults = null;

		try {			
			// Execute the SGE command to get the list of queues
			queueResults = Util.executeCommand(R.QUEUE_LIST_COMMAND);

			if(queueResults == null) {
				// If the command failed, return now	
				return;
			}

			// Set all queues as inactive (we will set them as active when we see them)
			Queues.setStatus(R.QUEUE_STATUS_INACTIVE);

			// Read the queue names one at a time
			String line;		
			while((line = queueResults.readLine()) != null) {
				String name = line;

				// In the database, update the attributes for the queue
				Queues.update(name,  GridEngineUtil.getQueueDetails(name));

				// Set the queue as active since we just saw it
				Queues.setStatus(name, R.QUEUE_STATUS_ACTIVE);
			}
			/*		// For each of the queue's node's, add an association
	      for(WorkerNode node : GridEngineUtil.getQueueAssociations(name)) {
	      log.debug("[loadQueueDetails] Associating queue (" + name + 
	      ") with node ("
	      + node.getName());
	      Queues.associate(name, node.getName());	
	      }				
	      }
			 */  
			//Adds all the associations to the db
			GridEngineUtil.setQueueAssociationsInDb();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { queueResults.close(); } catch (Exception e) { }
		}
	}

	/**
	 * Grabs queue usage information from SGE and dumps it in the database. Usage is in terms of
	 * job slots available, used, reserved and total.
	 */
	public static void loadQueueUsage() { 
		try {
			// Call SGE to get details about the usage of all queues
			BufferedReader buff = Util.executeCommand(R.QUEUE_USAGE_COMMAND);

			// Read line twice to skip past the header info returned
			buff.readLine();
			buff.readLine();

			String line = "";
			// For each queue output...
			while((line = buff.readLine()) != null) {
				// The output is separated by white spaces, split on whitespace to get the data in array form
				String[] data = line.split("\\s+");

				// Create a new queue and pick out the data we want from the output (0 = name, 2-5 = usage stats)
				Queue q = new Queue();
				q.setName(data[0]);
				q.setSlotsUsed(Integer.parseInt(data[2]));
				q.setSlotsReserved(Integer.parseInt(data[3]));
				q.setSlotsAvailable(Integer.parseInt(data[4]));
				q.setSlotsTotal(Integer.parseInt(data[5]));

				// Update the database with the new usage stats
				Queues.updateUsage(q);
			}	
			buff.close();
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	/**
	 * Extracts queue-node association from SGE and puts it into the db.
	 * @return true if successful
	 * @author Benton McCune
	 */
	public static Boolean setQueueAssociationsInDb() {

		// Call SGE to get info on the queues
		//String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));

		String[] envp = new String[2];
		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
		envp[1] = "SGE_ROOT="+R.SGE_ROOT; // it seems we need to set this explicitly if we change the environment.
		BufferedReader reader = Util.executeCommand(R.QUEUE_STATS_COMMAND,envp);
		String results = Util.bufferToString(reader);
		//String results = testString;
		log.info("Updating the DB with associations between SGE queues to compute nodes.");

		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("set Queue Associations says " + e.getMessage(), e);
		}

		// Parse the output from the SGE call to get the child worker nodes
		java.util.regex.Matcher matcher = queueAssocPattern.matcher(results);

		String[] capture;  // string array to store a queue and its associated node
		//Remove all association info from db so stale data isn't displayed
		Queues.clearQueueAssociations();
		// For each match...
		while(matcher.find()) {
			// Parse out the queue and node names from the regex parser and add it to the return list			
			capture = matcher.group().split("@");
			log.debug("queue = " + capture[0]);
			log.debug("node = " + capture[1]);
			Queues.associate(capture[0], capture[1]);
		}

		return true;
	}

	/**
	 * Calls SGE to get details about the given queue. 
	 * @param name The name of the queue to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getQueueDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();

		// Call SGE to get details for the given node
		//String results = Util.bufferToString(Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name));
		BufferedReader reader = Util.executeCommand(R.QUEUE_DETAILS_COMMAND + name);
		String results = Util.bufferToString(reader);
		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("get Queue Details says " + e.getMessage(), e);
		}
		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = queueKeyValPattern.matcher(results);

		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("\\s+");

			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}

		return details;
	}	

	/**
	 * Gets the worker nodes from SGE and adds them to the database if they don't already exist. This must be done
	 * BEFORE queues have been loaded as the queues will make associations to the nodes.
	 */
	public static synchronized void loadWorkerNodes() {
		BufferedReader nodeResults = null;

		try {			
			// Execute the SGE command to get the node list
			nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);

			if(nodeResults == null) {
				// If the command failed, return now				
				return;
			}

			// Set all nodes as inactive (we will update them to active as we see them)
			Cluster.setNodeStatus(R.NODE_STATUS_INACTIVE);

			// Read the nodes one at a time
			String line;		
			while((line = nodeResults.readLine()) != null) {
				String name = line;							
				// In the database, update the attributes for the node
				Cluster.updateNode(name,  GridEngineUtil.getNodeDetails(name));				
				// Set the node as active (because we just saw it!)
				Cluster.setNodeStatus(name, R.NODE_STATUS_ACTIVE);
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { nodeResults.close(); } catch (Exception e) { }
		}
	}

	/**
	 * Calls SGE to get details about the given node. 
	 * @param name The name of the node to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getNodeDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();

		// Call SGE to get details for the given node
		BufferedReader reader = Util.executeCommand(R.NODE_DETAILS_COMMAND + name);
		String results = Util.bufferToString(reader);
		try {
			reader.close();
		}
		catch (Exception e) {
			log.warn("get Node Details says " + e.getMessage(), e);
		}

		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = nodeKeyValPattern.matcher(results);

		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("=");

			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}

		return details;
	}	

	/**
	 * Finds all starexec jobs that are waiting to have their statistics processed
	 * and pulls the statistics from the grid engine into the database
	 */
	public static synchronized void processResults() {
		try {
			// First get the SGE ids of all the jobs that need their statistics processed
			final List<Integer> idsToProcess = Jobs.getSgeIdsByStatus(StatusCode.STATUS_WAIT_RESULTS.getVal());					
			//int numLeft = idsToProcess.size();
			// For each id to process...
			if (idsToProcess.size()>0){
				log.info(idsToProcess.size() + " jobs waiting to have stats processed");

			}

			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					for(int id : idsToProcess) {	
						int numLeft = idsToProcess.size();
						if (Common.getDataPoolData()){
							final int safeId = id;
							log.debug("Processing job pair " + safeId);

							// Execute the processing for this id on a thread from the pool
							//threadPool.execute(new Runnable() {					
							//	@Override
							//	public void run() {
							log.info("Processing pair " + safeId + " on thread " + Thread.currentThread().getName());
							//Jobs.setSGEPairStatus(safeId, StatusCode.STATUS_PROCESSING_RESULTS.getVal());
							// Process statistics and attributes
							boolean success = GridEngineUtil.processStatistics(safeId);
							//boolean success = true;
							log.info("Statistic processing success for " + safeId + " = " + success);

							success = success && GridEngineUtil.processAttributes(safeId);	
							log.info("Statistic AND Attribute processing success for " + safeId + " = " + success);
							JobPairs.setSGEPairStatus(safeId, (success) ? StatusCode.STATUS_COMPLETE.getVal() : StatusCode.ERROR_RESULTS.getVal());

							log.info("Processing complete for pair " + safeId + " on thread " + Thread.currentThread().getName());
							//	}
							//});
							numLeft--;
						}
						else{
							log.warn("Too many active connections - postponing job result processing for " + numLeft + " remaining job pairs");
							break;
						}
					}
				}});	
			if(idsToProcess != null && idsToProcess.size() > 0) {
				log.debug(String.format("Scheduled results processing for %d job pairs", idsToProcess.size()));
			}

		} catch (Exception e){
			log.error("processResults() says " + e.getMessage(), e);
		}
	}

	/**
	 * Pulls the statistics from the grid engine and places them into the database
	 * @param sgeId The sge id of the pair to process statistics for
	 * @return True if the operation was a success, false otherwise
	 */
	private static boolean processStatistics(int sgeId) {
		try {
			// Get the job's statistics
			String[] jobStats = GridEngineUtil.getSgeJobStats(sgeId);
			// Build a job pair based on the statistics
			log.info("Returned from getting SgeJobStats for sgeId = " + sgeId+".  Now writing stats to pair");
			JobPair pair = GridEngineUtil.rawStatsToPair(sgeId, jobStats);

			// Update the database with the pair
			log.info("About to add pair to db for sgeId = " + sgeId);
			JobPairs.updatePairStatistics(pair);
			log.info("Stats written to db for sgeId = " + sgeId);
			return true;			
		} catch (Exception e) {
			log.error("processStatistics says " + e.getMessage(), e);			
		}

		return false;
	}

	/**
	 * Runs the post processor on the output of the job pair and stores the
	 * resulting attributes in the database
	 * @param sgeId The sge id of the pair to process attributes for
     * @return True if the operation was a success, false otherwise
     */
    private static boolean processAttributes(int sgeId) {
    	log.info("processing attributes for " + sgeId);
    	BufferedReader reader = null;		
    	JobPair pair = JobPairs.getSGEPairDetailed(sgeId);
    	Job job = Jobs.getDetailed(pair.getJobId());
    	log.info("successfully retrieved pair id " + pair.getId()  + " and job id " + job.getId() + " from sgeId " + sgeId);
    	try {
    		log.info("getting post processor for job " + job.getId() +", sgeId = " +sgeId);
    		Processor processor = job.getPostProcessor();

    		if(processor != null) {
    			log.info("got post processor " + processor.getId() + " for job " + job.getId() +", sgeId = " +sgeId);
    			File stdOut = GridEngineUtil.getStdOutFile(job.getUserId(), job.getId(), pair.getSolver().getName(), pair.getConfiguration().getName(), pair.getBench().getName());
    			log.info("about to run processor "+ processor.getId() + " on stdOut file for job " + job.getId() +", sgeId = " +sgeId);
    			// Run the processor on the std out file
    			String command = processor.getFilePath() + stdOut.getAbsolutePath();

    			log.info("Command to execute = " + command);
    			reader = Util.executeCommand(command);			  
    			log.info("executed command on stdOut file with processor" + processor.getId() + " for job " + job.getId() +", sgeId = " +sgeId + ". Reader is null = " + (reader==null) + ". Reader is ready = " + (reader.ready()));
    			// Load results into a properties file
    			Properties prop = new Properties();
    			prop.load(reader);							
    			log.info("loaded properties for job " + job.getId() +", sgeId = " +sgeId + ".  About to add job attributes for pair " + pair.getId());
    			// Attach the attributes to the benchmark
    			Jobs.addJobAttributes(pair.getId(), prop);
    			log.info("Job " + job.getId() +", sgeId = " +sgeId + ".  added job attributes for pair " + pair.getId());

    		}
    		else
    		{
    			log.info("post processor for job " + job.getId() +", sgeId = " +sgeId + " was returned null");
    		}
    		log.info("returning true for processing attributes for sgeId " + sgeId);
    		return true;
    	} catch (Exception e) {
    		log.error("processAttributes says " + e.getMessage(), e);
    	} finally {
    		if(reader != null) {
    			try { reader.close(); log.info("Reader closed for sgeId " + sgeId);} catch(Exception e) {log.error("processAttributes failed at closing reader: " + e.getMessage(), e);}
    		}
    	}
    	log.warn("returning false for processing attributes for sgeId " + sgeId);
    	return false;
    }

    /**
     * Populates a job pair object based on the raw statistics from the grid engine
     * @param sgeId The sge id of the pair to convert
     * @param stats The raw statistics retrieved from the grid engine
     * @return A job pair with fully populated statistics based on the given statistics
     */
    private static JobPair rawStatsToPair(int sgeId, String[] stats) {
    	JobPair jp = new JobPair();
    	jp.setGridEngineId(sgeId);
    	jp.getNode().setName(stats[1]);
    	jp.setQueueSubmitTime(toTimestamp(stats[8]));
    	jp.setStartTime(toTimestamp(stats[9]));
    	jp.setEndTime(toTimestamp(stats[10]));
    	jp.setExitStatus(Integer.parseInt(stats[12]));
    	jp.setCpuUsage(Double.parseDouble(stats[36]));
    	jp.setUserTime(Double.parseDouble(stats[14]));
    	jp.setSystemTime(Double.parseDouble(stats[15]));
    	jp.setIoDataUsage(Double.parseDouble(stats[38]));
    	jp.setIoDataWait(Double.parseDouble(stats[40]));
    	jp.setMemoryUsage(Double.parseDouble(stats[37]));
    	jp.setMaxVirtualMemory(Double.parseDouble(stats[42]));
    	jp.setMaxResidenceSetSize(Double.parseDouble(stats[16]));
    	jp.setPageReclaims(Double.parseDouble(stats[21]));
    	jp.setPageFaults(Double.parseDouble(stats[22]));
    	jp.setBlockInput(Double.parseDouble(stats[24]));
    	jp.setBlockOutput(Double.parseDouble(stats[25]));
    	jp.setVoluntaryContextSwitches(Double.parseDouble(stats[29]));
    	jp.setInvoluntaryContextSwitches(Double.parseDouble(stats[30]));
    	log.info("Stats written to job pair for sgeId " + sgeId);
    	return jp;		
    }

    /**
     * Converts a string which is the number of seconds since Jan. 1st 1970 to a mysql timestamp
     * @param epochTime The string (which is parsed to a long) that is the unix epoch
     * @return The sql timestamp representing by the epoch
     */
    private static Timestamp toTimestamp(String epochTime) {
    	/* 
    	 * Unix time is seconds since Jan 1st 1970, but SQL's timestamp expects 
    	 * milliseconds since that time, so we multiply by 1000 to get milliseconds
    	 */
    	long unixTime = Long.parseLong(epochTime);
    	Timestamp t = new Timestamp(unixTime * 1000);			
    	return t;
    }

    /**
     * Retrieves an array of SGE job statistics
     * @param sgeId The id of the job to get statistics for
     * @return An array of strings representing the statistics for a job (see wiki for details)
     */
    private static String[] getSgeJobStats(int sgeId) throws Exception {
    	DataInputStream dis = null;
    	FileInputStream fis = null;
    	BufferedReader br = null;
    	try {
    		// Compile the pattern that is tailored for the job we're looking for		
    		Pattern statsPattern = Pattern.compile(String.format(R.STATS_ENTRY_PATTERN, sgeId), Pattern.CASE_INSENSITIVE);
    		int hackCount = 60;
    		while (hackCount > 0) {
    			// Open a buffered reader for the sge accounting file to read line by line
    			fis = new FileInputStream(R.SGE_ACCOUNTING_FILE);
    			dis = new DataInputStream(fis);
    			br = new BufferedReader(new InputStreamReader(dis));
    			log.debug("Starting search for " + sgeId + ". Attempt # " + (61 - hackCount));
    			//log.info("SGE ACCOUNTING FILE is at " + R.SGE_ACCOUNTING_FILE);
    			// For each line in the sge accounting file 
    			String line = null;
    			int lineNumber = 0;
    			while ((line = br.readLine()) != null)   {	
    				// If this is the stats entry we're looking for...
    				lineNumber++;
    				log.debug("Continuing search for " + sgeId + ". Attempt # " + (61 - hackCount) +". line is really ===" + line + "===");
    				if(statsPattern.matcher(line).matches()) {
    					// Split it by colons (the delimiter sge uses) and return it
    					log.info("Pattern found on line " + lineNumber + " for " + sgeId + " on attempt # " + (61 - hackCount));
    					return line.split(":");
    				}
    			}

    			dis.close();
    			fis.close();
    			br.close();	
    			hackCount--;
    			//This method can be called before SGE writes to the accounting file
    			if (hackCount > 0)
    			{
    				try{
    					Thread.sleep(1000);
    				}
    				catch(Exception e){
    					log.error("getSgeJobStats on sgeId " + sgeId + " says ");
    				}
    			}
    		}
    	} catch (Exception e) {
    		log.error("getSgeJobStats says " + e.getMessage(), e);
    	} finally {
    		// Close the accounting file
    		dis.close();
    		fis.close();
    		br.close();		
    	}		
    	throw new Exception("Job statistics for sge job #" + sgeId + " could not be found");
    }

    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param job The job the pair is apart of
     * @param pair The pair to get output for
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(Job job, JobPair pair, int limit) {
    	pair = JobPairs.getPairDetailed(pair.getId());
    	return GridEngineUtil.getStdOut(job.getUserId(), job.getId(), pair.getSolver().getName(), pair.getConfiguration().getName(), pair.getBench().getName(), limit);
    }

    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param userId The id of the user that submitted the job
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @param limit The maximum number of lines to return
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(int userId, int jobId, String solver_name, String config_name, String bench_name, int limit) {		
    	File stdoutFile = GridEngineUtil.getStdOutFile(userId, jobId, solver_name, config_name, bench_name);		
    	return Util.readFileLimited(stdoutFile, limit);
    }

    /**
     * Finds the standard output of a job pair and returns its file.
     * @param userId The id of the user that submitted the job
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @return All console output from a job pair run for the given pair
     */
    public static File getStdOutFile(int userId, int jobId, String solver_name, String config_name, String bench_name) {			
    	String stdoutPath = String.format("%s/%d/%d/%s___%s/%s", R.JOB_OUTPUT_DIR, userId, jobId, solver_name, config_name, bench_name);
    	log.info("The stdoutPath is: " + stdoutPath);

    	return (new File(stdoutPath));	
    }

    /**
     * Returns the log of a job pair by reading
     * in the physical log file into a string.
     * @param pair The pair to get the log for (must have a valid id and sge id)
     * @return The log of the job run
     */
    public static String getJobLog(JobPair pair) {
    	return GridEngineUtil.getJobLog(pair.getId(), pair.getGridEngineId());
    }

    /**
     * Returns the log of a job pair by reading
     * in the physical log file into a string.
     * @param pairId The id of the pair to get the log for
     * @param sgeId The SGE id of the pair
     * @return The log of the job run
     */
    public static String getJobLog(int pairId, int sgeId) {
    	try {
    		// Find the path to the job log. It's in the job log directory
    		// in the format job_1.bash.o2 where 1 is the pair id and 2 is the sge id
    		String logPath = String.format("%s/job_%d.bash.o%d", R.JOB_LOG_DIR, pairId, sgeId);			
    		log.debug("getJobLog(): checking existence of log file "+logPath);
    		File logFile = new File(logPath);

    		if(logFile.exists()) {
    			return FileUtils.readFileToString(logFile);
    		}
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}

    	return null;
    }
}