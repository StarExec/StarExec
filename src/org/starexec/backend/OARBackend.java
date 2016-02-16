package org.starexec.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.starexec.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Implementation of the Backend interface depending on the OAR scheduler (https://oar.imag.fr/)
 */
public class OARBackend implements Backend {    
	private static Logger log = Logger.getLogger(OARBackend.class);
	
	private static String JOB_ID_PATTERN = "OAR_JOB_ID=(-?\\d+)";
	
	
	
    // The regex patterns used to parse SGE output
 	private static Pattern jobIdPattern;

 	static {
 		// Compile the SGE output parsing patterns when this class is loaded
 		jobIdPattern = Pattern.compile(JOB_ID_PATTERN, Pattern.CASE_INSENSITIVE);
 	}
	@Override
	public void initialize(String BACKEND_ROOT) {
		//no initialization required
	}

	@Override
	public void destroyIf() {
		//no deconstruction required
	}

	@Override
	public boolean isError(int execCode) {
		return execCode<0;
	}

	@Override
	public int submitScript(String scriptPath, String workingDirectoryPath, String logPath) {
		try {
			String output = Util.executeCommand(new String[]{"oarsub","-O", logPath,"-E",logPath,"-d",workingDirectoryPath,
					"-l","/cpuset=1","-S",scriptPath});
			Matcher jobId = jobIdPattern.matcher(output);
			if (jobId.find()) {
				return Integer.parseInt(jobId.group(1));
			} else {
				log.warn("could not find any job ID for this submission!");
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return -1;
	}

	@Override
	public boolean killPair(int execId) {
		try{
		    Util.executeCommand("oardel " + execId);	
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}

	@Override
	public boolean killAll() {
		try{
			for (Integer i : this.getActiveExecutionIds()) {
				if (!killPair(i)) {
					log.error("ERROR: Unable to kill pair with execution id: " +i);
				}
			}
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}

	@Override
	public String getRunningJobsStatus() {
		try {	
			return Util.executeCommand("oarstat");
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

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
	
	@Override
	public String[] getQueues() {
		try {	
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
	public Map<String, String> getNodeQueueAssociations() {
		try {
			String json = Util.executeCommand("oarnodes -J");
			JsonObject object = new JsonParser().parse(json).getAsJsonObject();
			HashMap<String, String> nodesToQueues = new HashMap<String, String>();
			for (Entry<String, JsonElement> s : object.entrySet()) {
				nodesToQueues.put(s.getValue().getAsJsonObject().get("network_address").getAsString(),
						s.getValue().getAsJsonObject().get("queue").getAsString());
			}
			return nodesToQueues;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

	@Override
	public boolean clearNodeErrorStates() {
		try {			
			Util.executeCommand(new String[] {"oarnodesetting","--sql","state='Suspected'","-s","Alive"});
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	
	@Override
	public boolean deleteQueue(String queueName) {
		try {
			//Unassign all the nodes that were in this queue, making sure they are assigned to nothing.
			Util.executeCommand(new String[] {"oarnodesetting","--sql","queue='"+queueName+"'","-p","queue=null"});
			Util.executeCommand("oarnotify --remove_queue "+queueName);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean createQueue(String newQueueName, String[] nodeNames, String[] sourceQueueNames) {
		try {
			//TODO: Check different scheduling algorithms
			Util.executeCommand("oarnotify --add_queue "+newQueueName+",1,oar_sched_gantt_with_timesharing");
			moveNodes(newQueueName, nodeNames, sourceQueueNames);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	
	@Override
	public boolean moveNodes(String destQueueName, String[] nodeNames, String[] sourceQueueNames) {
		for (int i = 0; i < nodeNames.length; i++) {
			moveNode(nodeNames[i], destQueueName);
		}
		return false;
	}
	
	@Override
	public boolean moveNode(String nodeName, String queueName) {
		try {
			Util.executeCommand(new String [] {"oarnodesetting","--sql","network_address='"+nodeName+"'","-p","queue="+queueName});
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public Set<Integer> getActiveExecutionIds() throws IOException {
		try {
			String json = Util.executeCommand("oarstat -J");
			JsonObject object = new JsonParser().parse(json).getAsJsonObject();
			Set<Integer> ids = new HashSet<Integer>();
			for (Entry<String, JsonElement> s : object.entrySet()) {
				ids.add(s.getValue().getAsJsonObject().get("Job_Id").getAsInt());
			}
			return ids;
		} catch (com.google.gson.stream.MalformedJsonException e) {
			// this exception will get thrown whenever there is nothing running and oarstat -J
			// is executed, so we can return the empty set
			return new HashSet<Integer>();
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

}
