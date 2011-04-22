package com.starexec.app;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.*;

import com.google.gson.Gson;
import com.starexec.data.Database;
import com.starexec.data.to.Benchmark;
import com.starexec.data.to.Level;
import com.starexec.data.to.Solver;


@Path("")
public class RESTServices {
	protected Database database;
	
	public RESTServices(){
		database = new Database();
	}
	 
	@GET
	@Path("/levels/root")
	@Produces("application/json")
	public String getRoots() {		
		return new Gson().toJson(database.getRootLevels());				
	}
	
	@GET
	@Path("/levels/sublevels/{id}")
	@Produces("application/json")
	public String getSubLevels(@PathParam("id") int id) {		
		return new Gson().toJson(database.getSubLevels(id));				
	}
	
	@GET
	@Path("/solvers/all")
	@Produces("application/json")
	public String getAllSolvers() {		
		return new Gson().toJson(toSolverTree(database.getSolvers(null)));				
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
		List<Level> levels = database.getSubLevelsWithBench(id);					
		return new Gson().toJson(toLevelBenchTree(levels));				
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
	
	private List<JSTreeItem> toSolverTree(List<Solver> solvers){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Solver solver : solvers){
			JSTreeItem t = new JSTreeItem(solver.getName(), solver.getId(), "leaf", "solver");			
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
