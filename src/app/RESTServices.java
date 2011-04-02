package app;
import javax.ws.rs.*;

@Path("")
public class RESTServices {
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
}
