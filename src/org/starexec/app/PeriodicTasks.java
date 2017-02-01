package org.starexec.app;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.Status;
import org.starexec.data.to.User;
import org.starexec.exceptions.StarExecException;
import org.starexec.jobs.JobManager;
import org.starexec.jobs.ProcessingManager;
import org.starexec.util.Mail;
import org.starexec.util.RobustRunnable;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

/**
 * Created by agieg on 2/1/2017.
 */
public class PeriodicTasks {

    private static Logger log = Logger.getLogger(PeriodicTasks.class);

    private static final String updateClusterTaskName = "updateClusterTask";
    // Create a task that updates the cluster usage info (this may take some time)
    public static final Runnable UPDATE_CLUSTER = new RobustRunnable(updateClusterTaskName) {
        @Override
        protected void dorun() {
            Cluster.loadWorkerNodes();
            Cluster.loadQueueDetails();
        }
    };

    private static final String submitJobTasksName = "submitJobTasks";
    // Create a task that submits jobs that have pending/rejected job pairs
    public static final Runnable SUBMIT_JOBS= new RobustRunnable(submitJobTasksName) {
        @Override
        protected void dorun() {
            JobManager.checkPendingJobs();
        }
    };

    private static final String rerunFailedPairsTaskName = "rerunFailedPairsTask";
    public static final Runnable RERUN_FAILED_PAIRS = new RobustRunnable(rerunFailedPairsTaskName) {
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
    public static final Runnable POST_PROCESS_JOBS = new RobustRunnable(postProcessJobsTaskName) {
        @Override
        protected void dorun() {
            ProcessingManager.checkProcessingPairs();
        }
    };

    // Create a task that deletes download files older than 1 day
    private static final String clearTemporaryFilesTaskName = "clearTemporaryFilesTask";
    public static final Runnable CLEAR_TEMPORARY_FILES = new RobustRunnable(clearTemporaryFilesTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR).getAbsolutePath(), 1,false);

            // clear sandbox files older than one day old
            Util.clearOldSandboxFiles(Util.getSandboxDirectory().getAbsolutePath(), 1);
        }
    };

    /*  Create a task that deletes job logs older than 7 days */
    private static final String clearJobLogTaskName = "clearJobLogTask";
    public static final Runnable CLEAR_JOB_LOG = new RobustRunnable(clearJobLogTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(R.getJobLogDir(), R.CLEAR_JOB_LOG_PERIOD,true);
        }
    };
    /*  Create a task that deletes job scripts older than 3 days */
    private static final String clearJobScriptTaskName = "clearJobScriptTask";
    public static final Runnable CLEAR_JOB_SCRIPTS = new RobustRunnable(clearJobScriptTaskName) {
        @Override
        protected void dorun() {
            Util.clearOldFiles(R.getJobInboxDir(),1,false);
        }
    };
    /**
     * Removes solvers and benchmarks from the database that are both orphaned (unaffiliated
     * with any spaces or job pairs) AND have already been deleted on disk.
     */
    private static final String cleanDatabaseTaskName = "cleanDatabaseTask";
    public static final Runnable CLEAN_DATABASE = new RobustRunnable(cleanDatabaseTaskName) {
        @Override
        protected void dorun() {
            Solvers.cleanOrphanedDeletedSolvers();
            Benchmarks.cleanOrphanedDeletedBenchmarks();
            Jobs.cleanOrphanedDeletedJobs();
        }
    };

    private static final String findBrokenNodesTaskName = "findBrokenNodes";
    public static final Runnable FIND_BROKEN_NODES = new RobustRunnable(findBrokenNodesTaskName) {
        @Override
        protected void dorun() {
        }
    };

    private static final String findBrokenJobPairsTaskName = "findBrokenJobPairs";
    public static final Runnable FIND_BROKEN_JOB_PAIRS = new RobustRunnable(findBrokenJobPairsTaskName) {
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
    public static final Runnable UPDATE_USER_DISK_SIZES = new RobustRunnable(updateUserDiskSizesTaskName) {
        @Override
        protected void dorun() {
            if (!Users.updateAllUserDiskSizes()) {
                log.error("failed to update user disk sizes (periodic)");
            }

        }
    };

    private static final String deleteOldAnonymousLinksTaskName = "deleteOldAnonymousLinksTask";
    public static final Runnable DELETE_OLD_ANONYMOUS_LINKS = new RobustRunnable(deleteOldAnonymousLinksTaskName) {
        @Override
        protected void dorun() {
            try {
                AnonymousLinks.deleteOldLinks(R.MAX_AGE_OF_ANONYMOUS_LINKS_IN_DAYS);
            } catch (SQLException e) {
                log.error( "Caught SQLExcpetion: Failed to delete old anonymous links.", e);
            }
        }
    };

    private static final String updateCommunityStatsTaskName = "updateCommunityStats";
    public static final Runnable UPDATE_COMMUNITY_STATS = new RobustRunnable(updateCommunityStatsTaskName) {
        @Override
        protected void dorun() {
            Communities.updateCommunityMap();
        }
    };

    private static final String weeklyReportsTaskName = "weeklyReportsTask";
    public static final Runnable CREATE_WEEKLY_REPORTS = new RobustRunnable(weeklyReportsTaskName) {
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
}
