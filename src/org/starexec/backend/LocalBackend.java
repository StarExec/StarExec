package org.starexec.backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.starexec.constants.R;
import org.starexec.data.database.Queues;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;

import com.sun.jmx.remote.internal.ArrayQueue;

//TODO: Setup a thread that starts when the backend is initialized. This thread should just run a single job
// at a time, sleeping for a while whenever there is no new work. Jobs should just get kept in a simple FIFO queue
// as they are submitted.
public class LocalBackend implements Backend {
	private static Logger log = Logger.getLogger(LocalBackend.class);

	private class LocalJob {
		public int execId;
		public String scriptPath;
		public String workingDirectoryPath;
		public String logPath;
		public Process process;
	}
	boolean killingPairs = false;
	private File executeDirectory = null;
	
	private Set<Integer> activeIds = new HashSet<Integer>();
	
	private HashMap<String, Queue> namesToQueues = new HashMap<String, Queue>();
	private static final String NODE_NAME = "n001";
	java.util.Queue<LocalJob> jobsToRun = new ArrayDeque<LocalJob>();
	
	
	private int generateExecId() {
		return -1;
	}
	
	
	/**
	 * BACKEND_ROOT needs to point to a directory in which jobs may be executed.
	 * Starexec will need full permissions in this directory
	 */
	@Override
	public void initialize(String BACKEND_ROOT) {
		executeDirectory = new File(BACKEND_ROOT);
	}

	@Override
	public void destroyIf() {		
	}

	@Override
	public boolean isError(int execCode) {
		return execCode<=0;
	}

	@Override
	public synchronized int submitScript(String scriptPath, String workingDirectoryPath, String logPath) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public synchronized boolean killPair(int execId) {
		killingPairs = true;
		try {
			for (LocalJob job : jobsToRun) {
				if (job.execId==execId) {
					jobsToRun.remove(job);
					if (job.process!=null) {
						job.process.destroyForcibly();
					}
					activeIds.remove(execId);
					break;
				}
			}
			return true;
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
			return false;
		} finally  {
			killingPairs = false;
		}
		
	}

	@Override
	public synchronized boolean killAll() {
		killingPairs = true;
		try {
			int size = jobsToRun.size();
			while (size>0 && !jobsToRun.isEmpty()) {
				LocalJob j = jobsToRun.poll();
				if (j.process!=null) {
					j.process.destroyForcibly();
				}
				activeIds.remove(j.execId);
				size--;
			}
			return true;
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
			return false;
		} finally {
			killingPairs = false;
		}
		
	}

	@Override
	public String getRunningJobsStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> getActiveExecutionIds() throws IOException {
		return activeIds;
	}

	@Override
	public String[] getWorkerNodes() {
		return new String[] {NODE_NAME};
	}

	@Override
	public String[] getQueues() {
		return new String[] {R.DEFAULT_QUEUE_NAME};
	}

	@Override
	public Map<String, String> getNodeQueueAssociations() {
		HashMap<String, String> mapping = new HashMap<String, String>();
		mapping.put(NODE_NAME, R.DEFAULT_QUEUE_NAME);
		return mapping;
	}

	@Override
	public boolean clearNodeErrorStates() {
		return true;
	}

	/*
	 * This backend does not support having multiple nodes or queues, so all of the functions
	 * below simply return false.
	 */
	@Override
	public boolean deleteQueue(String queueName) {
		return false;
	}

	@Override
	public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
		return false;
	}

	@Override
	public boolean moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
		return true;
	}

	@Override
	public boolean moveNode(String nodeName, String queueName) {
		return false;
	}

}
