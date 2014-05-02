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
import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Requests;
import org.starexec.data.to.Queue;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.WorkerNode;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Util;


/**
 * Servlet which handles incoming requests adding new permanent queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class MoveNodes extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	

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

		String queue_name = (String)request.getParameter(name);
		//String node_name = (String)request.getParameter(Nodes);
		List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));
		
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
		req.setQueueName(queue_name);
		
		GridEngineUtil.moveNodes(req, NQ);
		
		Collection<Queue> queues = NQ.values();
		for (Queue q : queues) {
			// if the queue is not all.q and it is not a permanent queue
			// i.e. it is a reserved queue
			if (!q.getName().equals(R.DEFAULT_QUEUE_NAME) && !q.getPermanent()) {
				Requests.DecreaseNodeCount(q.getId()); // decrease the node count of the reservation by 1
			}
		}
		
		response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
		}
	}
