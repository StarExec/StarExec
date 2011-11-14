package org.starexec.app;

import java.util.LinkedList;
import java.util.List;

import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;

import com.google.gson.annotations.Expose;

/**
 * Holds all helper methods and classes for our restful web services
 */
public class RESTHelpers {
	/**
	 * Takes in a list of spaces and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param spaces The list of spaces to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toSpaceTree(List<Space> spaces){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Space space: spaces){
			JSTreeItem t = new JSTreeItem(space.getName(), space.getId(), "closed", "space");	
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Represents a node in jsTree tree with certain attributes
	 * used for displaying the node and obtaining information about the node.
	 * @author Tyler Jensen
	 */	
	protected static class JSTreeItem {		
		private String data;
		private JSTreeAttribute attr;
		private List<JSTreeItem> children;
		private String state;
				
		public JSTreeItem(String name, long id, String state, String type){
			this.data = name;
			this.attr = new JSTreeAttribute(id, type);
			this.state = state;
			this.children = new LinkedList<JSTreeItem>();			
		}
		
		public List<JSTreeItem> getChildren(){
			return children;
		}
	}
	
	/**
	 * An attribute of a jsTree node which holds the node's id so
	 * that it can be passed along to other ajax methods.
	 * @author Tyler Jensen
	 */	
	protected static class JSTreeAttribute {
		private long id;		
		private String rel;
		
		public JSTreeAttribute(long id, String type){
			this.id = id;	
			this.rel = type;
		}			
	}
	
	/**
	 * Represents a space along with a user's permission for the space. Used so
	 * the client side can determine what actions a user can take on a space.
	 * @author Tyler Jensen
	 */
	protected static class SpacePermissionPair {
		@Expose private Space space;
		@Expose private Permission perm;
		
		public SpacePermissionPair(Space s, Permission p) {
			this.space = s;
			this.perm = p;
		}
	}
}
