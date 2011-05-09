package com.starexec.app;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.*;

import com.google.gson.Gson;
import com.starexec.data.Database;
import com.starexec.data.to.Benchmark;
import com.starexec.data.to.Configuration;
import com.starexec.data.to.Job;
import com.starexec.data.to.JobPair;
import com.starexec.data.to.Level;
import com.starexec.data.to.Solver;


@Path("")
public class RESTServices {
	protected Database database;
	protected static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd, yyyy h:mm a");	
	
	public RESTServices(){
		database = new Database();
	}
	
	@GET
	@Path("/levels/sublevels")
	@Produces("application/json")
	public String getSubLevels(@QueryParam("id") int id) {		
		return new Gson().toJson(toLevelTree(database.getSubLevels(id)));				
	}
	
	@GET
	@Path("/solvers")
	@Produces("application/json")
	public String getAllSolvers(@QueryParam("id") int id) {		
		if(id < 0)
			return new Gson().toJson(toSolverTree(database.getSolvers(null)));
		else
			return new Gson().toJson(toConfigTree(database.getConfigurations(id)));
	}
	
	@GET
	@Path("/benchmarks/all")
	@Produces("application/json")
	public String getAllBenchmarks() {		
		return new Gson().toJson(database.getBenchmarks(null));						
	}
	
	@GET
	@Path("/level/bench")
	@Produces("application/json")	
	public String getLevelsBench(@QueryParam("id") int id) {					
		return new Gson().toJson(toLevelBenchTree(database.getSubLevelsWithBench(id)));				
	}
		
	@GET
	@POST
	@Path("/jobs/all")
	@Produces("application/json")	
	public String getJobs() {			
		return new Gson().toJson(jobToTableRow(database.getJobs()));				
	}
	
	@GET
	@POST
	@Path("/jobs/pairs/{id}")
	@Produces("application/json")	
	public String getJobPairs(@PathParam("id") int id) {		
		return new Gson().toJson(pairToTableRow(database.getJobPairs(id)));				
	}
	
	public TableRow pairToTableRow(List<JobPair> jList){
		TableRow tr = new TableRow(1, jList.size());
		for(JobPair jp : jList){
			Row r = new Row(jp.getId());
			r.addCell(jp.getId());
			r.addCell(jp.getStatus());
			r.addCell(jp.getResult());
			r.addCell(jp.getSolver().getName());
			
			if(jp.getConfig().getName().length() > 3)
				r.addCell(jp.getConfig().getName().substring(3));
			else
				r.addCell("Default");
			
			r.addCell(jp.getBenchmark().getFileName());		
			r.addCell(jp.getRunTime());
			
			if(jp.getStartTime() != null)
				r.addCell(dateFormat.format(jp.getStartTime()));
			else
				r.addCell("");
			
			if(jp.getEndTime() != null)
				r.addCell(dateFormat.format(jp.getEndTime()));
			else
				r.addCell("");
			
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
	public TableRow jobToTableRow(List<Job> jList){
		TableRow tr = new TableRow(1, jList.size());
		for(Job j : jList){
			Row r = new Row(j.getJobId());
			r.addCell(j.getJobId());
			r.addCell(j.getStatus());
			r.addCell(j.getRunTime());
			
			if(j.getSubmitted() != null)
				r.addCell(dateFormat.format(j.getSubmitted()));
			else
				r.addCell("");
			
			if(j.getCompleted() != null)
				r.addCell(dateFormat.format(j.getCompleted()));
			else
				r.addCell("");
			
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
	 * Represents a row (collection of cells with an id) in the jquery flexigrid	 
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
	private List<JSTreeItem> toLevelBenchTree(List<Level> levels){
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
	private List<JSTreeItem> toLevelTree(List<Level> levels){
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
	private List<JSTreeItem> toSolverTree(List<Solver> solvers){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Solver solver : solvers){
			JSTreeItem t = new JSTreeItem(solver.getName(), solver.getId(), "closed", "solver");			
			list.add(t);
		}

		return list;
	}
	
	private List<JSTreeItem> toConfigTree(List<Configuration> configs){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Configuration c : configs){
			JSTreeItem t;
			if(c.getName().length() > 3)
				t = new JSTreeItem(c.getName().substring(3), c.getId(), "leaf", "config");
			else
				t = new JSTreeItem("Default", c.getId(), "leaf", "config");
						
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Represents a node in jsTree tree with certain attributes
	 * used for displaying the node and obtaining information about
	 * the node.
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
