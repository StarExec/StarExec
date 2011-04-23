package com.starexec.manage;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import com.starexec.data.Database;
import com.starexec.data.to.*;


/**
 * This is a link in the solver chain as described above.
 * Associates a solver with n benchmarks.
 * 
 */
public class SolverLink {
	private Stack<Benchmark> bPaths;
	private Solver solver;
	private Database db;
	
	public SolverLink(int sid) throws Exception {
		bPaths = new Stack<Benchmark>();
		db = new Database();
		
		solver = db.getSolver(sid);
		
		if(solver == null)
			throw new Exception("Solver id " + sid + " returned null.");
	}
	
	public int getSize() {
		return bPaths.size();
	}
	
	public void addBenchmark(int bid) throws Exception {
		Benchmark bench = db.getBenchmark(bid);
		
		if(bench == null)
			throw new Exception("Benchmark id " + bid + " returned null.");
		
		bPaths.add(bench);
	}
	
	public void addBenchmarks(Collection<Integer> ilist) {
		List<Benchmark> blist = db.getBenchmarks(ilist);
		
		for(Benchmark b : blist)
			bPaths.add(b);
	}
	
	public Solver getSolver() {
		return solver;
	}
	
	public Benchmark getNextBenchmark() {
		if(bPaths.isEmpty())
			return null;
		else
			return bPaths.pop();
	}
}
