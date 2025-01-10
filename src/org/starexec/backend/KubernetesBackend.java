/*
* This assumes the head node is able to talk to an existing kubernetes cluster
* using kubectl. See https://kubernetes.io/docs/tasks/tools/install-kubectl/
* 
* The head node itself needn't be a kubernetes node I believe.
* LocalBackend is where a lot of this code comes from, but I didn't
* want to extend it directly since kubernetes isn't local.
* 
*/

package org.starexec.backend;

import java.io.IOException;
import java.io.File;
import java.util.*;

import org.starexec.constants.R;
import org.starexec.logger.StarLogger;
import org.starexec.util.RobustRunnable;
import org.starexec.util.Util;


public class KubernetesBackend implements Backend {
    private static final StarLogger log = StarLogger.getLogger(KubernetesBackend.class);
    
    // You can set this to any desired number 
    // (Should be >= num compute nodes in cluster, so they can all be used concurrently)
    // However, because the k8s backend runs the runscript/jobscript locally on the head node (which then dispatches the actual heavy jobs via k8s),
    // this needs to be low enough that the head node can handle it.
    // Think about "ulimit -Sn" which shows the max number of subprocesses a user/process can make.
    private static final int MAX_CONCURRENT_JOBS = 50;

    
    private final Map<Integer, LocalJob> activeIds = new HashMap<>();
    
    private String NODE_NAME = "n001";
    /**
     * An ordered queue of all jobs that have been 
     * submitted to the backend and have not yet completed. 
     * Jobs are kept in this queue until they are finished executing,
     * meaning that the running job will be the head of the queue
     */
    final java.util.Queue<LocalJob> jobsToRun = new ArrayDeque<>();
    private int curID = 1;

