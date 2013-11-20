package org.starexec.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ggf.drmaa.Session;
import org.jfree.util.Log;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Common;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Job;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;
import org.starexec.jobs.JobManager;
import org.starexec.util.ConfigUtil;
import org.starexec.util.GridEngineUtil;
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
    private static final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(5);	
    private Session session; // GridEngine session
	
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
			
			GridEngineUtil.destroySession(session);

			// Wait for the task scheduler to finish
			taskScheduler.awaitTermination(10, TimeUnit.SECONDS);
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
		
		// Create a task that updates statistics of jobs that are finished
		final Runnable processJobStatsTask = new RobustRunnable("processJobStats") {			
			@Override
			protected void dorun() {
			    log.info("processJobStats (periodic)");
			    GridEngineUtil.processResults();
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

		// Create a task that deletes download files older than 1 day
		final Runnable clearDownloadsTask = new RobustRunnable("clearDownloadsTask") {			
			@Override
			protected void dorun() {
			    log.info("clearDownloadsTask (periodic)");
				Util.clearOldFiles(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR).getAbsolutePath(), 1);
				Util.clearOldCachedFiles(14);
				//even though we're clearing unused cache files, they still might build up for a variety
				//of reasons. To stay robust, we should probably still clear out very old ones
				Util.clearOldFiles(new File(R.STAREXEC_ROOT,R.CACHED_FILE_DIR).getAbsolutePath(), 60);
			}
		};	
		
		/*  Create a task that deletes job logs older than 3 days */
		final Runnable clearJobLogTask = new RobustRunnable("clearJobLogTask") {			
			@Override
			protected void dorun() {
			    log.info("clearJobLogTask (periodic)");
				Util.clearOldFiles(R.JOB_LOG_DIR, 3);
				Util.clearOldFiles(R.JOB_INBOX_DIR,3);
				Util.clearOldFiles(R.JOBPAIR_INPUT_DIR, 1);
			}
		};
		/**
		 * Removes solvers and benchmarks from the database that are both orphaned (unaffiliated
		 * with any spaces or job pairs) AND have already been deleted on disk.
		 */
		final Runnable cleanDatabaseTask = new RobustRunnable("cleanDatabaseTask") {
			@Override
			protected void dorun() {
				log.info("cleanDatabaseTask (periodic");
				Solvers.cleanOrphanedDeletedSolvers();
				Benchmarks.cleanOrphanedDeletedBenchmarks();
				Jobs.cleanOrphanedDeletedJobs();
			}
		};
		
		final Runnable checkQueueReservations = new RobustRunnable("checkQueueReservations") {
			@Override
			protected void dorun() {
				log.info("checkQueueReservationsTask (periodic)");
				//java.util.Date today = new java.util.Date();
				java.util.Date today = new java.util.Date(113, 10, 21); // November 14, 2013
				List<QueueRequest> queueReservations = Requests.getAllQueueReservations();
				
				for (QueueRequest req : queueReservations) {
					SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
					
					/**
					 * If today is when the reservation is ending
					 */
					boolean end_is_today = fmt.format(req.getEndDate()).equals(fmt.format(today));
					log.debug("today = " + fmt.format(today));
					log.debug("end = " + fmt.format(req.getEndDate()));
					log.debug("end_is_today = " + end_is_today);
					if (end_is_today) {
						int queueId = Queues.getIdByName(req.getQueueName());
						
						//Pause jobs that are running on the queue
						List<Job> jobs = Cluster.getJobsRunningOnQueue(queueId);
						for (Job j : jobs) {
							Jobs.pause(j.getId());
						}
						
						//TODO: Send Email on either completion or all paused [COMPLETE]
						try {
							Mail.sendReservationEnding(req);
						} catch (IOException e) {
							e.printStackTrace();
						}

						//Move associated Nodes back to default queue
						List<WorkerNode> nodes = Queues.getNodes(queueId);
						for (WorkerNode n : nodes) {
							// TODO: SGE command to move node from queue back to all.q [COMPLETE]
							String[] envp = new String[5];
							envp[0] = "-aattr";
							envp[1] = "hostgroup";
							envp[2] = "hostlist";
							envp[3] = n.getName() + "cs.uiowa.edu";
							envp[4] = "@allhosts";

							Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf ", envp);
							Queues.associate(1, n.getId());
						}
						
						
						// TODO: delete queue and add its info into the historic_queue table [COMPLETE]
						/***** DELETE THE QUEUE *****/		
							//Database modification:
							Requests.DeleteReservation(req);
							
							//Delete the host group:
							String [] envp = new String[2];
							envp[0] = "-dhgrp";
							envp[1] = "@" + req.getQueueName() + "hosts";
							Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf", envp);

							//DISABLE the queue: 
							envp[0] = "-d";
							envp[1] = req.getQueueName();
							Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qmod", envp);
							//DELETE the queue:
							envp[0] = "-dq";
							envp[1] = req.getQueueName();
							Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf", envp);

					}
					
					/**
					 * if today is when the reservation is starting
					 */
					boolean start_is_today = (fmt.format(req.getStartDate())).equals(fmt.format(today));

					if (start_is_today) {
						
						String queueName = req.getQueueName();
						int queueId = Queues.getIdByName(queueName);
						Queue q = Queues.get(queueId);
						if (!q.getStatus().equals("ACTIVE")) {
							
							List<WorkerNode> transferNodes = new ArrayList<WorkerNode>();
							
							List<WorkerNode> nodes = Queues.getNodes(1);
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < req.getNodeCount(); i++) {
								transferNodes.add(nodes.get(i));
								sb.append(nodes.get(i).getName());
								sb.append(" ");
							}
							String hostList = sb.toString();
							
							// TODO: Create a queue
							/***** CREATE A QUEUE *****/
								// Create newHost.hgrp [COMPLETE]
								String newHost;
								try {
									newHost = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/sge/newHost.txt"));
									newHost = newHost.replace("$$GROUPNAME$$", "@" + req.getQueueName() + "hosts");
									newHost = newHost.replace("$$HOSTLIST$$", hostList);
									FileUtils.writeStringToFile(new File("/tmp/newHost.hgrp"), newHost);
								} catch (IOException e) {
									e.printStackTrace();
								}

								//Add the host [COMPLETE]
								String[] envp = new String[2];
								envp[0] = "-Ahgrp";
								envp[1] = "newHost.hgrp";
								Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Ahgrp newHost.hgrp");
								//Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf" , envp);
							
								// Create newQueue.q [COMPLETE]
									String newQueue;
									try {
										newQueue = FileUtils.readFileToString(new File(R.CONFIG_PATH, "/sge/newQueue.txt"));
										newQueue = newQueue.replace("$$QUEUENAME$$", req.getQueueName());
										newQueue = newQueue.replace("$$HOSTLIST$$", "@" + req.getQueueName() + "hosts");
										//newQueue = newQueue.replace("$$SLOTS$$", "");

										FileUtils.writeStringToFile(new File("/tmp/newQueue.q"), newQueue);
									} catch (IOException e) {
										e.printStackTrace();
									}
									envp[0] = "-Aq";
									envp[1] = "newQueue.q";
									Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf -Aq newQueue.q");
									//Util.executeCommand("sudo -u sgeadmin /export/cluster/sge-6.2u5/bin/lx24-amd64/qconf", envp);							
														
							
							//Make status "ACTIVE"
							/***** ACTIVATE A QUEUE *****/
								Queues.setStatus(req.getQueueName(), "ACTIVE"); 

							
							
							
							//List<WorkerNode> nodes = Queues.getNodes(1);
							//Move Nodes that are associated with default queue to the new queue
							//for (int i = 0; i < req.getNodeCount(); i++) {
								/***** Move Node from all.q to new queue *****/
									// TODO: SGE command to move node
								//	Queues.associate(queueId, nodes.get(i).getId());
							//}
							
							// TODO: Get all the job pairs running on the nodes, and reset them


							
						}
					}

				}
			}
		};
		
		//created directories expected by the system to exist
		File downloadDir=new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR);
		downloadDir.mkdirs();
		File cacheDir=new File(R.STAREXEC_ROOT,R.CACHED_FILE_DIR);
		cacheDir.mkdirs();
		File graphDir=new File(R.STAREXEC_ROOT,R.JOBGRAPH_FILE_DIR);
		graphDir.mkdirs();

		//Schedule the recurring tasks above to be run every so often
		if (R.RUN_PERIODIC_SGE_TASKS) {
		    taskScheduler.scheduleAtFixedRate(updateClusterTask, 0, R.CLUSTER_UPDATE_PERIOD, TimeUnit.SECONDS);	
		    taskScheduler.scheduleAtFixedRate(processJobStatsTask, 0, R.SGE_STATISTICS_PERIOD, TimeUnit.SECONDS);
		    taskScheduler.scheduleAtFixedRate(submitJobsTask, 0, R.JOB_SUBMISSION_PERIOD, TimeUnit.SECONDS);
		    taskScheduler.scheduleAtFixedRate(clearDownloadsTask, 0, 1, TimeUnit.HOURS);
		    taskScheduler.scheduleAtFixedRate(clearJobLogTask, 0, 72, TimeUnit.HOURS);
		    taskScheduler.scheduleAtFixedRate(cleanDatabaseTask, 0, 7, TimeUnit.DAYS);
		    taskScheduler.scheduleAtFixedRate(checkQueueReservations, 0, 30, TimeUnit.SECONDS);

		}	
	}
}