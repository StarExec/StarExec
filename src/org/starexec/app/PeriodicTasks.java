package org.starexec.app;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.time.StopWatch;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.ErrorLog;
import org.starexec.data.to.Status;
import org.starexec.data.to.User;
import org.starexec.data.to.tuples.PairIdJobId;
import org.starexec.data.to.WorkerNode;
import org.starexec.data.to.Queue;
import org.starexec.exceptions.StarExecException;
import org.starexec.jobs.JobManager;
import org.starexec.jobs.ProcessingManager;
import org.starexec.logger.StarLogger;
import org.starexec.util.Mail;
import org.starexec.util.RobustRunnable;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Class that contains the periodic task enumerations.
 * The PeriodicTask enumeration contains all the periodic tasks as constant enum values so that these
 * values can be references from any location. Storing these values as an enum with fields makes sense because
 * the period, delay, task, etc. are all related to one task and storing these as separate constants would not enforce
 * this relationship. Using an enum also allows all the tasks to be gotten with EnumSet.allOf()
 */
class PeriodicTasks {

    private static final StarLogger log = StarLogger.getLogger(PeriodicTasks.class);

    // Enum constants of all the periodic tasks.
    enum PeriodicTask {
        // period needs to be a supplier since CLUSTER_UPDATE_PERIOD is dynamically set by configuration.
        UPDATE_CLUSTER(true, UPDATE_CLUSTER_TASK, 0, () -> R.CLUSTER_UPDATE_PERIOD, TimeUnit.SECONDS),
        SUBMIT_JOBS(true, SUBMIT_JOBS_TASK, 0, () -> 20, TimeUnit.SECONDS),
        POST_PROCESS_JOBS(true, POST_PROCESS_JOBS_TASK, 0, () -> 45, TimeUnit.SECONDS),
        RERUN_FAILED_PAIRS(true, RERUN_FAILED_PAIRS_TASK, 0, () -> 90, TimeUnit.MINUTES),
        FIND_BROKEN_JOB_PAIRS(true, FIND_BROKEN_JOB_PAIRS_TASK, 0, () -> 3, TimeUnit.HOURS),
        SEND_ERROR_LOGS(true, SEND_ERROR_LOGS_TASK, 0, () -> 1, TimeUnit.DAYS),
        CLEAR_TEMPORARY_FILES(false, CLEAR_TEMPORARY_FILES_TASK, 0, () -> 3, TimeUnit.HOURS),
        CLEAR_JOB_LOG(false, CLEAR_JOB_LOG_TASK, 0, () -> 7, TimeUnit.DAYS),
        CLEAR_JOB_GRAPHS(false, CLEAR_JOB_GRAPHS_TASK, 0, () -> 7, TimeUnit.DAYS),
	FIND_BROKEN_NODES(true, FIND_BROKEN_NODES_TASK, 0, () -> 6, TimeUnit.HOURS),
        CLEAR_JOB_SCRIPTS(false, CLEAR_JOB_SCRIPTS_TASK, 0, () -> 12, TimeUnit.HOURS),
        CLEAN_DATABASE(false, CLEAN_DATABASE_TASK, 0, () -> 7, TimeUnit.DAYS),
        CREATE_WEEKLY_REPORTS(false, CREATE_WEEKLY_REPORTS_TASK, 0, () -> 1, TimeUnit.DAYS),
        DELETE_OLD_ANONYMOUS_LINKS(false, DELETE_OLD_ANONYMOUS_LINKS_TASK, 0, () -> 30, TimeUnit.DAYS),
        UPDATE_USER_DISK_SIZES(false, UPDATE_USER_DISK_SIZES_TASK, 0, () -> 1, TimeUnit.DAYS),
        UPDATE_COMMUNITY_STATS(false, UPDATE_COMMUNITY_STATS_TASK, 0, () -> 6, TimeUnit.HOURS),
        SAVE_ANALYTICS(false, SAVE_ANALYTICS_TASK, 10, () -> 10, TimeUnit.MINUTES),
        NOTIFY_USERS_OF_JOBS(false, NOTIFY_USERS_OF_JOBS_TASK, 0, () -> 5, TimeUnit.MINUTES),
	GENERATE_CLUSTER_GRAPH(true, GENERATE_CLUSTER_GRAPH_TASK, 5, () -> 5, TimeUnit.SECONDS);
	//CLEAR_JOB_SCRIPTS(true, CLEAR_JOB_SCRIPTS_TASK, 0, () -> 7, TimeUnit.DAYS); 

        public final boolean fullInstanceOnly;
        public final Runnable task;
        public final int delay;
        public final Supplier<Integer> period;
        public final TimeUnit unit;

