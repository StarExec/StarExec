package com.starexec.data.to;

import java.util.*;

public class Level {
	private int left;
	private int right;
	private String name;
	private int userId;
	private int groupId;
	private int id;
	private int depth;
	private String description;
	private List<Benchmark> benchmarks;
	
	public Level(int id){
		this.id = id;		
		benchmarks = new ArrayList<Benchmark>();
	}
	
	public Level(){
		benchmarks = new ArrayList<Benchmark>();
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public int getLeft() {
		return left;
	}
	
	public void setLeft(int left) {
		this.left = left;
	}
	
	public int getRight() {
		return right;
	}
	
	public void setRight(int right) {
		this.right = right;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public List<Benchmark> getBenchmarks() {
		return benchmarks;
	}

	public void setBenchmarks(List<Benchmark> benchmarks) {
		this.benchmarks = benchmarks;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}			
}
