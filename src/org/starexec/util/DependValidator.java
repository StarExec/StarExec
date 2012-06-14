package org.starexec.util;

import java.util.ArrayList;
import java.util.HashMap;
/**
 * This is a data structure used to hold information about dependencies for benchmarks as they are validated
 * 
 * @author Benton McCune
 *
 */
public class DependValidator {

	
	//these two are for the whole space.  keys are indices for benchmarks
	private HashMap<Integer, ArrayList<String>> pathMap;//value is a List of dependency paths for a benchmark
	private HashMap<Integer, ArrayList<Integer>> axiomMap;//value is a List of dependent bench id for a benchmark
	//The following map is for caching to improve performance by making far fewer calls to database
	private HashMap<String, Integer> foundDependencies;//keys are include paths, values are the benchmarks ids of secondary benchmarks
	
	//this is for a single benchmark	
	private ArrayList<String> paths;//List of dependency paths for a benchmark
	private ArrayList<Integer> axiomIds;//List of dependent bench id for a benchmark
	/**
	 * @return the pathMap
	 */
	public HashMap<Integer, ArrayList<String>> getPathMap() {
		return pathMap;
	}
	/**
	 * @param pathMap the pathMap to set
	 */
	public void setPathMap(HashMap<Integer, ArrayList<String>> pathMap) {
		this.pathMap = pathMap;
	}
	/**
	 * @return the axiomMap
	 */
	public HashMap<Integer, ArrayList<Integer>> getAxiomMap() {
		return axiomMap;
	}
	/**
	 * @param axiomMap the axiomMap to set
	 */
	public void setAxiomMap(HashMap<Integer, ArrayList<Integer>> axiomMap) {
		this.axiomMap = axiomMap;
	}
	/**
	 * @return the paths
	 */
	public ArrayList<String> getPaths() {
		return paths;
	}
	/**
	 * @param paths the paths to set
	 */
	public void setPaths(ArrayList<String> paths) {
		this.paths = paths;
	}
	/**
	 * @return the axiomIds
	 */
	public ArrayList<Integer> getAxiomIds() {
		return axiomIds;
	}
	/**
	 * @param axiomIds the axiomIds to set
	 */
	public void setAxiomIds(ArrayList<Integer> axiomIds) {
		this.axiomIds = axiomIds;
	}
	/**
	 * @return the foundDependencies
	 */
	public HashMap<String, Integer> getFoundDependencies() {
		return foundDependencies;
	}
	/**
	 * @param foundDependencies the foundDependencies to set
	 */
	public void setFoundDependencies(HashMap<String, Integer> foundDependencies) {
		this.foundDependencies = foundDependencies;
	}
	
}
