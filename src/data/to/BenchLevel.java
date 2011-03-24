package data.to;

import com.sun.jndi.toolkit.url.Uri;

public class BenchLevel {
	private String name;
	private String path;
	private int belongsTo;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getBelongsTo() {
		return belongsTo;
	}
	public void setBelongsTo(int belongsTo) {
		this.belongsTo = belongsTo;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
}
