package com.starexec.data;

import java.io.File;
import java.sql.*;
import java.util.*;
import javax.xml.parsers.*;

import org.apache.log4j.Logger;
import org.w3c.dom.*;

import com.starexec.constants.R;


/**
 * Manages, sets up and maintains prepared statements to be consumed and used by a
 * database.
 *  
 * @author Tyler Jensen
 */
public class StatementPool {	
	private static final Logger log = Logger.getLogger(StatementPool.class);
	
	// Private map which holds key/value pairs where the key is the statement key and the value is the corresponding prepared statement
	private Map<String, PreparedStatement> pool = new HashMap<String, PreparedStatement>();	
	
	/**
	 * Reads in the SQL statement xml file and prepares all statements for
	 * use by a database object.
	 * @param connection The connection to prepare the statements with
	 */
	public void Initialize(Connection connection){
		try {
			// Open the statements xml file and parse it into a dom
			File statementFile = new File(R.CLASS_PATH, "sqlstatements.xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			Document sqlDoc = db.parse(statementFile);
			sqlDoc.getDocumentElement().normalize();
			   
			// Get all statement nodss
			NodeList statementNodes = sqlDoc.getElementsByTagName(STATEMENT_NODE);			   
			log.debug("Parsing sql XML file resulted in " + statementNodes.getLength() + " statements.");
			
			// For each statement node in the xml file...
			for(int i = 0; i < statementNodes.getLength(); i++) {
				Node currentStatement = statementNodes.item(i);
				
				// Get all appropriate attributes
				String query = currentStatement.getTextContent();				
				String key = currentStatement.getAttributes().getNamedItem(STATEMENT_KEY).getNodeValue();								
				Boolean returnRows = null == currentStatement.getAttributes().getNamedItem(RETURN_ROWS);
				Boolean updateable = null == currentStatement.getAttributes().getNamedItem(UPDATEABLE);
				
				PreparedStatement prepStatement = null;
				
				// NOTE: For simplicity, we support only one additional attribute aside from key. For example
				// a node cannot be updateable and return rows, it must be one or the other. Also, we assume
				// the presence of either attribute means we should use that constructor no matter the attribute value
				
				// Based on the node's attribute, use the appropriate constructor
				if(returnRows) {
					prepStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				} else if (updateable){
					prepStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				} else {
					prepStatement = connection.prepareStatement(query);
				}
				
				// Add the statement to the pool
				pool.put(key, prepStatement);
			}			 
			 
			log.debug(pool.entrySet().size() + " entries added to the statement pool.");
		} catch (Exception e) {
			log.fatal(e);
		}
	}
	
	/**
	 * @param key The key of the desired prepared statement
	 * @return The prepared statement associated with the given key
	 */
	public PreparedStatement getStatement(String key){
		if(pool.containsKey(key)){
			// Return the key if we have it, or else return null
			return pool.get(key);
		}
		
		log.error("Could not find statement with key: " + key);
		return null;
	}
	
	// All keys below should correspond to an entry in the sqlstatements.xml file so code can reference the desired statement
	
	// Add keys
	public static final String ADD_USER = "adduser";
	public static final String ADD_PASSWORD = "addpass";
	public static final String ADD_PERMISSION = "addperm";
	public static final String ADD_LEVEL = "addlvl";
	public static final String ADD_BENCHMARK = "addbench";
	public static final String ADD_SOLVER = "addslvr";
	public static final String ADD_CAN_SOLVE = "addcanslv";
	public static final String ADD_JOB = "addjob";
	public static final String ADD_JOB_PAIR = "addjobpair";
	public static final String ADD_CONFIGURATION = "addconfig";
	public static final String ADD_CONFIGURATION2 = "addconfig2";
	
	// Get keys
	public static final String GET_USER = "getusr";
	public static final String GET_USER2 = "getusr2";	
	public static final String GET_SOLVERS = "getslvr";
	public static final String GET_ALL_SOLVERS = "getallslvr";
	public static final String GET_BENCHMARKS = "getbench";
	public static final String GET_IMMEDIATE_BENCHMARKS = "getimbench";
	public static final String GET_ALL_BENCHMARKS = "getallbench";
	public static final String GET_MAX_LEVEL = "getmaxlevel";
	public static final String GET_MAX_LEVEL_GROUP = "getmaxgroup";
	public static final String GET_ROOT_LEVELS = "getrtlvl";
	public static final String GET_SUBLEVELS = "getsublvl";	
	public static final String GET_LEVELS_FROM_BENCH = "getlvlbench";
	public static final String GET_JOBS = "getjobs";
	public static final String GET_JOB_PAIRS = "getjobpairs";	
	public static final String GET_CONFIGURATIONS = "getconfigs";
	public static final String GET_CONFIGURATION_IDS = "getconfigids";
	public static final String GET_CONFIGURATION = "getconfig";
	public static final String GET_CONFIGURATION_SOLVER = "getconfigforslvr";
	
	// Set keys
	public static final String SET_JOB_STATUS_START = "setjobstatstrt";
	public static final String SET_JOB_STATUS_DONE = "setjobstatdone";
	public static final String SET_PAIR_STATUS = "setpairstat";
	
	// Private xml tags and attributes for parsing statements
	private static final String STATEMENT_NODE = "Statement";
	private static final String STATEMENT_KEY = "key";
	private static final String RETURN_ROWS = "row_return";	
	private static final String UPDATEABLE = "updateable";
}
