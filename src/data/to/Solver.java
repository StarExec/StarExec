package data.to;

import java.io.File;
import java.sql.Date;

public class Solver {
	private long id;
	private String path;
	private String fileName;
	private long userId;
	private Date uploaded;
	private String notes;
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
		fileName = path.substring(path.lastIndexOf(File.separator) + 1);
	}
	
	public long getUserId() {
		return userId;
	}
	
	public void setUserId(long userId) {
		this.userId = userId;
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

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}		
}
