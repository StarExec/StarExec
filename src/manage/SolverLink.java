package manage;

import java.util.Stack;
import data.to.*;
import java.util.*;
import data.Database;


/**
 * This is a link in the solver chain as described above.
 * Associates a solver with n benchmarks.
 * 
 */
public class SolverLink {
	private Stack<String> bPaths;
	private String spath;
	private Database db;
	
	public SolverLink(int sid) throws Exception {
		bPaths = new Stack<String>();
		db = new Database();
		
		spath = db.getSolver(sid).getPath();
		
		if(spath == null)
			throw new Exception("Solver id " + sid + " returned null.");
	}
	
	public int getSize() {
		return bPaths.size();
	}
	
	public void addBenchmark(int bid) throws Exception {
		String bpath = db.getBenchmark(bid).getPath();
		
		if(bpath == null)
			throw new Exception("Benchmark id " + bid + " returned null.");
		
		bPaths.add(bpath);
	}
	
	public void addBenchmarks(Collection<Integer> ilist) {
		List<Benchmark> blist = db.getBenchmarks(ilist);
		
		for(Benchmark b : blist)
			bPaths.add(b.getFileName());
	}
	
	public String getSolverPath() {
		return spath;
	}
	
	public String getNextBenchmarkPath() {
		if(bPaths.isEmpty())
			return null;
		else
			return bPaths.pop();
	}
}
