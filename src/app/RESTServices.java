package app;
import javax.ws.rs.*;

import com.google.gson.Gson;

import data.Database;

@Path("")
public class RESTServices {
	protected Database database;
	
	public RESTServices(){
		database = new Database();
	}
	
	@GET
	@Path("/hello")
	@Produces("text/plain")
	public String hello(){
		return "Hello World";    
	}
	 
	@GET
	@Path("/echo/{command}")
	@Produces("text/plain")
	public String getBook(@PathParam("command") String input) {
		return "Echo: " + input;
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
}
