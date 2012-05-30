package org.starexec.util;

import java.util.ArrayList;
import java.util.HashMap;

public class DependValidator {

	
	//these two are for the whole space
	private HashMap<Integer, ArrayList<String>> pathMap;
	private HashMap<Integer, ArrayList<Integer>> axiomMap;
	
	//this is for a single benchmark	
	private ArrayList<String> paths;
	private ArrayList<Integer> axiomIds;
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
	
}
