package org.starexec.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;
import org.starexec.util.Strings;
import org.starexec.util.Util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.Expose;

/**
 * Holds all helper methods and classes for our restful web services
 */
public class RESTHelpers {
	private static final Logger log = Logger.getLogger(RESTHelpers.class);		
	private static Gson gson = new Gson();
	
	public enum Primitive {
		JOB, USER, SOLVER, BENCHMARK, SPACE
	}
	
    private static final String SEARCH_QUERY = "sSearch";
	private static final String SORT_DIRECTION = "sSortDir_0";
	private static final String SYNC_VALUE = "sEcho";
	private static final String SORT_COLUMN = "iSortCol_0";
	private static final String STARTING_RECORD = "iDisplayStart";
	private static final String RECORDS_PER_PAGE = "iDisplayLength";
	private static final String TOTAL_RECORDS = "iTotalRecords";
	private static final String TOTAL_RECORDS_AFTER_QUERY = "iTotalDisplayRecords";
	
	private static final int 	EMPTY = 0;
	public static final int ASCENDING = 0;
	public static final int DESCENDING = 1;

	
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
	 * Takes in a list of worker nodes and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param nodes The list of worker nodes to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toNodeList(List<WorkerNode> nodes){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(WorkerNode n : nodes){
			// Only take the first part of the host name, the full one is too int to display on the client
			JSTreeItem t = new JSTreeItem(n.getName().split("\\.")[0], n.getId(), "leaf", n.getStatus().equals("ACTIVE") ? "enabled_node" : "disabled_node");	
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Takes in a list of queues and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param queues The list of queues to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toQueueList(List<Queue> queues){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Queue q : queues){
			JSTreeItem t = new JSTreeItem(q.getName(), q.getId(), "closed", "queue");	
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Takes in a list of spaces (communities) and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param communities The list of communities to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toCommunityList(List<Space> communities){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		for(Space space: communities){
			JSTreeItem t = new JSTreeItem(space.getName(), space.getId(), "leaf", "space");	
			list.add(t);
		}

		return list;
	}
	
	/**
	 * Represents a node in jsTree tree with certain attributes
	 * used for displaying the node and obtaining information about the node.
	 * @author Tyler Jensen
	 */	
	@SuppressWarnings("unused")
	protected static class JSTreeItem {		
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
	 * that it can be passed aint to other ajax methods.
	 * @author Tyler Jensen
	 */	
	@SuppressWarnings("unused")
	protected static class JSTreeAttribute {
		private int id;		
		private String rel;
		
		public JSTreeAttribute(int id, String type){
			this.id = id;	
			this.rel = type;
		}			
	}
	
	/**
	 * Represents a space and a user's permission for that space.  This is purely a helper
	 * class so we can easily read the information via javascript on the client.
	 * @author Tyler Jensen & Todd Elvers
	 */
	@SuppressWarnings("unused")
	protected static class SpacePermPair {
		@Expose private Space space;
		@Expose private Permission perm;
		
		public SpacePermPair(Space s, Permission p) {
			this.space = s;
			this.perm = p;
		}
	}
	
	/**
	 * Represents community details including the requesting user's permissions
	 * for the community aint with the community's leaders.
	 * Permissions are used so the client side can determine what actions a user can take on the community
	 * @author Tyler Jensen
	 */
	@SuppressWarnings("unused")
	protected static class CommunityDetails {		
		@Expose private Space space;
		@Expose private Permission perm;
		@Expose private List<User> leaders;
		@Expose private List<Website> websites;
		
		public CommunityDetails(Space s, Permission p, List<User> leaders, List<Website> websites) {
			this.space = s;
			this.perm = p;
			this.leaders = leaders;
			this.websites = websites;
		}
	}
	
	
	/**
	 * Validate the parameters of a request for a DataTable page
	 *
	 * @param type the primitive type being queried for
	 * @param request the object containing the parameters to validate
	 * @return an attribute map containing the valid parameters parsed from the request object,<br>
	 * 		or null if parameter validation fails
	 * @author Todd Elvers
	 */
	private static HashMap<String, Integer> getAttrMap(Primitive type, HttpServletRequest request){
		HashMap<String, Integer> attrMap = new HashMap<String, Integer>();
		
		try{
			// Parameters from the DataTable object
		    String iDisplayStart = (String) request.getParameter(STARTING_RECORD);	// Represents the record number the current page starts at (0 for page 1, 10 for page 2, 
		    String iDisplayLength = (String) request.getParameter(RECORDS_PER_PAGE);// Represents the number of records in a page (default = 10 records per page)
		    String sEcho = (String) request.getParameter(SYNC_VALUE);				// Unique number used to keep the client-server interaction synchronized
		    String iSortCol = (String) request.getParameter(SORT_COLUMN);			// Given an array of the column names, this is an index to which column is being used to sort
		    String sDir = (String) request.getParameter(SORT_DIRECTION);			// Represents the sorting direction ('asc' for ascending or 'desc' for descending)
		    String sSearch = (String) request.getParameter(SEARCH_QUERY);			// Represents the filter/search query (if no filter/search query is provided, this is empty)
		    
		    // Ensures the starting entry exists and is non-negative
	    	if(Util.isNullOrEmpty(iDisplayStart)) {
	    		return null;
	    	} else {
	    		int startingEntry = Integer.parseInt(iDisplayStart);
	    		attrMap.put(STARTING_RECORD, startingEntry);
	    		if (startingEntry < 0) {
	    			return null;
	    		}
	    	}
	    	
	    	// Ensures the number of entries per page is specified and is between 10 and 100
	    	if (Util.isNullOrEmpty(iDisplayLength)) {
	    		return null;
	    	} else {
	    		int entriesPerPage = Integer.parseInt(iDisplayLength);
	    		attrMap.put(RECORDS_PER_PAGE, entriesPerPage);
	    		if (entriesPerPage < 10 || entriesPerPage > 100) {
	    			return null;
	    		}
	    	}

	    	// Ensures the sync variable is an integer
	    	if (Util.isNullOrEmpty(sEcho)) {
	    		return null;
	    	} else {
	    		attrMap.put(SYNC_VALUE, Integer.parseInt(sEcho));
	    	}
	    	
	    	// Ensures the columns to sort on are specified and valid
	    	if (Util.isNullOrEmpty(iSortCol)) {
	    		// Allow jobs datatable to have a sort column null, then set
	    		// the column to sort by to column 5, which doesn't exist on
	    		// the screen but represents the creation date
	    		if(type == Primitive.JOB){
	    			attrMap.put(SORT_COLUMN, 5);
	    		} else {
	    			return null;
	    		}
	    	} else {
	    		int sortColumnIndex = Integer.parseInt(iSortCol);
	    		attrMap.put(SORT_COLUMN, sortColumnIndex);
	    		switch(type){
		    		case JOB:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 4) {
			    			return null;
			    		}
		    			break;
		    		case USER:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 3) {
			    			return null;
			    		}
		    			break;
		    		case SOLVER:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) {
			    			return null;
			    		}
		    			break;
		    		case BENCHMARK:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) {
			    			return null;
			    		}
		    			break;
		    		case SPACE:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) {
			    			return null;
			    		}
		    			break;
	    		}
	    		
	    	}
	    	
	    	// Ensures the sort direction is specified and valid
	    	if (Util.isNullOrEmpty(sDir)) {
	    		// Only permit the jobs table to have a null sorting direction;
	    		// this allows for jobs to be sorted initially on their creation date
	    		if(type == Primitive.JOB){
	    			attrMap.put(SORT_DIRECTION, DESCENDING);
	    		} else {
	    			return null;
	    		}
	    	} else {
	    		if(sDir.contains("asc") || sDir.contains("desc")){
	    			attrMap.put(SORT_DIRECTION, (sDir.equals("asc") ? ASCENDING	: DESCENDING));
	    		} else {
	    			return null;
	    		}
	    	}
	    	
	    	// Depending on if the search/filter is empty or not, this will be 0 or 1
	    	if (Util.isNullOrEmpty(sSearch)) {
	    		attrMap.put(SEARCH_QUERY, 0);
	    	} else {
	    		attrMap.put(SEARCH_QUERY, 1);
	    	}
	    	
	    	// Add the last two parameters, which will be set later, to the attribute map
	    	attrMap.put(TOTAL_RECORDS, 0);
	    	attrMap.put(TOTAL_RECORDS_AFTER_QUERY, 0);
	    	
	    	return attrMap;
	    } catch(Exception e){
	    	log.error(e.getMessage(), e);
	    }
	    
