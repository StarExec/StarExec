package com.starexec.data.to;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class Solver {
	private int id;
	transient private String path;
	private String name;
	private int userId;
	private Date uploaded;
	private String notes;
	private List<Level> supportedDivs;
	private List<Configuration> configurations;
	
	public Solver(){
		supportedDivs = new ArrayList<Level>(5);
		configurations = new ArrayList<Configuration>(3);
	}
	
	public List<Level> getSupportedDivs() {
		return supportedDivs;
	}
	
	public List<Configuration> getConfigurations(){
		return configurations;
	}

	public void addSupportedDiv(Level division) {
		this.supportedDivs.add(division);
	}
	
	public void addConfig(Configuration config){
		this.configurations.add(config);
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path){
		this.path = path;
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
	
	public Date getUploaded() {
		return uploaded;
	}
	
	public void setUploaded(Date uploaded) {
		this.uploaded = uploaded;
	}
	
	public String getName(){
		return name;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}		
}
