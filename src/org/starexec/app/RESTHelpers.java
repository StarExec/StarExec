package org.starexec.app;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Statistics;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobSolver;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.Website;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;
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

	// Job pairs aren't technically a primitive class according to how 
	// we've discussed primitives, but to save time and energy I've included them here as such
	public enum Primitive {
		JOB, USER, SOLVER, BENCHMARK, SPACE, JOB_PAIR, JOB_STATS
	}
	
    private static final String SEARCH_QUERY = "sSearch";
	private static final String SORT_DIRECTION = "sSortDir_0";
	private static final String SYNC_VALUE = "sEcho";
	private static final String SORT_COLUMN = "iSortCol_0";
	private static final String STARTING_RECORD = "iDisplayStart";
	private static final String RECORDS_PER_PAGE = "iDisplayLength";
	private static final String TOTAL_RECORDS = "iTotalRecords";
	private static final String TOTAL_RECORDS_AFTER_QUERY = "iTotalDisplayRecords";
	
	private static final int EMPTY = 0;
	private static final int   ASC = 0;
	private static final int  DESC = 1;

	
	/**
	 * Takes in a list of spaces and converts it into
	 * a list of JSTreeItems suitable for being displayed
	 * on the client side with the jsTree plugin.
	 * @param spaces The list of spaces to convert
	 * @return List of JSTreeItems to be serialized and sent to client
	 * @author Tyler Jensen
	 */
	protected static List<JSTreeItem> toSpaceTree(List<Space> spaceList, int userID){
		List<JSTreeItem> list = new LinkedList<JSTreeItem>();
		
		
		for(Space space: spaceList){
			JSTreeItem t;
			
			if (Spaces.getSubSpaces(space.getId(), userID, false).size()>0) {
				 t = new JSTreeItem(space.getName(), space.getId(), "closed", "space");	
			} else {
				t = new JSTreeItem(space.getName(), space.getId(), "leaf", "space");	
			}
			
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
		JSTreeItem t;
		for(Queue q : queues){
			if (Queues.getNodes(q.getId()).size()>0) {
				t = new JSTreeItem(q.getName(), q.getId(), "closed", "queue");	
			} else {
				t = new JSTreeItem(q.getName(), q.getId(), "leaf", "queue");	
			}
			
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
		    String iDisplayStart = (String) request.getParameter(STARTING_RECORD);	// Represents the record number the current page starts at (0 for page 1, 10 for page 2, etc.) 
		    String iDisplayLength = (String) request.getParameter(RECORDS_PER_PAGE);// Represents the number of records in a page (default = 10 records per page)
		    String sEcho = (String) request.getParameter(SYNC_VALUE);				// Unique number used to keep the client-server interaction synchronized
		    String iSortCol = (String) request.getParameter(SORT_COLUMN);			// Given an array of the column names, this is an index to which column is being used to sort
		    String sDir = (String) request.getParameter(SORT_DIRECTION);			// Represents the sorting direction ('asc' for ascending or 'desc' for descending)
		    String sSearch = (String) request.getParameter(SEARCH_QUERY);			// Represents the filter/search query (if no filter/search query is provided, this is empty)
		    
		    // Validates the starting record, the number of records per page, and the sync value
		    if(Util.isNullOrEmpty(iDisplayStart)
		    		||	Util.isNullOrEmpty(iDisplayLength)
		    		||	Util.isNullOrEmpty(sEcho)
		    		||	Integer.parseInt(iDisplayStart) < 0
		    		||	Integer.parseInt(iDisplayLength) < 10
		    		||	Integer.parseInt(iDisplayLength) > 100
		    		||	Integer.parseInt(sEcho) < 0) {
		    	return null;
		    }
	    	
	    	// Validates that the columns to sort on are specified and valid
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
		    			if (sortColumnIndex < 0 || sortColumnIndex > 5) return null;
		    			break;
		    		case JOB_PAIR:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 6) return null;
		    			break;
		    		case JOB_STATS:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 5) return null;
		    		case USER:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 3) return null;
		    			break;
		    		case SOLVER:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) return null;
		    			break;
		    		case BENCHMARK:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) return null;
		    			break;
		    		case SPACE:
		    			if (sortColumnIndex < 0 || sortColumnIndex > 2) return null;
		    			break;
	    		}
	    	}
	    	
	    	// Validates that the sort direction is specified and valid
	    	if (Util.isNullOrEmpty(sDir)) {
	    		// Only permit the jobs table to have a null sorting direction;
	    		// this allows for jobs to be sorted initially on their creation date
	    		if(type == Primitive.JOB){
	    			attrMap.put(SORT_DIRECTION, DESC);
	    		} else {
	    			return null;
	    		}
	    	} else {
	    		if(sDir.contains("asc") || sDir.contains("desc")){
	    			attrMap.put(SORT_DIRECTION, (sDir.equals("asc") ? ASC : DESC));
	    		} else {
	    			return null;
	    		}
	    	}
	    	
	    	// Depending on if the search/filter is empty or not, this will be 0 or 1
	    	if (Util.isNullOrEmpty(sSearch)) {
	    		attrMap.put(SEARCH_QUERY, EMPTY);
	    	} else {
	    		attrMap.put(SEARCH_QUERY, 1);
	    	}

	    	// The request is valid if it makes it this far;
	    	// Finish the validation by adding the remaining attributes to the map
	    	attrMap.put(RECORDS_PER_PAGE, Integer.parseInt(iDisplayLength));
    		attrMap.put(STARTING_RECORD, Integer.parseInt(iDisplayStart));
    		attrMap.put(SYNC_VALUE, Integer.parseInt(sEcho));
	    	attrMap.put(TOTAL_RECORDS, EMPTY);
	    	attrMap.put(TOTAL_RECORDS_AFTER_QUERY, EMPTY);
	    	
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
	public static String getPairStatHtml(String statType, int numerator, int denominator){
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
	 * Returns the HTML representing a job pair's status
	 *
	 * @param statType 'asc' or 'desc'
	 * @param numerator a job pair's completePairs, pendingPairs, or errorPairs variable
	 * @param denominator a job pair's totalPairs variable
	 * @return HTML representing a job pair's status
	 * @author Todd Elvers
	 */
	public static String getPercentStatHtml(String statType, int value, Boolean percentage){
		StringBuilder sb = new StringBuilder();
		sb.append("<p class=\"stat ");
		sb.append(statType);
		sb.append("\">");
		sb.append(value);
		if (percentage){
			sb.append(" %");
		}
		sb.append("</p>");
		return sb.toString();
	}
	/**
	 * Gets the next page of entries for a DataTable object
	 *
	 * @param type the kind of primitives to query for
	 * @param id either the id of the space to get the primitives from, or the id of the job
	 * to get job pairs for
	 * @param request the object containing all the DataTable parameters
	 * @return a JSON object representing the next page of primitives to return to the client,<br>
	 * 		or null if the parameters of the request fail validation
	 * @author Todd Elvers
	 */
	protected static JsonObject getNextDataTablesPage(Primitive type, int id, HttpServletRequest request){
		// Parameter validation
	    HashMap<String, Integer> attrMap = RESTHelpers.getAttrMap(type, request);
	    if(null == attrMap){
	    	return null;
	    }
	    
	    JsonObject nextPage = new JsonObject();		// JSON object representing next page for client's DataTable object
	    JsonArray dataTablePageEntries = null;		// JSON array containing the DataTable primitive entries
	    
    	int currentUserId = SessionUtil.getUserId(request);
    	
	    switch(type){
	    
		    case JOB:
	    		List<Job> jobsToDisplay = new LinkedList<Job>();
	    		int totalJobsInSpace = Jobs.getCountInSpace(id);
	    		
	    		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
	    		jobsToDisplay = Jobs.getJobsForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id													// Parent space id 
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
		    		entry.add(new JsonPrimitive(getPercentStatHtml("asc", job.getLiteJobPairStats().get("completionPercentage"), true)));
		    		entry.add(new JsonPrimitive(getPercentStatHtml("static", job.getLiteJobPairStats().get("totalPairs"), false)));
		    		entry.add(new JsonPrimitive(getPercentStatHtml("desc", job.getLiteJobPairStats().get("errorPercentage"), true)));
		    		entry.add(new JsonPrimitive(job.getCreateTime().toString()));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    	
		    	
		    case USER:
	    		List<User> usersToDisplay = new LinkedList<User>();
	    		int totalUsersInSpace = Users.getCountInSpace(id);
	    		if (Spaces.isPublicSpace(id)){
	    			totalUsersInSpace--;
	    		}
	    		// Retrieves the relevant User objects to use in constructing the JSON to send to the client
	    		usersToDisplay = Users.getUsersForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id													// Parent space id 
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
	    		int totalSolversInSpace =  Solvers.getCountInSpace(id);
	    		
	    		// Retrieves the relevant Solver objects to use in constructing the JSON to send to the client
	    		solversToDisplay = Solvers.getSolversForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id												// Parent space id 
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
		    	int totalBenchmarksInSpace = Benchmarks.getCountInSpace(id);
		    	
		    	// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		    	benchmarksToDisplay = Benchmarks.getBenchmarksForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY),			 		// Search query
	    				id												// Parent space id 
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
		    	
	    		int userId = SessionUtil.getUserId(request);
	    		int totalSubspacesInSpace = Spaces.getCountInSpace(id, userId);
	    		
		    	// Retrieves the relevant Benchmark objects to use in constructing the JSON to send to the client
		    	spacesToDisplay = Spaces.getSpacesForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id,											// Parent space id 
	    				userId												// Id of user making request
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
		    		sb.append(id);
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
		    	
		    case JOB_PAIR:
		    	List<JobPair> jobPairsToDisplay = new LinkedList<JobPair>();
	    		int totalJobPairsforJob = Jobs.getJobPairCount(id);
	    		
	    		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
	    		jobPairsToDisplay = Jobs.getJobPairsForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id													// Job id 
				);
	    		
	    		
	    		/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalJobPairsforJob);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, jobPairsToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalJobPairsforJob);

			    
			    
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(JobPair jp : jobPairsToDisplay){
		    		StringBuilder sb = new StringBuilder();
					String hiddenJobPairId;
					
					// Create the hidden input tag containing the jobpair id
					sb.append("<input type=\"hidden\" value=\"");
					sb.append(jp.getId());
					sb.append("\" name=\"pid\"/>");
					hiddenJobPairId = sb.toString();
		    		
		    		// Create the benchmark link and append the hidden input element
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		sb.append(jp.getBench().getDescription());
		    		sb.append("\" href=\"/starexec/secure/details/benchmark.jsp?id=");
		    		sb.append(jp.getBench().getId());
		    		sb.append("\">");
		    		sb.append(jp.getBench().getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
		    		sb.append(hiddenJobPairId);
					String benchLink = sb.toString();
					
					// Create the solver link
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		sb.append(jp.getSolver().getDescription());
		    		sb.append("\" href=\"/starexec/secure/details/solver.jsp?id=");
		    		sb.append(jp.getSolver().getId());
		    		sb.append("\">");
		    		sb.append(jp.getSolver().getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
					String solverLink = sb.toString();
					
					// Create the configuration link
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		sb.append(jp.getSolver().getConfigurations().get(0).getDescription());
		    		sb.append("\" href=\"/starexec/secure/details/configuration.jsp?id=");
		    		sb.append(jp.getSolver().getConfigurations().get(0).getId());
		    		sb.append("\">");
		    		sb.append(jp.getSolver().getConfigurations().get(0).getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
					String configLink = sb.toString();
					
					// Create the status field
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		sb.append(jp.getStatus().getDescription());
		    		sb.append("\">");
		    		sb.append(jp.getStatus().getStatus());
		    		sb.append("</a>");
					String status = sb.toString();
					
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(benchLink));
		    		entry.add(new JsonPrimitive(solverLink));
		    		entry.add(new JsonPrimitive(configLink));
		    		entry.add(new JsonPrimitive(status));
		    		double displayWC = Math.round(jp.getWallclockTime()*100)/100.0;		    		
		    		entry.add(new JsonPrimitive(displayWC + " s"));
		    		entry.add(new JsonPrimitive(jp.getStarexecResult()));
		    		entry.add(new JsonPrimitive(jp.getSpace().getName()));
		    		
		    		dataTablePageEntries.add(entry);
		    	}
		    	
		    	break;
		    
		    case JOB_STATS:
		    	List<JobSolver> jobSolversToDisplay = null;
	    		int totalJobPairs = Jobs.getJobPairCount(id);
	    		
	    		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
	    		jobSolversToDisplay = Jobs.getJobStatsForNextPage(
	    				attrMap.get(STARTING_RECORD),						// Record to start at  
	    				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
	    				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
	    				attrMap.get(SORT_COLUMN), 							// Column sorted on
	    				request.getParameter(SEARCH_QUERY), 				// Search query
	    				id													// Job id 
				);
	    		
	    		
	    		/**
		    	 * Used to display the 'total entries' information at the bottom of the DataTable;
		    	 * also indirectly controls whether or not the pagination buttons are toggle-able
		    	 */
		    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
		    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalJobPairs);
		    	} 
		    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
		    	else {
		    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, jobSolversToDisplay.size());
		    	}
			    attrMap.put(TOTAL_RECORDS, totalJobPairs);
			    
		    	/**
		    	 * Generate the HTML for the next DataTable page of entries
		    	 */
		    	dataTablePageEntries = new JsonArray();
		    	for(JobSolver js : jobSolversToDisplay){
		    		StringBuilder sb = new StringBuilder();
					String hiddenJobPairId;
					
					// Create the solver link
		    		sb = new StringBuilder();
		    		sb.append("<a title=\"");
		    		sb.append(js.getSolver().getName());
		    		sb.append("\" href=\"/starexec/secure/details/solver.jsp?id=");
		    		sb.append(js.getSolver().getId());
		    		sb.append("\">");
		    		sb.append(js.getSolver().getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
					String solverLink = sb.toString();
					
					sb= new StringBuilder();
					sb.append("<a title=\"");
		    		sb.append(js.getSolver().getConfigurations().get(0).getName());
		    		sb.append("\" href=\"/starexec/secure/details/configuration.jsp?id=");
		    		sb.append(js.getSolver().getConfigurations().get(0).getId());
		    		sb.append("\">");
		    		sb.append(js.getSolver().getConfigurations().get(0).getName());
		    		sb.append("<img class=\"extLink\" src=\"/starexec/images/external.png\"/></a>");
					String configLink = sb.toString();
					
					
					
					// Create an object, and inject the above HTML, to represent an entry in the DataTable
					JsonArray entry = new JsonArray();
		    		entry.add(new JsonPrimitive(solverLink));
		    		entry.add(new JsonPrimitive(configLink));
		    		entry.add(new JsonPrimitive(js.getCorrectJobPairs()));
		    		entry.add(new JsonPrimitive(js.getIncorrectJobPairs()));
		    		entry.add(new JsonPrimitive(js.getIncompleteJobPairs()));
		    		entry.add(new JsonPrimitive(js.getErrorJobPairs()));
		    		
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
	
	/**
	 * Copy a space into another space
	 * @param srcId The Id of the space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @return The Id of the new copy of the space.
	 * @author Ruoyu Zhang
	 */
	
	protected static int copySpace(int srcId, int desId, int usrId) {	
		if (srcId == desId){
			return 0;
		}
		
		Space sourceSpace = Spaces.getDetails(srcId, usrId);
		
		// Create a new space
		Space tempSpace = new Space();	
		tempSpace.setName(sourceSpace.getName());
		tempSpace.setDescription(sourceSpace.getDescription());
		tempSpace.setLocked(sourceSpace.isLocked());
		tempSpace.setBenchmarks(sourceSpace.getBenchmarks());
		tempSpace.setSolvers(sourceSpace.getSolvers());
		tempSpace.setJobs(sourceSpace.getJobs());
				
		// Make the default permissions for the space to be added
		Permission p = new Permission();
		p.setAddBenchmark(true);
		p.setAddJob(true);
		p.setAddSolver(true);
		p.setAddSpace(true);
		p.setAddUser(true);
		p.setLeader(true);
		p.setRemoveBench(true);
		p.setRemoveJob(true);
		p.setRemoveSolver(true);
		p.setRemoveSpace(true);
		p.setRemoveUser(true);
		
		// Set the default permission on the space
		tempSpace.setPermission(p);
		int newSpaceId = Spaces.add(tempSpace, desId, usrId);
		
		if(newSpaceId <= 0) {			
			// If it failed, notify an error
			return 0;
		}
		
		if(Permissions.canUserSeeSpace(srcId, usrId)){
			//Copying the references of benchmarks
			List<Benchmark> benchmarks = sourceSpace.getBenchmarks();
			List<Integer> benchmarkIds = new LinkedList<Integer>();
			int benchId = 0;
			for (Benchmark benchmark: benchmarks){
				benchId = benchmark.getId();
				if(Permissions.canUserSeeBench(benchId, usrId)){
					benchmarkIds.add(benchId);
				}
			}
			Benchmarks.associate(benchmarkIds, newSpaceId);		
			
			//Copying the references of solvers
			List<Solver> solvers = sourceSpace.getSolvers();
			List<Integer> solverIds = new LinkedList<Integer>();
			int solverId = 0;
			for (Solver solver: solvers) {
				solverId = solver.getId();
				if(Permissions.canUserSeeSolver(solverId, usrId)){
					solverIds.add(solverId);
				}
			}
			Solvers.associate(solverIds, newSpaceId);
			
			//Copying the references of jobs
			List<Job> jobs = sourceSpace.getJobs();
			List<Integer> jobIds = new LinkedList<Integer>();
			int jobId = 0;
			for (Job job : jobs){
				jobId = job.getId();
				if(Permissions.canUserSeeJob(jobId, usrId)){
					jobIds.add(jobId);
				}
			}
			Jobs.associate(jobIds, newSpaceId);
		}
		
		return newSpaceId;
	}
	
	/**
	 * Copy a hierarchy of the space into another space
	 * @param srcId The Id of the source space which is being copied.
	 * @param desId The Id of the destination space which is copied into.
	 * @param usrId The Id of the user doing the copy.
	 * @return The Id of the root space of the copied hierarchy.
	 * @author Ruoyu Zhang
	 */
	protected static int copyHierarchy(int srcId, int desId, int usrId) {
		if (srcId == desId){
			return 0;
		}
		
		int newSpaceId = copySpace(srcId, desId, usrId);
		if (newSpaceId == 0){
			return 0;
		}
		else {
			List<Space> subSpaces = Spaces.getSubSpaces(srcId, usrId, false);
			if (subSpaces == null){
				return newSpaceId;
			}
			else {
				for (Space space : subSpaces) {
					if (copyHierarchy(space.getId(), newSpaceId, usrId) == 0)
						return 0;
				}
				return newSpaceId;
			}
		}
	}
	
	/**
	 * Get the next page of jobs belongs to a specific user identified by usrId
	 * @param usrId Id of the user we are looking for
	 * @param request The http request
	 * @return JsonObject containing the result of the query
	 * @author Ruoyu Zhang
	 */
	protected static JsonObject getNextPageOfUserJobs(int usrId, HttpServletRequest request){
		// Parameter validation
	    HashMap<String, Integer> attrMap = RESTHelpers.getAttrMap(RESTHelpers.Primitive.JOB, request);
	    
	    if(null == attrMap){
	    	return null;
	    }
	    
	    JsonObject nextPage = new JsonObject();		// JSON object representing next page for client's DataTable object
	    JsonArray dataTablePageEntries = null;		// JSON array containing the DataTable primitive entries
	    
	    List<Job> jobsToDisplay = new LinkedList<Job>();
		int totalJobsCount = Jobs.getJobCountByUser(usrId);
		
		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
		jobsToDisplay = Jobs.getJobsByUserForNextPage(
				attrMap.get(STARTING_RECORD),						// Record to start at  
				attrMap.get(RECORDS_PER_PAGE), 						// Number of records to return
				attrMap.get(SORT_DIRECTION) == ASC ? true : false,	// Sort direction (true for ASC)
				attrMap.get(SORT_COLUMN), 							// Column sorted on
				request.getParameter(SEARCH_QUERY), 				// Search query
				usrId												// User id 
		);
		
		/**
    	 * Used to display the 'total entries' information at the bottom of the DataTable;
    	 * also indirectly controls whether or not the pagination buttons are toggle-able
    	 */
    	// If no search is provided, TOTAL_RECORDS_AFTER_QUERY = TOTAL_RECORDS
    	if(attrMap.get(SEARCH_QUERY) == EMPTY){
    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, totalJobsCount);
    	} 
    	// Otherwise, TOTAL_RECORDS_AFTER_QUERY < TOTAL_RECORDS 
    	else {
    		attrMap.put(TOTAL_RECORDS_AFTER_QUERY, jobsToDisplay.size());
    	}
	    attrMap.put(TOTAL_RECORDS, totalJobsCount);
	    
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
    		entry.add(new JsonPrimitive(job.getCreateTime().toString()));
    		
    		dataTablePageEntries.add(entry);
    	}
	    
	    // Build the actual JSON response object and populated it with the created data
	    nextPage.addProperty(SYNC_VALUE, attrMap.get(SYNC_VALUE));
	    nextPage.addProperty(TOTAL_RECORDS, attrMap.get(TOTAL_RECORDS));
	    nextPage.addProperty(TOTAL_RECORDS_AFTER_QUERY, attrMap.get(TOTAL_RECORDS_AFTER_QUERY));
	    nextPage.add("aaData", dataTablePageEntries);
	    
	    // Return the next DataTable page
    	return nextPage;
	}
	
	/**
	 * Get the result table for all the jobs in a space.
	 * @param spaceId The id of the space for which the result table is generated
	 * @param request The http request
	 * @return JsonObject containing the result of the query
	 * @throws Exception
	 * @author Ruoyu Zhang
	 */
	public static JsonObject getResultTable(int spaceId, HttpServletRequest request) throws Exception {
		// Parameter validation
		
	    HashMap<String, Integer> attrMap = RESTHelpers.getAttrMap(Primitive.JOB, request);
	    if(null == attrMap){
	    	return null;
	    }
	    
	    JsonObject nextPage = new JsonObject();		// JSON object representing next page for client's DataTable object
	    JsonArray dataTablePageEntries = null;		// JSON array containing the DataTable primitive entries

    	List<Job> jobsToDisplay = new LinkedList<Job>();
		int totalJobsInSpace = Jobs.getCountInSpace(spaceId);
		
		// Retrieves the relevant Job objects to use in constructing the JSON to send to the client
		jobsToDisplay = Jobs.getJobsForNextPage(0, 20, true, 1, "", spaceId);
		
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
			
			Integer score = job.getLiteJobPairStats().get("totalPairs");
			
			// Create an object, and inject the above HTML, to represent an entry in the DataTable
			JsonArray entry = new JsonArray();
    		entry.add(new JsonPrimitive(jobLink));
    		entry.add(new JsonPrimitive(score));
    		entry.add(new JsonPrimitive(score));
    		
    		dataTablePageEntries.add(entry);
    	}
    	
    	nextPage.addProperty(SYNC_VALUE, attrMap.get(SYNC_VALUE));
	    nextPage.addProperty(TOTAL_RECORDS, attrMap.get(TOTAL_RECORDS));
	    nextPage.addProperty(TOTAL_RECORDS_AFTER_QUERY, attrMap.get(TOTAL_RECORDS_AFTER_QUERY));
	    nextPage.add("aaData", dataTablePageEntries);
		return nextPage;
	}
}

