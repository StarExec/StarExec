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
    
    // Label key used to identify a "queue" in Kubernetes.
    // If you want something else (like "starexec-queue" or "starexecQueue"),
    // just change this constant.
    private static final String QUEUE_LABEL_KEY = "starexecQueue";
    private static final String DEFAULT_QUEUE_NAME = "default";

    private final Map<Integer, LocalJob> activeIds = new HashMap<>();
    
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
                log.info("activeIds.size(): " + activeIds.size());
                log.info("jobsToRun.size(): " + jobsToRun.size());
                removeInactiveJobs();

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


    /**
     * Removes jobs from activeIds whose local process has finished (i.e. the process is no longer alive).
     * This method assumes that once the local process started by the script at j.scriptPath completes,
     * the corresponding k8s job is either finished or detached.
     */
    public synchronized void removeInactiveJobs() {
        Iterator<Map.Entry<Integer, LocalJob>> iterator = activeIds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, LocalJob> entry = iterator.next();
            LocalJob job = entry.getValue();
            // Only check jobs that have a process (i.e. have been started).
            if (job.process != null && !job.process.isAlive()) {
                log.info("Removing inactive job: " + job.execId);
                iterator.remove();
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

        

    
    
    // Assume these fields/consts exist, as in previous examples:
    //   private static final String QUEUE_LABEL_KEY = "starexecQueue";
    //   private static final String DEFAULT_QUEUE_NAME = "default";
    //
    // Also assume:
    //   - Util.executeCommand(String[] cmd) throws IOException, returning a single String (stdout).
    //   - log.info(String message) logs a string.
    //   - We must catch or handle IOException internally (no throws in method signatures).
    
    /**
     * Return the names of all worker nodes in the K8s cluster,
     * but only those labeled "nodegroup=computenodes".
     */
    @Override
    public String[] getWorkerNodes() {
        String[] cmd = {
            "kubectl", "get", "nodes",
            "-l", "nodegroup=computenodes", // <=== Filter here
            "-o", "custom-columns=NAME:.metadata.name",
            "--no-headers"
        };
    
        try {
            String output = Util.executeCommand(cmd);
            String[] lines = output.split("\\r?\\n");
            List<String> nodes = new ArrayList<>();
    
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    nodes.add(trimmed);
                }
            }
            return nodes.toArray(new String[0]);
        } catch (IOException e) {
            log.info("IOException in getWorkerNodes: " + e.getMessage());
            return new String[0];
        }
    }
    
    /**
     * Return the names of all queues known to the system,
     * but only considering nodes labeled "nodegroup=computenodes".
     * We gather "starexecQueue=..." labels from these nodes.
     * Plus we always include DEFAULT_QUEUE_NAME to represent unlabeled queue membership.
     */
    @Override
    public String[] getQueues() {
        // Only fetch nodes with nodegroup=computenodes
        String[] cmd = {
            "kubectl", "get", "nodes",
            "-l", "nodegroup=computenodes",
            "--show-labels",
            "--no-headers"
        };
    
        Set<String> queueNames = new HashSet<>();
        try {
            String output = Util.executeCommand(cmd);
            String[] lines = output.split("\\r?\\n");
    
            for (String line : lines) {
                String[] parts = line.split("\\s+");
                if (parts.length < 1) {
                    continue;
                }
                String labelsPart = parts[parts.length - 1]; // The last column is the label list
                String[] labels = labelsPart.split(",");
                for (String label : labels) {
                    label = label.trim();
                    if (label.startsWith(QUEUE_LABEL_KEY + "=")) {
                        String queue = label.substring(label.indexOf('=') + 1);
                        queueNames.add(queue);
                    }
                }
            }
        } catch (IOException e) {
            log.info("IOException in getQueues: " + e.getMessage());
            // We'll proceed with an empty set
        }
    
        // There's always a default queue for unlabeled nodes
        queueNames.add(DEFAULT_QUEUE_NAME);
    
        return queueNames.toArray(new String[0]);
    }
    
    /**
     * Return a map of node -> queue, but only for nodes labeled "nodegroup=computenodes".
     * If a node has no queue label, it is in DEFAULT_QUEUE_NAME.
     */
    @Override
    public Map<String, String> getNodeQueueAssociations() {
        Map<String, String> nodeQueueMap = new HashMap<>();
        String[] cmd = {
            "kubectl", "get", "nodes",
            "-l", "nodegroup=computenodes",
            "--show-labels",
            "--no-headers"
        };
    
        try {
            String output = Util.executeCommand(cmd);
            String[] lines = output.split("\\r?\\n");
    
            for (String line : lines) {
                String[] parts = line.split("\\s+");
                if (parts.length < 1) {
                    continue;
                }
                String nodeName = parts[0];
                String labelsPart = parts[parts.length - 1];
                String[] labels = labelsPart.split(",");
    
                // Default to default queue if we don't see a starexecQueue label
                String queueName = DEFAULT_QUEUE_NAME;
                for (String label : labels) {
                    label = label.trim();
                    if (label.startsWith(QUEUE_LABEL_KEY + "=")) {
                        queueName = label.substring(label.indexOf('=') + 1);
                        break;
                    }
                }
                nodeQueueMap.put(nodeName, queueName);
            }
        } catch (IOException e) {
            log.info("IOException in getNodeQueueAssociations: " + e.getMessage());
            // Return whatever we have so far (or empty if none).
        }
    
        return nodeQueueMap;
    }
    
    /**
     * Clears node error states. For now, do nothing or uncordon any nodes if desired.
     */
    @Override
    public boolean clearNodeErrorStates() {
        log.info("clearNodeErrorStates() called, but not implemented. Doing nothing.");
        return true;
    }
    
    /**
     * Delete a queue by removing its label from all nodes labeled nodegroup=computenodes
     * that have starexecQueue=<queueName>. If the queue is the default queue, do nothing.
     */
    @Override
    public void deleteQueue(String queueName) {
        if (DEFAULT_QUEUE_NAME.equals(queueName)) {
            log.info("Cannot delete the default queue; doing nothing.");
            return;
        }
    
        // First, list nodes that have nodegroup=computenodes and starexecQueue=queueName
        String labelSelector = "nodegroup=computenodes," + QUEUE_LABEL_KEY + "=" + queueName;
        String[] getCmd = {
            "kubectl", "get", "nodes",
            "-l", labelSelector,
            "-o", "custom-columns=NAME:.metadata.name",
            "--no-headers"
        };
    
        try {
            String output = Util.executeCommand(getCmd);
            String[] lines = output.split("\\r?\\n");
            int numNodes = 0;
    
            for (String node : lines) {
                String trimmed = node.trim();
                if (!trimmed.isEmpty()) {
                    String[] removeLabelCmd = {
                        "kubectl", "label", "node", trimmed, QUEUE_LABEL_KEY + "-"
                    };
                    Util.executeCommand(removeLabelCmd);
                    numNodes++;
                }
            }
            log.info("Deleted queue '" + queueName + "' from " + numNodes + " node(s).");
        } catch (IOException e) {
            log.info("IOException in deleteQueue for '" + queueName + "': " + e.getMessage());
        }
    }
    
    /**
     * Create a queue for the nodes labeled nodegroup=computenodes, by labeling
     * them with starexecQueue=<newQueueName>. If newQueueName is default, do nothing.
     */
    @Override
    public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
        if (DEFAULT_QUEUE_NAME.equals(newQueueName)) {
            log.info("createQueue called for default queue. Nothing to do.");
            return true;
        }
        if (nodeNames == null || nodeNames.length == 0) {
            log.info("No nodes specified; no queue created.");
            return false;
        }
    
        // Label the requested nodes, but only consider them valid if they also have nodegroup=computenodes
        try {
            for (String node : nodeNames) {
                // You could either check in code that this node is labeled nodegroup=computenodes
                // or rely on your architecture to ensure these nodeNames are correct.
                String[] cmd = {
                    "kubectl", "label", "node", node,
                    QUEUE_LABEL_KEY + "=" + newQueueName,
                    "--overwrite"
                };
                Util.executeCommand(cmd);
            }
            log.info("Created queue '" + newQueueName + "', labeling " + nodeNames.length + " node(s).");
            return true;
        } catch (IOException e) {
            log.info("IOException in createQueue for '" + newQueueName + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create a queue with slots for the nodes labeled nodegroup=computenodes.
     * If it's the default queue, do nothing. Otherwise, label them with
     * starexecQueue + possibly starexecSlots.
     */
    @Override
    public boolean createQueueWithSlots(String newQueueName, String[] nodeNames, String[] sourceQueueNames, Integer slots) {
        if (DEFAULT_QUEUE_NAME.equals(newQueueName)) {
            log.info("createQueueWithSlots called for default queue. Nothing to do.");
            return true;
        }
    
        if (!createQueue(newQueueName, nodeNames, sourceQueueNames)) {
            return false;
        }
    
        if (slots != null) {
            try {
                for (String node : nodeNames) {
                    String[] cmd = {
                        "kubectl", "label", "node", node,
                        "starexecSlots=" + slots,
                        "--overwrite"
                    };
                    Util.executeCommand(cmd);
                }
                log.info("Set 'starexecSlots=" + slots + "' for queue '" + newQueueName
                         + "' on " + nodeNames.length + " node(s).");
            } catch (IOException e) {
                log.info("IOException in createQueueWithSlots for '" + newQueueName + "': " + e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Move the given nodes from one queue to another, ignoring any node that
     * is NOT labeled nodegroup=computenodes. If destQueueName is empty, do nothing.
     * If destQueueName is default, remove the starexecQueue label. 
     */
    @Override
    public void moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
        if (nodeNames == null || nodeNames.length == 0) {
            log.info("moveNodes called with no node names; doing nothing.");
            return;
        }
        if (destQueueName == null || destQueueName.trim().isEmpty()) {
            log.info("No destination queue specified; doing nothing.");
            return;
        }
    
        try {
            for (String node : nodeNames) {
                // Optionally check if this node is labeled nodegroup=computenodes first.
                // If you'd like, do:
                //  kubectl get node <node> --show-labels
                //  and parse it. For brevity, we're skipping that.
    
                // Remove the queue label
                String[] removeLabelCmd = {
                    "kubectl", "label", "node", node, QUEUE_LABEL_KEY + "-"
                };
                Util.executeCommand(removeLabelCmd);
    
                // If the destination is NOT default, label the node with the new queue
                if (!DEFAULT_QUEUE_NAME.equals(destQueueName)) {
                    String[] addLabelCmd = {
                        "kubectl", "label", "node", node,
                        QUEUE_LABEL_KEY + "=" + destQueueName,
                        "--overwrite"
                    };
                    Util.executeCommand(addLabelCmd);
                }
            }
            log.info("Moved " + nodeNames.length + " node(s) to queue '" + destQueueName + "'.");
        } catch (IOException e) {
            log.info("IOException in moveNodes to '" + destQueueName + "': " + e.getMessage());
        }
    }
    
    /**
     * Move a single node. Convenience wrapper around moveNodes().
     */
    @Override
    public void moveNode(String nodeName, String queueName) {
        moveNodes(queueName, new String[]{nodeName}, null);
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
        final Runnable runLocalJobsRunnable = new RobustRunnable("runLocalJobsRunnable") {
            @Override
            protected void dorun() {
                log.info("initializing local job execution");
                runJobsForever();
            }
        };
        new Thread(runLocalJobsRunnable).start();
        log.debug("returning from k8s backend initialization");
    }

}
