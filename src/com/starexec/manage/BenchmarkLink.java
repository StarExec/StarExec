package com.starexec.manage;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import com.starexec.data.Database;
import com.starexec.data.Databases;
import com.starexec.data.to.*;


/**
 * This is a link in the solver chain as described above.
 * Associates a solver with n benchmarks.
 * 
 */
public class BenchmarkLink {
	private Jobject parent;
	private Stack<Configuration> cPaths;
	private Benchmark benchmark;	
	
	public BenchmarkLink(int bid, Jobject parent) throws Exception {		
		this.cPaths = new Stack<Configuration>();
		this.benchmark = Databases.next().getBenchmark(bid);
		this.parent = parent;
		
		if(benchmark == null)
			throw new Exception("Benchmark id " + bid + " returned null.");
	}
	
	public int getSize() {
		return cPaths.size();
	}
	
	public void addConfig(int cid) throws Exception {
		Configuration c = Databases.next().getConfiguration(cid);
		
		if(c == null)
			throw new Exception("Configuration id " + cid + " returned null.");
		
		addConfig(c);
	}
	
	public void addConfigs(Collection<Integer> ilist) {
		List<Configuration> clist = Databases.next().getConfigurations(ilist);

		for(Configuration c : clist)
			addConfig(c);
	}
	
	private void addConfig(Configuration c) {
		int sid = c.getSolverId();
		cPaths.add(c);
		if(parent.getSolver(sid) == null) {
			Solver s = Databases.next().getSolver(c.getSolverId());
			parent.addSolver(s);
		}
	}
	
	public Benchmark getBenchmark() {
		return benchmark;
	}
	
	public Configuration getNextConfig() {
		if(cPaths.isEmpty())
			return null;
		else
			return cPaths.pop();
	}
}
