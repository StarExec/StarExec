package data.to;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class Solver {
	private int id;
	private String path;
	private String fileName;
	private int userId;
	private Date uploaded;
	private String notes;
	private List<Level> supportedDivs;
	
	public Solver(){
		supportedDivs = new ArrayList<Level>(5);
	}
	
	public List<Level> getSupportedDivs() {
		return supportedDivs;
	}

	public void addSupportedDiv(Level division) {
		this.supportedDivs.add(division);
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
	
	public void setPath(String path) {
		this.path = path;
		fileName = path.substring(path.lastIndexOf(File.separator) + 1);
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
