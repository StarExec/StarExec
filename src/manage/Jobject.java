package manage;

import java.util.Stack;
import data.Database;

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
	
	/**
	 * This is a link in the solver chain as described above.
	 * Associates a solver with n benchmarks.
	 * 
	 */
	private class SolverLink {
		private Stack<String> bPaths;
		private String sPath;
		
		public SolverLink(Long sid) {
			bPaths = new Stack<String>();
			String sPath = null;
			
			// Contact database to get the String path of the solver SID
			
			this.sPath = sPath;
		}
		
		public int getSize() {
			return bPaths.size();
		}
		
		public void addBenchmarkPath(Long bid) {
			String bPath = "";
			
			// Contact database to receive the String path that corrosponds to BID
			
			bPaths.add(bPath);
		}
		
		public String getSolverPath() {
			return sPath;
		}
		
		public String getNextBenchmarkPath() {
			if(!bPaths.isEmpty())
				return null;
			else
				return bPaths.pop();
		}
	}
	
	public Jobject() {
		this.solverChain = new Stack<SolverLink>();
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
	 * Gives back the solver/benchmark stack at this index of the chain. Useful if scanning
	 * @param Index in chain
	 * 
	 */
	public SolverLink peekLink(int index) {
		return solverChain.get(index);
	}
	
	/**
	 * Adds the solver to the chain and returns a reference to it.
	 * The passed-back reference allows you to continue to add benchmarks to a particular solver, 
	 * the idea being you populate a solver link before moving onto the next solver in the job.
	 * @param Solver ID
	 * @return The SolverLink just created.
	 */
	public SolverLink addSolver(Long sid) {
		SolverLink lnk = new SolverLink(sid);
		solverChain.add(lnk);
		return lnk;
	}
}
