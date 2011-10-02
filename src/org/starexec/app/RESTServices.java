package org.starexec.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.log4j.Logger;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Class which handles all RESTful web service requests.
 * 
 * @author Tyler Jensen
 * @deprecated This file is out of date and is included for future reference
 */
@Path("")
public class RESTServices {	
	private static final Logger log = Logger.getLogger(RESTServices.class);			
	
	public RESTServices(){
		
	}
	
	@GET
	@Path("/solvers")
	@Produces("application/json")
	public String getAllSolvers(@QueryParam("id") int id) {		
		throw new NotImplementedException();
	}
}
