package org.starexec.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	}

	@Override
	public void destroyIf() {		
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
			String details = Util.executeCommand("oarnodes --sql \"network_address = '"+nodeName+"'\"");
			
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

	
	//TODO: Done, test
	@Override
	public String[] getQueues() {
		try {	
			//TODO: This will need to get parsed into the list of nodes. May also need sudo admin
			String queues = Util.executeCommand("oarnotify -l");
			String[] lines = queues.split(System.getProperty("line.separator"));
			List<String> names = new ArrayList<String>();
			for (int i=0;i<lines.length;i+=4) {
				if (lines[i+3].contains("= Active")) {
					names.add(lines[i].trim());
				}
			}
			return names.toArray(new String[names.size()]);
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

	
	// TODO: Done, Test
	@Override
	public boolean deleteQueue(String queueName) {
		try {
			//Unassign all the nodes that were in this queue, making sure they are assigned to nothing.
			Util.executeCommand("oarnodesetting --sql \"queue='"+queueName+"'\" -p \"queue=null\"");
			Util.executeCommand("oarnotify --remove_queue "+queueName);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	//TODO: Done, test
	@Override
	public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
		try {
			//TODO: Check different scheduling algorithms
			Util.executeCommand("oarnotify --add_queue "+newQueueName+" 1 oar_sched_gantt_with_timesharing");
			for (int i =0;i<nodeNames.length;i++) {
				moveNode(nodeNames[i], newQueueName);
			}
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	
	//TODO: Done, test
	@Override
	public boolean moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
		for (int i = 0; i < nodeNames.length; i++) {
			moveNode(nodeNames[i], destQueueName);
		}
		return false;
	}
	
	//TODO: Done, test
	@Override
	public boolean moveNode(String nodeName, String queueName) {
		try {
			Util.executeCommand("oarnodesetting --sql \"network_address='"+nodeName+"'\" -p \"queue="+queueName+"\"");
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public Set<Integer> getActiveExecutionIds() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
