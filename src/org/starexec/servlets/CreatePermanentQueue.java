package org.starexec.servlets;


import java.io.IOException;
import java.util.Collection;
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
import org.starexec.data.database.Requests;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;


/**
 * Servlet which handles incoming requests adding new permanent queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class CreatePermanentQueue extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	

	// Request attributes
	private static final String name = "name";
	//private static final String Nodes = "Nodes";
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

		HashMap<WorkerNode, Queue> NQ = new HashMap<WorkerNode, Queue>();
		
		log.debug("nodeIds = " + nodeIds);
		LinkedList<String> nodeNames = new LinkedList<String>();
		LinkedList<String> queueNames = new LinkedList<String>();
		if (nodeIds != null) {
		    for (int id : nodeIds) {
			log.debug("id = " + id);
			WorkerNode n = new WorkerNode();
			n.setId(id);
			n.setName(Cluster.getNodeNameById(id));
			Queue q = Cluster.getQueueForNode(n);
			NQ.put(n, q);

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

		//BACKEND Changes
		QueueRequest req = new QueueRequest();
		req.setQueueName(queue_name + ".q");
	
		//TODO : BUG when trying to create a permanent queue using an orphaned node, seems to create queue with right node, returning wrong status code for some reason? seems related to cputimeout and wallclock timeout
		String[] nNames = nodeNames.toArray(new String[nodeNames.size()]);
		String[] qNames = queueNames.toArray(new String[queueNames.size()]);
		boolean backend_success = R.BACKEND.createPermanentQueue(true,queue_name+".q",nNames,qNames);

		log.debug("backend_success: " + backend_success);

		//reloads worker nodes and queues
		Cluster.loadWorkerNodes();
		Cluster.loadQueues();
		
		Collection<Queue> queues = NQ.values();
		for (Queue q : queues) {
			// if the queue is not all.q and it is not a permanent queue
			// i.e. it is a reserved queue
			if (q.getId() != 1 && !q.getPermanent()) {
				Requests.DecreaseNodeCount(q.getId());
			}
		}
		
		//DatabaseChanges
		log.debug("about to get queue with name = "+req.getQueueName());
		int queueId=Queues.getIdByName(req.getQueueName());
		log.debug("just added new permanent queue with id = "+queueId);
		
		boolean success = Queues.makeQueuePermanent(queueId);
		log.debug("after Queues.makeQueuePermament - success: " + success);
		
		Integer cpuTimeout=Integer.parseInt(request.getParameter(maxCpuTimeout));
		Integer wallTimeout=Integer.parseInt(request.getParameter(maxWallTimeout));
		log.debug(cpuTimeout);
		log.debug(wallTimeout);
		success = success && Queues.updateQueueCpuTimeout(queueId, cpuTimeout);
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
			if (!Validator.isValidInteger(request.getParameter(maxCpuTimeout)) || !Validator.isValidInteger(request.getParameter(maxWallTimeout))) {
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
