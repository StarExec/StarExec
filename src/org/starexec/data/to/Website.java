package org.starexec.data.to;

import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;

import com.google.gson.annotations.Expose;

public class Website extends Identifiable {
	
	/**
	 * Represents the type of the processor (along with it's SQL storage values)
	 */
	public static enum WebsiteType {
		
		USER(1), 
		SOLVER(2),
		SPACE(3);  //type for the output from a previous stage
		
		
		private int val;
		
		private WebsiteType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return this.val;
		}
		
		public static WebsiteType valueOf(int val) {
			switch(val) {			
				case 1:
					return USER;
				case 2:
					return SOLVER;
				case 3:
					return SPACE;
				default:
					return null;				
			}
		}
	}
	
	
	@Expose private String url;
	@Expose private String name;
	private int primId; // the ID of the primitive this website is associated with
	private WebsiteType type;
	/**
	 * @return the url of the website
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @param url the url to set for the website
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * @return the display name of the website
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the display name to set for the website
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return String.format("<a target=\"_blank\" href=\"%s\">%s</a>", url, name);
	}

	public int getPrimId() {
		return primId;
	}

	public void setPrimId(int primId) {
		this.primId = primId;
	}

	public WebsiteType getType() {
		return type;
	}

	public void setType(WebsiteType type) {
		this.type = type;
	}
}
