package org.starexec.backend;

import java.util.Map;

import org.starexec.util.Util;

public class OARBackend implements Backend {

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
		    Util.executeCommand("oardel --sql 'true'");	
		    return true;
		} catch (Exception e) {
		    return false;
		}
	}

	@Override
	public String getRunningJobsStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getWorkerNodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getNodeDetails(String nodeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getQueues() {
		// TODO Auto-generated method stub
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
