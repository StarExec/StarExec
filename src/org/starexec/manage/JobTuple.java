package org.starexec.manage;

import org.starexec.data.to.*;
import org.starexec.data.database.*;

/**
 * The DS that houses a single jobtuple (currently solver, config, benchmark, queue)
 * @author Clifton Palmer
 */
public class JobTuple {
	private Solver solver;
	private Configuration conf;
	private Benchmark bench;
	private JobPair jobPair;
	private Queue queue;
	
	/**
	 * This builds a JobTuple out of a pair and queue.
	 * @param pid : pair id
	 * @param qid : queue id
	 */
	public JobTuple(long pid, long qid) {
		long confId, benchmarkId, solverId = -1;
		
		// Retrieve job_pair to get confId, benchmarkId
		jobPair = Jobs.getPair(pid);
		confId = jobPair.getConfigId();
		benchmarkId = jobPair.getBenchmarkId();
		
		// Retrieve solverId from config
		conf = Solvers.getConfiguration(confId);
		solverId = conf.getSolverId();
		
		// Retrieve benchmark, solver, queue objects
		solver = Solvers.get(solverId);
		bench = Benchmarks.get(benchmarkId);
		queue = Queues.get(qid);
	}
	
	public Solver getSolver() {
		return solver;
	}
	
	public Configuration getConfiguration() {
		return conf;
	}
	
	public Benchmark getBenchmark() {
		return bench;
	}
	
	public Queue getQueue() {
		return queue;
	}

	public JobPair getJobPair() {
		return jobPair;
	}
}
