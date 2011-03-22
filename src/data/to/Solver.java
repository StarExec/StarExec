package data.to;

import java.io.File;
import java.sql.Date;

public class Solver {
	private int id;
	private String path;
	private int user;
	private Date uploaded;
	
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
	}
	
	public int getUser() {
		return user;
	}
	
	public void setUser(int user) {
		this.user = user;
	}
	
	public Date getUploaded() {
		return uploaded;
	}
	
	public void setUploaded(Date uploaded) {
		this.uploaded = uploaded;
	}
	
	public String getFileName(){
		return path.substring(path.lastIndexOf(File.separator) + 1);
	}
}
