package com.starexec.data.to;

public class Configuration {
	private int id;
	private int solverId;
	private String name;
	private String notes;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getSolverId() {
		return solverId;
	}
	
	public void setSolverId(int solverId) {
		this.solverId = solverId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getNotes() {
		return notes;
	}
	
	public void setNotes(String notes) {
		this.notes = notes;
	}	
}
