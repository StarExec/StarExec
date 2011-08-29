package com.starexec.app;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import com.starexec.data.to.*;

/**
 * Contains helper methods and classes for the RESTServices class. These helpers are
 * used to allow proper JSON strings to be returned to client-side jQuery plugins
 * that require special formatting.
 * 
 * @author Tyler Jensen
 */
public class RESTHelpers {
	
	private static Logger log = Logger.getLogger(RESTHelpers.class);
	protected static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd, yyyy h:mm a");
	
	/**
	 * Takes in a list of job pairs and returns a table that
	 * can be displayed in the jQuery flexigrid
	 * @param jList List of job pairs to convert for display
	 * @return Flexigrid-friendly object containing the pair's data
	 */
	protected static TableRow pairToTableRow(List<JobPair> jList){
		
		TableRow tr = new TableRow(1, jList.size());
		for(JobPair jp : jList){
			Row r = new Row(jp.getId());
			r.addCell(jp.getId());
			r.addCell(jp.getStatus());
			r.addCell(jp.getResult());
			r.addCell(jp.getSolver().getName());
			
			if(jp.getConfig().getName().length() > 3) {
				r.addCell(jp.getConfig().getName().substring(3));
			} else {
				r.addCell("Default");
			}
			
			r.addCell(jp.getBenchmark().getFileName());		
			r.addCell(jp.getRunTime());
			
			if(jp.getStartTime() != null) {
				r.addCell(dateFormat.format(jp.getStartTime()));
			} else {
				r.addCell("");
			}
			
			if(jp.getEndTime() != null) {
				r.addCell(dateFormat.format(jp.getEndTime()));
			} else {
				r.addCell("");
			}
			
			r.addCell(jp.getNode());			
			tr.addRow(r);
		}
		
		return tr;
	}
	
	/**
	 * Converts a job list into the appropriate format to be
	 * displayed by jquery's flexigrid.
	 * @param jList The list of jobs to format
	 * @return A TableRow object that can be serialized to JSON in the proper format
	 */
	protected static  TableRow jobToTableRow(List<Job> jList){
		TableRow tr = new TableRow(1, jList.size());
		for(Job j : jList){
			Row r = new Row(j.getJobId());
			r.addCell(j.getJobId());
			r.addCell(j.getStatus());
			r.addCell(j.getRunTime());
			
			if(j.getSubmitted() != null) {
				r.addCell(dateFormat.format(j.getSubmitted()));
			} else {
				r.addCell("");
			}
			
			if(j.getCompleted() != null) {
				r.addCell(dateFormat.format(j.getCompleted()));
			} else {
				r.addCell("");
			}
			
			r.addCell(j.getNode());
			r.addCell(j.getTimeout());
			tr.addRow(r);
		}
		
		return tr;
	}
	
	/**
	 * A collection of rows and metadata that represents the format
	 * that the jQuery flexigrid requires. This can be serialized and sent
	 * to a client to be displayed properly in the flexigrid.
	 */
	public static class TableRow {
		private int page;
		private int total;
		private List<Row> rows;
		
		public TableRow(int page, int total){
			this.page = page;
			this.total = total;
			rows = new LinkedList<Row>();
		}
		
		public void addRow(Row r){
			rows.add(r);
		}
	}
	
	/**
	 * Represents a row (collection of cells with an id) in the jQuery flexigrid	 
	 */
	public static class Row {
		private int id;
		private List<String> cell;
		
		public Row(int id){
			this.id = id;
			cell = new LinkedList<String>();
		}
		
		public void addCell(Object o){
			cell.add(o.toString());
		}
	}
	
	/**
	 * Takes in a list of levels and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param levels The list of levels to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 */
	@SuppressWarnings("unused")
	protected static List<JSTreeItem> toLevelBenchTree(List<Level> levels){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Level level : levels){
			JSTreeItem t = new JSTreeItem(level.getName(), level.getId(), "closed", "level");
			
			for(Benchmark b : level.getBenchmarks()){
				JSTreeItem t1 = new JSTreeItem(b.getFileName(), b.getId(), "leaf", "bench");
				t.getChildren().add(t1);
			}
			
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Takes in a list of levels and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param levels The list of levels to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 */
	@SuppressWarnings("unused")
	protected static List<JSTreeItem> toLevelTree(List<Level> levels){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Level level : levels){
			JSTreeItem t = new JSTreeItem(level.getName(), level.getId(), "closed", "level");	
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Takes in a list of solvers and gives back a list of tree items that can be
	 * serialized into JSON and displayed by the jsTree plugin.
	 * @param solvers The solvers to format
	 * @return A formatable list of JSTreeItems
	 */
	@SuppressWarnings("unused")
	protected static List<JSTreeItem> toSolverTree(List<Solver> solvers){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Solver solver : solvers){
			JSTreeItem t = new JSTreeItem(solver.getName(), solver.getId(), "closed", "solver");			
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Builds a list of items to display a solver's configurations in jsTree
	 * @param configs A list of configurations for a solver
	 * @return A list of JSTreeItems that contains for properly formatted
	 * objects to display the configuration tree
	 */
	@SuppressWarnings("unused")
	protected static List<JSTreeItem> toConfigTree(List<Configuration> configs){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Configuration c : configs){
			JSTreeItem t;
			if(c.getName().length() > 3) {
				// If there are more than 3 letters in the name, extract the name
				t = new JSTreeItem(c.getName().substring(3), c.getId(), "leaf", "config");
			} else {
				// Or else set it as default
				t = new JSTreeItem("Default", c.getId(), "leaf", "config");
			}
			
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Represents a node in jsTree tree with certain attributes
	 * used for displaying the node and obtaining information about
	 * the node. Class member names should NOT be changed.
	 */
	@SuppressWarnings("unused")
	public static class JSTreeItem {		
		private String data;
		private JSTreeAttribute attr;
		private List<JSTreeItem> children;
		private String state;
				
		public JSTreeItem(String name, int id, String state, String type){
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
	 */
	@SuppressWarnings("unused")
	public static class JSTreeAttribute {
		private int id;		
		private String rel;
		
		public JSTreeAttribute(int id, String type){
			this.id = id;	
			this.rel = type;
		}			
	}
}
