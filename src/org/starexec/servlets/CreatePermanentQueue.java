package org.starexec.servlets;


import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;


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
		int userId=SessionUtil.getUserId(request);
		int status=QueueSecurity.canUserMakeQueue(userId);
		if (status!=0) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid permissions");
			return;
		}
		String queue_name = (String)request.getParameter(name);
		Integer cpuTimeout=Integer.parseInt(request.getParameter(maxCpuTimeout));
		Integer wallTimeout=Integer.parseInt(request.getParameter(maxWallTimeout));
		
		//String node_name = (String)request.getParameter(Nodes);
		List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));

	
		// Make sure that the queue has a unique name
		if(Queues.notUniquePrimitiveName(queue_name)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The requested queue name is already in use. Please select another.");
			return;
		}
		if (cpuTimeout<=0 || wallTimeout<=0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Timeouts need to be greater than 0.");
			return;
		}
		
		HashMap<WorkerNode, Queue> NQ = new HashMap<WorkerNode, Queue>();
		
		log.debug("nodeIds = " + nodeIds);
		if (nodeIds != null) {
			for (int id : nodeIds) {
				log.debug("id = " + id);
				WorkerNode n = new WorkerNode();
				n.setId(id);
				n.setName(Cluster.getNodeNameById(id));
				Queue q = Cluster.getQueueForNode(n);
				NQ.put(n, q);
			}
		}
		
		//GridEngine Changes
		QueueRequest req = new QueueRequest();
		req.setQueueName(queue_name + ".q");
		GridEngineUtil.createPermanentQueue(req, true, NQ);
		
		//TODO: reduce the count of reservations for nodes that were removed from reservations
		Collection<Queue> queues = NQ.values();
		for (Queue q : queues) {
			// if the queue is not all.q and it is not a permanent queue
			// i.e. it is a reserved queue
			if (q.getId() != 1 && !q.getPermanent()) {
				//TODO: reduce the count of the reservation
				Requests.DecreaseNodeCount(q.getId());
			}
		}
		
		//DatabaseChanges
		int queueId=Queues.getIdByName(queue_name + ".q");
		boolean success = Queues.makeQueuePermanent(queueId);
		success = success && Queues.updateQueueCpuTimeout(queueId, req.getCpuTimeout());
		success = success && Queues.updateQueueWallclockTimeout(queueId, req.getWallTimeout());
		if (!success) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the queue to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
		    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
		}
	}
}