	    return null;
	}
	
	
	/**
	 * Returns the HTML representing a job pair's status
	 *
	 * @param statType 'asc' or 'desc'
	 * @param numerator a job pair's completePairs, pendingPairs, or errorPairs variable
	 * @param denominator a job pair's totalPairs variable
	 * @return HTML representing a job pair's status
	 * @author Todd Elvers
	 */
	private static String getPairStatHtml(String statType, int numerator, int denominator){
		StringBuilder sb = new StringBuilder();
		sb.append("<p class=\"stat ");
		sb.append(statType);
		sb.append("\">");
		sb.append(numerator);
		sb.append("/");
		sb.append(denominator);
		sb.append("</p>");
		return sb.toString();
	}
	
	/**
	 * Gets the next page of entries for a DataTable object
	 *
	 * @param type the kind of primitives to query for
	 * @param spaceId the id of the space to get the primitives from
	 * @param request the object containing all the DataTable parameters
	 * @return a JSON object representing the next page of primitives to return to the client,<br>
	 * 		or null if the parameters of the request fail validation
	 * @author Todd Elvers
	 */
	protected static JsonObject getNextDataTablesPage(Primitive type, int spaceId, HttpServletRequest request){
		
		// Parameter validation
	    HashMap<String, Integer> attrMap = RESTHelpers.getAttrMap(type, request);
	    if(null == attrMap){
	    	log.debug("Returning null...");
	    	return null;
	    }
	    
	    JsonObject nextPage = new JsonObject();		// JSON object representing next page for client's DataTable object
	    JsonArray dataTablePageEntries = null;		// JSON array containing the DataTable primitive entries
	    
    	int currentUserId = SessionUtil.getUserId(request);
    	
	    switch(type){
	    
		    case JOB:
	    		List<Job> jobsToDisplay = new LinkedList<Job>();
	    		int totalJobsInSpace = Jobs.getCountInSpace(spaceId);
	    		
	    		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
	    		jobsToDisplay = Jobs.getJobsForNextPage(
	    				attrMap.get(STARTING_RECORD),					// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 					// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == 0 ? true : false,// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 						// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 			// Search query
	    				spaceId											// Parent space id 
				);
	    		
	    		
	    		/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalJobsInSpace);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, jobsToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalJobsInSpace);

			    
			    
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(Job job : jobsToDisplay){
		    		StringBuilder sb = new StringBuilder();
					String hiddenJobId;
					
					// Create the hidden input tag containing the job id
					sb.append("<input type=\"hidden\" value=\"");
					sb.append(job.getId());
					sb.append("\" prim=\"job\"/>");
					hiddenJobId = sb.toString();
		    		
		    		// Create the job "details" link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a href=\"/starexec/secure/details/job.jsp?id=");
		    		sb.append(job.getId());
		    		sb.append("\" target=\"blank\">");
		    		sb.append(job.getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenJobId);
					String jobLink = sb.toString();
					
					String status = job.getLiteJobPairStats().get("pendingPairs") > 0 ? "incomplete" : "complete";
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(jobLink));
		    		entry.add(new JsonPrimitive(status));
		    		entry.add(new JsonPrimitive(getPairStatHtml("asc", job.getLiteJobPairStats().get("completePairs"), job.getLiteJobPairStats().get("totalPairs"))));
		    		entry.add(new JsonPrimitive(getPairStatHtml("desc", job.getLiteJobPairStats().get("pendingPairs"), job.getLiteJobPairStats().get("totalPairs"))));
		    		entry.add(new JsonPrimitive(getPairStatHtml("desc", job.getLiteJobPairStats().get("errorPairs"), job.getLiteJobPairStats().get("totalPairs"))));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    	
		    	
		    case USER:
	    		List<User> usersToDisplay = new LinkedList<User>();
	    		int totalUsersInSpace = Users.getCountInSpace(spaceId);
	    		
	    		// Retrieves the relevant User objects to use in constructing the JSON to send to the client
	    		usersToDisplay = Users.getUsersForNextPage(
	    				attrMap.get(STARTING_RECORD),					// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 					// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == 0 ? true : false,// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 						// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 			// Search query
	    				spaceId											// Parent space id 
				);
	    		
	    		
	    		/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalUsersInSpace);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, usersToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalUsersInSpace);
		    	
	    		
		    	
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(User user : usersToDisplay){
		    		StringBuilder sb = new StringBuilder();
					String hiddenUserId;
					
					// Create the hidden input tag containing the user id
					if(user.getId() == currentUserId) {
						sb.append("<input type=\"hidden\" value=\"");
						sb.append(user.getId());
						sb.append("\" name=\"currentUser\" id=\"uid"+user.getId()+"\" prim=\"user\"/>");
						hiddenUserId = sb.toString();
					} else {
						sb.append("<input type=\"hidden\" value=\"");
						sb.append(user.getId());
						sb.append("\" id=\"uid"+user.getId()+"\" prim=\"user\"/>");
						hiddenUserId = sb.toString();
					}
		    		
		    		// Create the user "details" link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a href=\"/starexec/secure/details/user.jsp?id=");
		    		sb.append(user.getId());
		    		sb.append("\" target=\"blank\">");
		    		sb.append(user.getFullName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenUserId);
					String userLink = sb.toString();
					
					sb = new StringBuilder();
					sb.append("<a href=\"mailto:");
					sb.append(user.getEmail());
					sb.append("\">");
					sb.append(user.getEmail());
					sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
					String emailLink = sb.toString();
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(userLink));
		    		entry.add(new JsonPrimitive(user.getInstitution()));
		    		entry.add(new JsonPrimitive(emailLink));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    	
		    	
		    case SOLVER:
	    		List<Solver> solversToDisplay = new LinkedList<Solver>();
	    		int totalSolversInSpace =  Solvers.getCountInSpace(spaceId);
	    		
	    		// Retrieves the relevant Solver objects to use in constructing the JSON to send to the client
	    		solversToDisplay = Solvers.getSolversForNextPage(
	    				attrMap.get(STARTING_RECORD),					// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 					// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == 0 ? true : false,// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 						// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 			// Search query
	    				spaceId											// Parent space id 
				);
	    		
	    		
	    		/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalSolversInSpace);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, solversToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalSolversInSpace);
		    	
			    
			    
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(Solver solver : solversToDisplay){
		    		StringBuilder sb = new StringBuilder();
		    		
		    		// Create the hidden input tag containing the solver id
		    		sb.append("<input type=\"hidden\" value=\"");
		    		sb.append(solver.getId());
		    		sb.append("\" prim=\"solver\" />");
		    		String hiddenSolverId = sb.toString();
		    		
		    		// Create the solver "details" link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a href=\"/starexec/secure/details/solver.jsp?id=");
		    		sb.append(solver.getId());
		    		sb.append("\" target=\"blank\">");
		    		sb.append(solver.getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenSolverId);
					String solverLink = sb.toString();
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(solverLink));
		    		entry.add(new JsonPrimitive(solver.getDescription()));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    	
		    	
		    case BENCHMARK:
		    	List<Benchmark> benchmarksToDisplay = new LinkedList<Benchmark>();
		    	int totalBenchmarksInSpace = Benchmarks.getCountInSpace(spaceId);
		    	
		    	// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		    	benchmarksToDisplay = Benchmarks.getBenchmarksForNextPage(
	    				attrMap.get(STARTING_RECORD),					// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 					// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == 0 ? true : false,// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 						// Column sorted on
	    				request.getParameter(SEARCH_QUERY),			 	// Search query
	    				spaceId											// Parent space id 
				);
		    	
		    	
		    	/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalBenchmarksInSpace);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, benchmarksToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalBenchmarksInSpace);
			    
			    
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(Benchmark bench : benchmarksToDisplay){
		    		StringBuilder sb = new StringBuilder();
		    		
		    		// Create the hidden input tag containing the benchmark id
		    		sb.append("<input type=\"hidden\" value=\"");
		    		sb.append(bench.getId());
		    		sb.append("\" prim=\"benchmark\"/>");
		    		String hiddenBenchId = sb.toString();
		    		
		    		// Create the benchmark "details" link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		// Set the tooltip to be the benchmark's description
		    		sb.append(bench.getDescription());
		    		sb.append("\" href=\"/starexec/secure/details/benchmark.jsp?id=");
		    		sb.append(bench.getId());
		    		sb.append("\" target=\"blank\">");
		    		sb.append(bench.getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenBenchId);
					String benchLink = sb.toString();
					
					// Create the benchmark type tag
					sb = new StringBuilder();
					sb.append("<span title=\"");
					// Set the tooltip to be the benchmark type's description
					sb.append(bench.getType().getDescription());
					sb.append("\">");
					sb.append(bench.getType().getName());
					sb.append("</span>");
					String typeSpan = sb.toString();
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(benchLink));
		    		entry.add(new JsonPrimitive(typeSpan));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    	
		    	
		    case SPACE:
		    	List<Space> spacesToDisplay = new LinkedList<Space>();

	    		int totalSubspacesInSpace = Spaces.getCountInSpace(spaceId);
		    	
		    	// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		    	spacesToDisplay = Spaces.getSpacesForNextPage(
	    				attrMap.get(STARTING_RECORD),					// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 					// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == 0 ? true : false,// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 						// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 			// Search query
	    				spaceId											// Parent space id 
				);
		    	
		    	
		    	/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalSubspacesInSpace);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, spacesToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalSubspacesInSpace);
		    	
		    	
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(Space space : spacesToDisplay){
		    		StringBuilder sb = new StringBuilder();
					String hiddenSpaceId;
					
					// Create the hidden input tag containing the space id
					sb.append("<input type=\"hidden\" value=\"");
					sb.append(space.getId());
					sb.append("\" prim=\"space\" />");
					hiddenSpaceId = sb.toString();
		    		
					// Create the space "details" link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a class=\"spaceLink\" onclick=\"openSpace(");
		    		sb.append(spaceId);
		    		sb.append(",");
		    		sb.append(space.getId());
		    		sb.append(")\">");
		    		sb.append(space.getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenSpaceId);
					String spaceLink = sb.toString();
					
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(spaceLink));
		    		entry.add(new JsonPrimitive(space.getDescription()));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
	    }
	    
	    // Build the actual JSON response object and populated it with the created data
	    nextPage.addProperty(SYNC_VALUE, attrMap.get(SYNC_VALUE));
	    nextPage.addProperty(TOTAL_RECORDS, attrMap.get(TOTAL_RECORDS));
	    nextPage.addProperty(TOTAL_RECORDS_AFTER_QUERY, attrMap.get(TOTAL_RECORDS_AFTER_QUERY));
	    nextPage.add("aaData", dataTablePageEntries);
	    
	    // Return the next DataTable page
    	return nextPage;
	}
}

