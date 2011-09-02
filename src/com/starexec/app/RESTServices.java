package com.starexec.app;
import javax.ws.rs.*;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.starexec.data.Databases;


@Path("")
/**
 * Class which handles all RESTful web service requests.
 */
public class RESTServices {
	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	
	public RESTServices(){
		
	}
	
	@GET
	@Path("/levels/sublevels")
	@Produces("application/json")
	public String getSubLevels(@QueryParam("id") int id) {		
		return new Gson().toJson(
				RESTHelpers.toLevelTree(
				Databases.next().getSubLevels(id)));				
	}
	
	@GET
	@Path("/solvers")
	@Produces("application/json")
	public String getAllSolvers(@QueryParam("id") int id) {		
		if(id < 0) {
			return new Gson().toJson(
					RESTHelpers.toSolverTree(
					Databases.next().getSolvers(null)));
		} else {
			return new Gson().toJson(
					RESTHelpers.toConfigTree(
					Databases.next().getConfigurations(id)));
		}
	}
	
	@GET
	@Path("/benchmarks/all")
	@Produces("application/json")
	public String getAllBenchmarks() {	
		return new Gson().toJson(Databases.next().getBenchmarks(null));						
	}
	
	@GET
	@Path("/level/bench")
	@Produces("application/json")	
	public String getLevelsBench(@QueryParam("id") int id) {					
		return new Gson().toJson(
				RESTHelpers.toLevelBenchTree(
				Databases.next().getSubLevelsWithBench(id)));				
	}
		
	@GET
	@POST
	@Path("/jobs/all")
	@Produces("application/json")	
	public String getJobs() {			
		return new Gson().toJson(
				RESTHelpers.jobToTableRow(
				Databases.next().getJobs()));				
	}
	
	@GET
	@POST
	@Path("/jobs/pairs/{id}")
	@Produces("application/json")	
	public String getJobPairs(@PathParam("id") int id) {		
		return new Gson().toJson(
				RESTHelpers.pairToTableRow(
				Databases.next().getJobPairs(id)));				
	}	
}
