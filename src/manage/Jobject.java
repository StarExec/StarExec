package manage;

import java.util.*;

/**
 * This holds the paths to the solvers/benchmarks, and is passed to the job manager to begin a job.
 * For n solvers, there is at least n benchmarks.
 * The format for the chain is:
 * 
 * +------------+    +------------+
 * | solver1    | -->| solver2    |--> ...
 * +------------+    +------------+
 * +------------+    +------------+
 * | benchmark1 |    | benchmark3 |
 * +------------+    +------------+
 * | benchmark2 |    | benchmark4 |
 * +------------+    +------------+
 * +------------+    +------------+
 * | ...        |    | ...        |
 *     
 * Each link in the chain is a SolverLink (solver and benchmarks in a stack). 
 * There may be duplicates in benchmarks or solvers; there's no constraints for that right now.
 * Constraints should be added in the SolverLink object and NOT in the Jobject parent,
 * since SolverLinks will be exposed to users.
 * 
 * @author cpalmer
 *  
 */
public class Jobject {
	private Stack<SolverLink> solverChain;
	public final int JID, UID;
	public final Date jobCreated;
	
	/**
	 * This is a link in the solver chain as described above.
	 * Associates a solver with n benchmarks.
	 * 
	 */
	private class SolverLink {
		private Stack<String> benchmarks;
		private String solverPath;
		
		public SolverLink(String solverPath) {
			benchmarks = new Stack<String>();
			this.solverPath = solverPath;
		}
		
		public void addBenchmark(String b) {
			// Might want to constrain benchmarks ???
			benchmarks.push(b);
		}
		
		public String getSolver() {
			return solverPath;
		}
		
		public String getBenchMark() {
			if(!benchmarks.isEmpty())
				return "";
			else
				return benchmarks.pop();
		}
	}
	
	public Jobject() {
		this.solverChain = new Stack<SolverLink>();
		this.JID = -1;
		this.UID = -1;
		this.jobCreated = new Date(System.currentTimeMillis());
	}
	
	/**
	 * Gets the number of solvers in the solver chain.
	 * 
	 */
	public int getNumSolvers() {
		return solverChain.size();
	}
	
	/**
	 * Pops the next solver on the chain.
	 * 
	 */
	public SolverLink popLink() {
		return solverChain.pop();
	}
	
	/**
	 * Gives back the solver/benchmark stack at this index of the chain.
	 * @param index
	 * 
	 */
	public SolverLink peekLink(int index) {
		return solverChain.get(index);
	}
	
	/**
	 * Adds the solver to the chain and returns a reference to it.
	 * @param solverPath
	 * @return
	 */
	public SolverLink addSolver(String solverPath) {
		SolverLink s = new SolverLink(solverPath);
		return addSolver(s);
	}
	
	/**
	 * Adds the solver to the chain and returns a reference to it.
	 * @param s
	 * @return
	 */
	public SolverLink addSolver(SolverLink s) {
		solverChain.push(s);
		return s;
	}
}