        /**
         *
         * @param fullInstanceOnly true if this task should only be run when a working backend is configured
         * @param task the runnable that will be run for the task.
         * @param delay initial delay before the task should be run.
         * @param period the period between each successive run of the task.
         * @param unit the time unit to use for period and delay.
         */
        PeriodicTask(boolean fullInstanceOnly, Runnable task, int delay, Supplier<Integer> period, TimeUnit unit) {
            this.fullInstanceOnly = fullInstanceOnly;
            this.delay = delay;
            this.period = period;
            this.unit = unit;
            this.task = task;
        }
    }


    private static final String sendErrorLogsTaskName = "sendErrorReportsTask";
    private static final Runnable SEND_ERROR_LOGS_TASK = new RobustRunnable(sendErrorLogsTaskName) {
        @Override
        protected void dorun() {
            // Calculate the timestamp from yesterday.
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -R.TIME_BETWEEN_SENDING_ERROR_LOGS);
            Timestamp yesterday = new Timestamp(calendar.getTime().getTime());

            try {
                if (ErrorLogs.existBefore(yesterday)) {
					log.info("Found error logs from "+R.TIME_BETWEEN_SENDING_ERROR_LOGS+" days ago. Sending error logs...");
                    // Make sure users we're emailing are developers and admins.
                    List<User> usersSubscribedToErrorLogs = Users.getUsersSubscribedToErrorLogs();
                    for (User user : usersSubscribedToErrorLogs) {
                        if (!GeneralSecurity.hasAdminReadPrivileges(user.getId())) {
                            log.error("Found user who wasn't developer/admin while emailing error logs: "+user.getEmail());
                        }
                    }

                    usersSubscribedToErrorLogs = usersSubscribedToErrorLogs.stream()
                            .filter(u -> GeneralSecurity.hasAdminReadPrivileges(u.getId()))
                            .collect(Collectors.toList());

                    // Gather the error logs and send them.
                    List<ErrorLog> allLogs = ErrorLogs.getAll();
                    Mail.sendErrorLogEmails(allLogs, usersSubscribedToErrorLogs);

                    // Delete all the error logs, in the future we may keep some until they get too old.
                    ErrorLogs.deleteAll();
                } else {
					log.info("No error logs from "+R.TIME_BETWEEN_SENDING_ERROR_LOGS+" days ago. Not sending error logs.");
				}
            } catch (SQLException e) {
                log.error("Failed to send error log emails.", e);
            }
        }
    };


    private static final String updateClusterTaskName = "updateClusterTask";
    // Create a task that updates the cluster usage info (this may take some time)
    private static final Runnable UPDATE_CLUSTER_TASK = new RobustRunnable(updateClusterTaskName) {
        @Override
        protected void dorun() {
            Cluster.loadWorkerNodes();
            Cluster.loadQueueDetails();
        }
    };

    private static final String generateClusterGraphTaskName = "generateClusterGraphTask";
    private static final Runnable GENERATE_CLUSTER_GRAPH_TASK = new RobustRunnable(generateClusterGraphTaskName) {
	@Override
	protected void dorun() {
	    try {

            // previous implementation for just one queue graph; commented out by Alexander Brown 11/20
//		    int num_enqueued = Util.executeCommand("qstat -u tomcat -s p").split("\r\n|\r|\n").length - 2;
//		    if(num_enqueued < 0) num_enqueued = 0; //Adjust for the top two lines being headings.
//		    Statistics.addQueuePlotPoint(num_enqueued);

            // loop through the SGE cluster queues, calling the function to create the queue graph for each one
            for ( Queue q : Queues.getAllActive() ) {
                Statistics.addQueuePlotPoint( q.getId() );
            }

	    } catch(Exception e) {
		    log.warn(e.getMessage());
	    }        
	}
    };

    private static final String submitJobTasksName = "submitJobTasks";
    // Create a task that submits jobs that have pending/rejected job pairs
    private static final Runnable SUBMIT_JOBS_TASK= new RobustRunnable(submitJobTasksName) {
        @Override
        protected void dorun() {
            JobManager.checkPendingJobs();
        }
    };

    private static final String rerunFailedPairsTaskName = "rerunFailedPairsTask";
    private static final Runnable RERUN_FAILED_PAIRS_TASK = new RobustRunnable(rerunFailedPairsTaskName) {
        @Override
        protected void dorun() {
            try {
                // Get all pairs that haven't already been rerun that have the ERROR_RUNSCRIPT status and
                // rerun them.
                StopWatch timer = new StopWatch();
                timer.start();
                List<Integer> pairIdsToRerun = JobPairs.getPairIdsByStatusNotRerunAfterDate(
                        Status.StatusCode.ERROR_RUNSCRIPT,
                        R.earliestDateToRerunFailedPairs());
                timer.stop();
                log.info("("+this.name+")"+" Got " + pairIdsToRerun.size() + " in " + timer.toString());


                for (Integer pairId : pairIdsToRerun) {
                    Jobs.rerunPair(pairId);
                    PairsRerun.markPairAsRerun(pairId);
                }
            } catch (SQLException e) {
                log.warn(this.name+" caught SQLException. Could not get pairs to rerun from database.", e);
            }
        }
    };

    private static final String postProcessJobsTaskName = "postProcessJobsTask";
    // Create a task that submits jobs that have pending/rejected job pairs
    private static final Runnable POST_PROCESS_JOBS_TASK = new RobustRunnable(postProcessJobsTaskName) {
        @Override
        protected void dorun() {
            ProcessingManager.checkProcessingPairs();
        }
    };

    // Create a task that deletes download files older than 1 day
    private static final String clearTemporaryFilesTaskName = "clearTemporaryFilesTask";
    private static final Runnable CLEAR_TEMPORARY_FILES_TASK = new RobustRunnable(clearTemporaryFilesTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR).getAbsolutePath(), 1,false);

            // clear sandbox files older than one day old
            Util.clearOldSandboxFiles(Util.getSandboxDirectory().getAbsolutePath(), 1);
        }
    };

    /*  Create a task that deletes job logs older than 7 days */
    private static final String clearJobLogTaskName = "clearJobLogTask";
    private static final Runnable CLEAR_JOB_LOG_TASK = new RobustRunnable(clearJobLogTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(R.JOB_LOG_DIRECTORY, R.CLEAR_JOB_LOG_PERIOD, true);
        }
    };
     /*  Create a task that deletes job graphs older than 7 days */
    private static final String clearJobGraphsTaskName = "clearJobGraphsTask";
    private static final Runnable CLEAR_JOB_GRAPHS_TASK = new RobustRunnable(clearJobGraphsTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(R.JOBGRAPH_FILE_DIR, 7, true);
        }
    };
    /*  Create a task that deletes job scripts older than 3 days */
    private static final String clearJobScriptTaskName = "clearJobScriptTask";
    private static final Runnable CLEAR_JOB_SCRIPTS_TASK = new RobustRunnable(clearJobScriptTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(R.getJobInboxDir(),1,false);
        }
    };
    /* Create a task that deletes job graphs older than 7 days old */
    /*private static final String clearJobGraphsTaskName = "clearJobGraphsTask";
    private static final runnable CLEAR_JOB_GRAPHS_TASK = new RobustRunnable(clearJobGraphsTaskName) {
	@Override
	protected void doRun() {
	    File f = new File(R.STAREXEC_ROOT, R.JOBGRAPH_FILE_DIR);
	    Util.clearOldFiles(f.getPath(), 7, false);	    
	}
    };*/

    /**
     * Removes solvers and benchmarks from the database that are both orphaned (unaffiliated
     * with any spaces or job pairs) AND have already been deleted on disk.
     */
    private static final String cleanDatabaseTaskName = "cleanDatabaseTask";
    private static final Runnable CLEAN_DATABASE_TASK = new RobustRunnable(cleanDatabaseTaskName) {
        @Override
        protected void dorun() {
            Solvers.cleanOrphanedDeletedSolvers();
            Benchmarks.cleanOrphanedDeletedBenchmarks();
            Jobs.cleanOrphanedDeletedJobs();
        }
    };

    private static final String findBrokenNodesTaskName = "findBrokenNodes";
    private static final Runnable FIND_BROKEN_NODES_TASK = new RobustRunnable(findBrokenNodesTaskName) {
        @Override
        protected void dorun() {
            try {
                ImmutableSet<PairIdJobId> brokenPairs = JobPairs.getPairsEnqueuedLongerThan(R.PAIR_ENQUEUE_TIME_THRESHOLD);
                for (PairIdJobId pairAndJob : brokenPairs) {
                    log.warn("Detected pair that has been enqueued for "+R.PAIR_ENQUEUE_TIME_THRESHOLD+" minutes "+
                            "without running. Pair has id "+pairAndJob.pairId+" and is in job with id "+pairAndJob.jobId);

                }
            } catch (SQLException e) {
                log.error("Database error while searching for broken pairs.");
            }
        }
    };

    private static final String findBrokenJobPairsTaskName = "findBrokenJobPairs";
    private static final Runnable FIND_BROKEN_JOB_PAIRS_TASK = new RobustRunnable(findBrokenJobPairsTaskName) {
        @Override
        protected void dorun() {
            try {
                Jobs.setBrokenPairsToErrorStatus(R.BACKEND);
            } catch (IOException e) {
                log.error("Caught IOException: " + e.getMessage(), e);
            }
        }
    };

    private static final String updateUserDiskSizesTaskName = "updateUserDiskSizesTask";
    private static final Runnable UPDATE_USER_DISK_SIZES_TASK = new RobustRunnable(updateUserDiskSizesTaskName) {
        @Override
        protected void dorun() {
            if (!Users.updateAllUserDiskSizes()) {
                log.error("failed to update user disk sizes (periodic)");
            }

        }
    };

    private static final String deleteOldAnonymousLinksTaskName = "deleteOldAnonymousLinksTask";
    private static final Runnable DELETE_OLD_ANONYMOUS_LINKS_TASK = new RobustRunnable(deleteOldAnonymousLinksTaskName) {
        @Override
        protected void dorun() {
            try {
                AnonymousLinks.deleteOldLinks(R.MAX_AGE_OF_ANONYMOUS_LINKS_IN_DAYS);
            } catch (SQLException e) {
                log.error( "Caught SQLException: Failed to delete old anonymous links.", e);
            }
        }
    };

    private static final String updateCommunityStatsTaskName = "updateCommunityStats";
    private static final Runnable UPDATE_COMMUNITY_STATS_TASK = new RobustRunnable(updateCommunityStatsTaskName) {
        @Override
        protected void dorun() {
            Communities.updateCommunityMap();
        }
    };

    private static final String weeklyReportsTaskName = "weeklyReportsTask";
    private static final Runnable CREATE_WEEKLY_REPORTS_TASK = new RobustRunnable(weeklyReportsTaskName) {
        @Override
        protected void dorun() {
            // create the reports directory in the starexec data directory if it does not already exist
            String dataDirectory = R.STAREXEC_DATA_DIR;
            File reportsDirectory = new File(dataDirectory, "/reports/");
            if (!reportsDirectory.exists()) {
                try {
                    log.debug("Attempting to create reports directory " + dataDirectory + "/reports/");
                    boolean reportsDirectoryCreated = reportsDirectory.mkdir();
                    if (!reportsDirectoryCreated) {
                        log.error("Starexec does not have permission to create the reports directory in " + dataDirectory);
                    }
                } catch (SecurityException e) {
                    log.error("Starexec does not have permission to create the reports directory in " + dataDirectory, e);
                }
            }

            List<User> subscribedUsers = Users.getAllUsersSubscribedToReports();
            try {
                if (subscribedUsers.size() > Users.getCount()) {
                    // make sure that we're not sending unnecessary emails
                    throw new StarExecException("There are more users subscribed to reports than users in the system!");
                }

                Calendar today = Calendar.getInstance();
                // check if it's the day to email reports and check if reports were already sent today
                if (today.get(Calendar.DAY_OF_WEEK) == R.EMAIL_REPORTS_DAY && !Mail.reportsEmailedToday()) {
                    String reportsEmail = Mail.generateGenericReportsEmail();
                    log.info("Storing reports and sending reports to subscribed users.");
                    Mail.storeReportsEmail(reportsEmail);
                    Mail.sendReports(subscribedUsers, reportsEmail);
                    // Set all the events occurrences in the reports table back to 0.
                    Reports.resetReports();
                    // Erase all data from the logins table.
                    Logins.resetLogins();
                }
            } catch (IOException e) {
                log.error("Issue while storing reports email as text file.", e);
            } catch (StarExecException e) {
                log.error("Caught StarExecException: "+e.getMessage(), e);
            } catch (SQLException e) {
                log.error("Caught SQLException: "+e.getMessage(), e);
            }
        }
    };

	private static final String  saveAnalyticsTask = "saveAnalyticsTask";
	// Create a task that submits jobs that have pending/rejected job pairs
	private static final Runnable SAVE_ANALYTICS_TASK = new RobustRunnable(saveAnalyticsTask) {
		@Override
		protected void dorun() {
			Analytics.saveToDB();
		}
	};

	// Create a task that notifies Users of status changes to Jobs they have
	// subscribed to
	private static final String notifyUsersOfJobsTask = "notifyUsersOfJobsTask";
	private static final Runnable NOTIFY_USERS_OF_JOBS_TASK = new RobustRunnable(notifyUsersOfJobsTask) {
		@Override
		protected void dorun() {
			Notifications.sendEmailNotifications();
		}
	};
}
