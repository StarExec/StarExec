package com.starexec.data.to;

import java.io.File;
import java.sql.Timestamp;

public class Benchmark {
	private int id;
	transient private String path;
	private String fileName;
	private int userid;
	private int communityId;
	private Timestamp uploaded;
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
	
	public Timestamp getUploaded() {
		return uploaded;
	}
	
	public void setUploaded(Timestamp uploaded) {
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

	public int getCommunityId() {
		return communityId;
	}

	public void setCommunityId(int communityId) {
		this.communityId = communityId;
	}		
}
