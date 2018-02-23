package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.Queue;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Servlet which handles incoming requests to move nodes from queue to queue
 *
 * @author Wyatt Kaiser
 */
@SuppressWarnings("ALL")
public class MoveNodes extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(MoveNodes.class);

	// Request attributes
	private static final String name = "name";
	private static final String nodes = "node";

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			int userId = SessionUtil.getUserId(request);
			if (!GeneralSecurity.hasAdminWritePrivileges(userId)) {
				String message = "You do not have permission to perform the requested operation";
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
				return;
			}
			log.debug("Received request to move nodes.");

			String queueName = (String) request.getParameter(name);
			List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));

			log.debug("nodeIds = " + nodeIds);

			LinkedList<String> nodeNames = new LinkedList<>();
			LinkedList<String> queueNames = new LinkedList<>();
			// map for counting how many nodes will be removed from each queue
			HashMap<Integer, Integer> queueIdToNodesRemoved = new HashMap<>();
			if (nodeIds != null) {
				for (int id : nodeIds) {
					Queue q = Cluster.getQueueForNode(id);

					nodeNames.add(Cluster.getNodeNameById(id));
					if (q == null) {
						queueNames.add(null);
					} else {
						queueNames.add(q.getName());
						if (!queueIdToNodesRemoved.containsKey(q.getId())) {
							queueIdToNodesRemoved.put(q.getId(), 0);
						}
						queueIdToNodesRemoved.put(q.getId(), queueIdToNodesRemoved.get(q.getId()) + 1);
					}
				}
			}

			//BACKEND Changes
			R.BACKEND.moveNodes(queueName, nodeNames.toArray(new String[nodeNames.size()]),
			                    queueNames.toArray(new String[queueNames.size()]));

			Cluster.loadWorkerNodes();
			Cluster.loadQueueDetails();
			response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
		} catch (Exception e) {
			log.error("Caught Exception in MoveNodes.doPost", e);
			response.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error moving the nodes.");
		}
	}
}
