package org.starexec.data.to;

/**
 * Represents a class of nodes, specifically it represents a hardware that is common to one or more nodes
 * 
 * @author Tyler Jensen
 * @deprecated This class will be implemented later when hardware specs are received
 */
public class NodeClass extends Identifiable {
	private String name;

	/**
	 * @return the canonical name of the class
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set for the nodeclass
	 */
	public void setName(String name) {
		this.name = name;
	}	
}
