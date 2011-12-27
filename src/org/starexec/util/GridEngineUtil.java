package org.starexec.util;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;

/**
 * Contains methods for interacting with the sun grid engine. This class is NOT operating system independent
 * and may require environmental set up to function properly in Windows.
 * @author Tyler Jensen
 *
 */
public class GridEngineUtil {
	private static final Logger log = Logger.getLogger(GridEngineUtil.class);
	
	// The node detail attribute regex pattern to parse SGE output
	private static Pattern keyValPattern;
	
	static {
		// Compile the SGE node detail parse pattern when this class is loaded
		keyValPattern = Pattern.compile(R.NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);
	}
	
	/**
	 * Gets the worker nodes from SGE and adds them to the database if they don't already exist.
	 */
	public static void loadWorkerNodes() {
		BufferedReader nodeResults = null;
		
		try {			
			// Execute the SGE command to get the node list
			nodeResults = Util.executeCommand(R.NODE_LIST_COMMAND);
			
			if(nodeResults == null) {
				// If the command failed, return now				
				return;
			}
			
			// Read the nodes one at a time
			String line;		
			while((line = nodeResults.readLine()) != null) {
				String name = line;
				if(!R.USE_FULL_HOST_NAME) {
					// Just get the first part of the name if we're not using the full host name
					int periodIndex = line.indexOf(".");					
					if(periodIndex > 0) {						
						name = line.substring(0, periodIndex);
					}														
				}
				
				// In the database, update the attributes for the node
				Cluster.updateNode(name,  GridEngineUtil.getNodeDetails(name));
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		} finally {
			// Try to close the result list if it is allowed
			try { nodeResults.close(); } catch (Exception e) { }
		}
	}
	
	/**
	 * Calls SGE to get details about the given node. 
	 * @param name The name of the node to get details about
	 * @return A hashmap of key value pairs. The key is the attribute name and the value is the value for that attribute.
	 */
	public static HashMap<String, String> getNodeDetails(String name) {
		// Make the results hashmap that will be returned		
		HashMap<String, String> details = new HashMap<String, String>();
		
		// Call SGE to get details for the given node
		String results = Util.bufferToString(Util.executeCommand(R.NODE_DETAILS_COMMAND + name));
		
		// Parse the output from the SGE call to get the key/value pairs for the node
		java.util.regex.Matcher matcher = keyValPattern.matcher(results);
		
		// For each match...
		while(matcher.find()) {
			// Split apart the key from the value
			String[] keyVal = matcher.group().split("=");
			
			// Add the results to the details hashmap
			details.put(keyVal[0], keyVal[1]);
		}
		
		return details;
	}	
}
