package org.starexec.data.to;

import com.google.gson.annotations.Expose;

/**
 * Represents a benchmark dependency in the database
 * 
 * @author Benton McCune
 */
public class BenchmarkDependency extends Identifiable{
	private Benchmark primaryBench;
	@Expose private Benchmark secondaryBench; //e.g. an axiom that the primary benchmark references
	@Expose private String dependencyPath ="";//the path to the file that we want in the execution host
	
	/**
	 * @return the primaryBench
	 */
	public Benchmark getPrimaryBench() {
		return primaryBench;
	}
	/**
	 * @param primaryBench the primaryBench to set
	 */
	public void setPrimaryBench(Benchmark primaryBench) {
		this.primaryBench = primaryBench;
	}
	/**
	 * @return the secondaryBench
	 */
	public Benchmark getSecondaryBench() {
		return secondaryBench;
	}
	/**
	 * @param secondaryBench the secondaryBench to set
	 */
	public void setSecondaryBench(Benchmark secondaryBench) {
		this.secondaryBench = secondaryBench;
	}
	/**
	 * @return the dependencyPath
	 */
	public String getDependencyPath() {
		return dependencyPath;
	}
	/**
	 * @param dependencyPath the dependencyPath to set
	 */
	public void setDependencyPath(String dependencyPath) {
		this.dependencyPath = dependencyPath;
	}

}
