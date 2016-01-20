package org.starexec.servlets;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Queue;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles incoming requests adding new queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class CreateQueue extends HttpServlet {		
	private static final Logger log = Logger.getLogger(CreateQueue.class);	

	// Request attributes
	private static final String name = "name";
	private static final String nodes = "node";
	private static final String maxCpuTimeout="cpuTimeout";
	private static final String maxWallTimeout="wallTimeout";
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	

		ValidatorStatusCode status=isRequestValid(request);
		if (!status.isSuccess()) {
			//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
			response.sendError(HttpServletResponse.SC_FORBIDDEN, status.getMessage());
			return;
		}
		
		
		//String node_name = (String)request.getParameter(Nodes);
		List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));

		
		log.debug("nodeIds = " + nodeIds);
		LinkedList<String> nodeNames = new LinkedList<String>();
		LinkedList<String> queueNames = new LinkedList<String>();
		if (nodeIds != null) {
		    for (int id : nodeIds) {
				log.debug("id = " + id);
				Queue q = Cluster.getQueueForNode(id);
				nodeNames.add(Cluster.getNodeNameById(id));
				if(q == null){
				    queueNames.add(null);
				}else{
				    queueNames.add(q.getName());
				}
		    }
		}
		String queue_name = (String)request.getParameter(name);
		log.debug("queue_name: " + queue_name);

		String qName = queue_name+".q";
	
		//TODO : BUG when trying to create a queue using an orphaned node, seems to create queue with right node,
		//returning wrong status code for some reason? seems related to cputimeout and wallclock timeout
		String[] nNames = nodeNames.toArray(new String[nodeNames.size()]);
		String[] qNames = queueNames.toArray(new String[queueNames.size()]);
		boolean backend_success = R.BACKEND.createQueue(qName,nNames,qNames);

		log.debug("backend_success: " + backend_success);

		//reloads worker nodes and queues
		Cluster.loadWorkerNodes();
		Cluster.loadQueues();
				
		//DatabaseChanges
		int queueId=Queues.getIdByName(qName);
		log.debug("just added new queue with id = "+queueId);
		
		
		Integer cpuTimeout=Integer.parseInt(request.getParameter(maxCpuTimeout));
		Integer wallTimeout=Integer.parseInt(request.getParameter(maxWallTimeout));
		
		boolean success = Queues.updateQueueCpuTimeout(queueId, cpuTimeout);
		success = success && Queues.updateQueueWallclockTimeout(queueId, wallTimeout);
		if (!success) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the queue to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
		    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
		}
	}
	
	private static ValidatorStatusCode isRequestValid(HttpServletRequest request) {
		try {
			int userId=SessionUtil.getUserId(request);
			String queueName = request.getParameter(name);
			if (!Validator.isValidPosInteger(request.getParameter(maxCpuTimeout)) || !Validator.isValidPosInteger(request.getParameter(maxWallTimeout))) {
				return new ValidatorStatusCode(false, "Timeouts need to be valid integers");
			}
			
			Integer cpuTimeout=Integer.parseInt(request.getParameter(maxCpuTimeout));
			Integer wallTimeout=Integer.parseInt(request.getParameter(maxWallTimeout));
			if (cpuTimeout<=0 || wallTimeout<=0) {
				return new ValidatorStatusCode(false,"Timeouts need to be greater than 0.");
			}
			
			
			return	QueueSecurity.canUserMakeQueue(userId, queueName);

		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		
		return new ValidatorStatusCode(false, "Internal error processing queue creation request");
		
	}
}
