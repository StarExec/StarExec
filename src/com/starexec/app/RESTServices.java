package com.starexec.app;
import javax.ws.rs.*;

import com.google.gson.Gson;
import com.starexec.data.Database;


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
		Gson gson = new Gson();
		String jsonStr = gson.toJson(database.getRootLevels());		
		return jsonStr;
	}
	
	@GET
	@Path("/levels/sublevels/{id}")
	@Produces("application/json")
	public String getSubLevels(@PathParam("id") String id) {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(database.getSubLevels(Integer.parseInt(id)));		
		return jsonStr;
	}
	
	@GET
	@Path("/solvers/all")
	@Produces("application/json")
	public String getAllSolvers() {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(database.getSolvers(null));		
		return jsonStr;
	}
	
	@GET
	@Path("/benchmarks/all")
	@Produces("application/json")
	public String getAllBenchmarks() {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(database.getBenchmarks(null));				
		return jsonStr;
	}
}
