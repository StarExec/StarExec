package org.starexec.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.starexec.util.Util;

public class OARBackend implements Backend {
	
    private static String NODE_DETAIL_PATTERN = "[^\\s,][\\w|-]+=[^,\\s]+";  // The regular expression to parse out the key/value pairs from OAR's node detail output

    private static Pattern nodeKeyValPattern;
 	static {
 		// Compile the SGE output parsing patterns when this class is loaded
 		nodeKeyValPattern = Pattern.compile(NODE_DETAIL_PATTERN, Pattern.CASE_INSENSITIVE);

 	}
    
    
	private static Logger log = Logger.getLogger(OARBackend.class);
	@Override
	public void initialize(String BACKEND_ROOT) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroyIf() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isError(int execCode) {
		return execCode<0;
	}

	@Override
	public int submitScript(String scriptPath, String workingDirectoryPath, String logPath) {
		// TODO Auto-generated method stub
		return 0;
	}

	//TODO: Done, test
	@Override
	public boolean killPair(int execId) {
		try{
		    Util.executeCommand("oardel " + execId);	
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}

	//TODO: Done, test
	@Override
	public boolean killAll() {
		try{
		    Util.executeCommand("oardel --sql 'true'");	
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}

	//TODO: Done, test
	@Override
	public String getRunningJobsStatus() {
		try {	
			return Util.executeCommand("oarstat -f");
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

	//TODO: Done, test
	@Override
	public String[] getWorkerNodes() {
		try {	
			String nodes = Util.executeCommand("oarnodes -l");
			
    		return nodes.split(System.getProperty("line.separator"));
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}
	
	//TODO: Done, test
	@Override
	public Map<String, String> getNodeDetails(String nodeName) {
		try {	
			String details = Util.executeCommand("oarnodes -sql \"network_address = '"+nodeName+"'\"");
			
			// Parse the output from the SGE call to get the key/value pairs for the node
    		java.util.regex.Matcher matcher = nodeKeyValPattern.matcher(details);

    		Map<String, String> detailMap = new HashMap<String,String>();
    		// For each match...
    		while(matcher.find()) {
    			// Split apart the key from the value
    			String[] keyVal = matcher.group().split("=");
    			
    			// Add the results to the details list
    			detailMap.put(keyVal[0], keyVal[1]);
    		}

    		return detailMap;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

	@Override
	public String[] getQueues() {
		try {	
			//TODO: This will need to get parsed into the list of nodes. May also need sudo admin
			String queues = Util.executeCommand("oarnotify -l");
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

	@Override
	public Map<String, String> getQueueDetails(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getQueueNodeAssociations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean clearNodeErrorStates() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteQueue(String queueName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean moveNode(String nodeName, String queueName) {
		// TODO Auto-generated method stub
		return false;
	}

}
