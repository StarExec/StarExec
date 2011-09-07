package com.starexec.app;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.starexec.data.*;
import com.starexec.data.to.*;
import com.starexec.data.Databases;


@Path("")
/**
 * Class which handles all RESTful web service requests.
 */
public class RESTServices {
	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	
	public RESTServices(){
		
	}
	
	// TODO: None of these web services will work, need to redefine "communities"
	// and "spaces" and some other fundamental things before refactoring.
	
	@GET
	@Path("/levels/sublevels")
	@Produces("application/json")
	public String getSubLevels(@QueryParam("id") int id) {		
		if(id < 0) {						
			return new Gson().toJson(
					RESTHelpers.toLevelTree(
					Databases.next().getRootLevels(0)));
		} else {
			return new Gson().toJson(
					RESTHelpers.toLevelTree(
					Databases.next().getSubLevels(id)));				
		}
	}
	
	@GET
	@Path("/solvers")
	@Produces("application/json")
	public String getAllSolvers(@QueryParam("id") int id) {		
		if(id < 0) {
			return new Gson().toJson(
					RESTHelpers.toSolverTree(
					Databases.next().getAllSolvers(0)));
		} else {
			return new Gson().toJson(
					RESTHelpers.toConfigTree(
					Databases.next().getConfigurations(id)));
		}
	}
	
	@GET
	@Path("/level/bench")
	@Produces("application/json")	
	public String getLevelsBench(@QueryParam("id") int id) {	
		if(id < 0){
			return new Gson().toJson(
					RESTHelpers.toLevelBenchTree(
					Databases.next().getRootLevels(0)));
		} else {
			return new Gson().toJson(
					RESTHelpers.toLevelBenchTree(
					Databases.next().getSubLevelsWithBench(id)));
		}
	}
		
	@GET
	@POST
	@Path("/jobs/all")
	@Produces("application/json")	
	public String getJobs(@Context HttpServletRequest request) {			
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
