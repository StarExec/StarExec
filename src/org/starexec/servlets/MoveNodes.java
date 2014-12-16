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
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.database.Queues;


import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

/**
 * Servlet which handles incoming requests adding new permanent queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class MoveNodes extends HttpServlet {		
	private static final Logger log = Logger.getLogger(MoveNodes.class);	

	// Request attributes
	private static final String name = "name";
	private static final String nodes = "node";

	
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

	try {
		int userId=SessionUtil.getUserId(request);
		if (!Users.isAdmin(userId)) {
			String message="You do not have permission to perform the requested operation";
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
		}
	    log.debug("Received request to move nodes.");

	    String queueName = (String)request.getParameter(name);
	    List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));
		
	    HashMap<WorkerNode, Queue> NQ = new HashMap<WorkerNode, Queue>();
		
	    log.debug("nodeIds = " + nodeIds);
	    
	    LinkedList<String> nodeNames = new LinkedList<String>();
	    LinkedList<String> queueNames = new LinkedList<String>();

	    if (nodeIds != null) {
		for (int id : nodeIds) {

		    //TODO: don't need to make WorkerNode, can get queue by node id
		    WorkerNode n = new WorkerNode();
		    n.setId(id);
		    n.setName(Cluster.getNodeNameById(id));
		    Queue q = Cluster.getQueueForNode(n);
		    NQ.put(n, q);

		    nodeNames.add(Cluster.getNodeNameById(id));
		    if(q == null){
			queueNames.add(null);
		    } else{
			queueNames.add(q.getName());
			//Need to call this in prepartion for moving the nodes
			Queues.pauseJobsIfOneWorker(q);
		    }

		}
	    }
		
	    //BACKEND Changes
	    R.BACKEND.moveNodes(R.SGE_ROOT,queueName,nodeNames.toArray(new String[nodeNames.size()]),queueNames.toArray(new String[queueNames.size()]));

	    Cluster.loadWorkerNodes();
	    Cluster.loadQueues();
		
	    Collection<Queue> queues = NQ.values();
	    for (Queue q : queues) {
		// if there is a queue which is not all.q and it is not a permanent queue
		// i.e. it is a reserved queue
		if (q != null)
		    if (!q.getName().equals(R.DEFAULT_QUEUE_NAME) && !q.getPermanent()) {
			Requests.DecreaseNodeCount(q.getId()); // decrease the node count of the reservation by 1
		    }
	    }
		
	    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
	}
	catch (Exception e) {
	    log.error("Move Nodes Servlet encountered this exception: " + e.getMessage(), e);
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error moving the nodes.");
	}
    }
}
