package com.starexec.manage;

import java.util.Stack;

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
	//private User usr; // Info about user building the job??
	private Stack<SolverLink> solverChain;
	
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
	public SolverLink addSolver(int sid) throws Exception {
		SolverLink lnk = new SolverLink(sid);
		addLink(lnk);
		return lnk;
	}
	
	public void addLink(SolverLink lnk) {
		solverChain.add(lnk);
	}
}