    private static class LocalJob {
        public int execId = 0;
        public String scriptPath = "";
        public String workingDirectoryPath = "";
        public String logPath = "";
        public Process process;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(scriptPath).append(" ");
            sb.append(execId).append(" ");
            if (process != null) {
                sb.append("running");
            } else {
                sb.append("pending");
            }
            return sb.toString();
        }
    }


    /**
     * Generates a new ID that is unique among all jobs currently enqueued/ running
     * 
     * @return
     * @throws Exception
     */
    private int generateExecId() throws Exception {
        if (activeIds.size() == Integer.MAX_VALUE) {
            throw new Exception("Cannot support more that Integer.MAX_VALUE pairs");
        }
        while (true) {
            curID = (curID + 1) % Integer.MAX_VALUE;
            // make sure the ID is not 0 when we return it
            curID = Math.max(curID, 1);
            if (!activeIds.containsKey(curID)) {
                return curID;
            }
        }
    }

    @Override
    public boolean isError(int execCode) {
        return execCode <= 0;
    }


    private synchronized void runJob(LocalJob j) {
        try {
            ProcessBuilder builder = new ProcessBuilder(j.scriptPath);
            builder.redirectErrorStream(true);
            builder.directory(new File(j.workingDirectoryPath));
            builder.redirectOutput(new File(j.logPath));
    
            // Start the job in a separate thread so it doesn't block the main loop
            Thread jobThread = new Thread(() -> {
                try {
                    j.process = builder.start();
                    j.process.waitFor();  // Wait for the job to complete
                    log.info("Job " + j.execId + " completed");
                } catch (Exception e) {
                    log.error("Error running job " + j.execId + ": " + e.getMessage(), e);
                }
            });
    
            jobThread.start();  // Start the thread to run the job
        } catch (Exception e) {
            log.error("Error starting job " + j.execId + ": " + e.getMessage(), e);
        }
    }
    
    private void runJobsForever() {
        while (true) {
            try {
                // Sleep for a while if there are no jobs to run
                if (jobsToRun.isEmpty()) {
                    Thread.sleep(R.JOB_SUBMISSION_PERIOD * 1000);
                    continue;
                }
    
                // Check if the number of currently running jobs is below the limit
                if (activeIds.size() < MAX_CONCURRENT_JOBS) {
                    // Peek the job queue but don't remove the job yet
                    LocalJob job = jobsToRun.peek();
                    if (job != null) {
                        // Start the job and let it run asynchronously
                        runJob(job);
                        // Remove the job from the queue after it's started
                        jobsToRun.poll();
                    }
                }
    
                // Sleep for a short time before checking the job queue again
                Thread.sleep(200);
    
            } catch (Exception e) {
                log.error("Error in job execution loop: " + e.getMessage(), e);
            }
        }
    }
    

    @Override
    public synchronized int submitScript(String scriptPath, String workingDirectoryPath, String logPath) {
        try {
            LocalJob j = new LocalJob();
            j.execId = generateExecId();
            j.scriptPath = scriptPath;
            j.workingDirectoryPath = workingDirectoryPath;
            j.logPath = logPath;
            activeIds.put(j.execId, j);
            jobsToRun.add(j);
            return j.execId;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public synchronized boolean killPair(int execId) {
        try {
            if (activeIds.containsKey(execId)) {
                LocalJob job = activeIds.get(execId);

                if (job.process != null) {
                    job.process.destroyForcibly();
                }
                jobsToRun.remove(job);
                activeIds.remove(execId);
            }
            return true;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return false;
        }

    }

    @Override
    public synchronized boolean killAll() {
        try {
            while (!jobsToRun.isEmpty()) {
                LocalJob j = jobsToRun.poll();
                if (j.process != null) {
                    j.process.destroyForcibly();
                }
                activeIds.remove(j.execId);
            }
            return true;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return false;
        }

    }

    @Override
    public synchronized String getRunningJobsStatus() {
        StringBuilder sb = new StringBuilder();
        for (LocalJob j : jobsToRun) {
            sb.append(j.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public Set<Integer> getActiveExecutionIds() throws IOException {
        // we don't want to return the keyset of activeIds, since
        // changes to that set are reflected in the map, meaning returning it
        // makes activeIds externally mutable
        Set<Integer> newSet = new HashSet<>();
        newSet.addAll(activeIds.keySet());
        return newSet;
    }

    @Override
    public String[] getWorkerNodes() {
        return new String[] { NODE_NAME };
    }

    @Override
    public String[] getQueues() {
        return new String[] { R.DEFAULT_QUEUE_NAME };
    }

    @Override
    public Map<String, String> getNodeQueueAssociations() {
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put(NODE_NAME, R.DEFAULT_QUEUE_NAME);
        return mapping;
    }

    @Override
    public boolean clearNodeErrorStates() {
        return true;
    }

    /*
     * This backend does not support having multiple nodes or queues, so all of the
     * functions
     * below simply return false.
     */
    @Override
    public void deleteQueue(String queueName) {
    }

    @Override
    public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
        return false;
    }

    @Override
    public boolean createQueueWithSlots(String newQueueName, String[] nodeNames, String[] sourceQueueNames,
            Integer slots) {
        return false;
    }

    @Override
    public void moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
    }

    @Override
    public void moveNode(String nodeName, String queueName) {
    }

    @Override
    public void destroyIf() {
        // no deconstruction needed
    }

    /**
     * BACKEND_ROOT is not meaningful for this backend and will be ignored.
     * Initialization creates the execution loop for local jobs
     */
    @Override
    public void initialize(String BACKEND_ROOT) {
        // set the name of the single node used by this backend to the name of the
        // system
        try {
            String nodeName = Util.executeCommand("hostname");
            NODE_NAME = nodeName.split("\n")[0];
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        final Runnable runLocalJobsRunnable = new RobustRunnable("runLocalJobsRunnable") {
            @Override
            protected void dorun() {
                log.info("initializing local job execution");
                runJobsForever();
            }
        };
        new Thread(runLocalJobsRunnable).start();
        log.debug("returning from local backend initialization");
    }

}
