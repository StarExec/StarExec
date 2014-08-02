package org.starexec.servlets;


import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.util.Util;


/**
 * Servlet which handles incoming requests adding new permanent queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class AssocCommunities extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	

	// Request attributes
	private static final String name = "name";
	private static final String communities = "community";

	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	//TODO: Secure
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	

		String queue_name = (String)request.getParameter(name);
		int queue_id = Queues.getIdByName(queue_name);
		List<Integer> community_ids = Util.toIntegerList(request.getParameterValues(communities));
		
		
		boolean result = Cluster.setPermQueueCommunityAccess(community_ids, queue_id);
		
		if (result) {
			response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
		} else {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
