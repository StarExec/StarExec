package org.starexec.manage;


/**
 * This holds the paths to the solvers/benchmarks, and is passed to the job manager to begin a job.
 * For n benchmarks, there is at least n solvers.
 * The format for the chain is:
 * 
 * +------------+    +------------+
 * | benchmark1 | -->| benchmark2 |--> ...
 * +------------+    +------------+
 * +------------+    +------------+
 * | config1    |    | config2    |
 * +------------+    +------------+
 * | config2    |    | config3    |
 * +------------+    +------------+
 * +------------+    +------------+
 * | ...        |    | ...        |
 *     
 * Each link in the chain is a SolverLink (benchmarks and solver in a stack). 
 * There may be duplicates in benchmarks or solvers; there's no constraints for that right now.
 * Constraints should be added in the SolverLink object and NOT in the Jobject parent,
 * since SolverLinks will be exposed to users.
 * 
 * @author Clifton Palmer
 * @deprecated OUT OF DATE
 */
public class Jobject {
//	private User usr; // Info about user building the job??
//	private String description;
//	private Stack<BenchmarkLink> chain;
//	private HashMap<Integer, Solver> solvers; // Holds common solver info.
//	
//	public Jobject(User usr) {
//		this.usr = usr;
//		this.description = "None";
//		this.solvers = new HashMap<Integer, Solver>();
//		this.chain = new Stack<BenchmarkLink>();
//	}
//	
//	public void setDescription(String description) {
//		this.description = description;
//	}
//	
//	public void addSolver(Solver s) {
//		solvers.put(s.getId(), s);
//	}
//	
//	public Solver getSolver(int sid) {
//		return solvers.get(sid);
//	}
//	
//	public String getDescription() {
//		return this.description;
//	}
//	
//	public User getUser() {
//		return usr;
//	}
//
//	public int getNumBenchmarks() {
//		return chain.size();
//	}
//	
//	public BenchmarkLink popLink() {
//		return chain.pop();
//	}
//	
//	/**
//	 * Gives back the solver/benchmark stack at this index of the chain. Useful if scanning
//	 * @param Index in chain
//	 * 
//	 */
//	public BenchmarkLink peekLink(int index) {
//		return chain.get(index);
//	}
//	
//	/**
//	 * Adds the solver to the chain and returns a reference to it.
//	 * The passed-back reference allows you to continue to add benchmarks to a particular solver, 
//	 * the idea being you populate a solver link before moving onto the next solver in the job.
//	 * @param Solver ID
//	 * @return The SolverLink just created.
//	 */
//	public BenchmarkLink addBenchmark(int bid) throws Exception {
//		BenchmarkLink lnk = new BenchmarkLink(bid, this);
//		addLink(lnk);
//		return lnk;
//	}
//	
//	public void addLink(BenchmarkLink lnk) {
//		chain.add(lnk);
//	}
}
