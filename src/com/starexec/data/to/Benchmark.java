package com.starexec.data.to;

import java.io.File;
import java.sql.Date;

public class Benchmark {
	private int id;
	private String path;
	private String fileName;
	private int userid;
	private Date uploaded;
	private int level;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
		fileName = path.substring(path.lastIndexOf(File.separator) + 1);
	}
	
	public int getUserId() {
		return userid;
	}
	
	public void setUserId(int userid) {
		this.userid = userid;
	}
	
	public Date getUploaded() {
		return uploaded;
	}
	
	public void setUploaded(Date uploaded) {
		this.uploaded = uploaded;
	}
	
	public String getFileName(){
		return fileName;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}		
}
