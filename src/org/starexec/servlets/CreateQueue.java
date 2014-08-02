package org.starexec.servlets;


import java.io.IOException;
import java.sql.Date;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Users;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;



/**
 * Servlet which handles incoming requests adding new queues
 * @author Wyatt Kaiser
 */
@SuppressWarnings("serial")
public class CreateQueue extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	



	private static final String id = "id";

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

		ValidatorStatusCode status=QueueSecurity.canUserMakeQueue(userId);
		if (!status.isSuccess()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, status.getMessage());
			return;
		}
		Integer requestId=  Integer.parseInt(request.getParameter(id));
		QueueRequest req = Requests.getQueueRequest(requestId);
		
		String queue_name = req.getQueueName();
		int queueUserId = req.getUserId();
		int queueSpaceId = req.getSpaceId();
		Date start = req.getStartDate();
		Date end = req.getEndDate();
		String message = req.getMessage();
	
		// Make sure that the queue has a unique name
		if(Queues.notUniquePrimitiveName(queue_name)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The requested queue name is already in use. Please select another.");
			return;
		}
		
		
		
		//Add the queue, reserve the nodes, and approve the reservation
		int newQueueId = Queues.add(queue_name + ".q",req.getCpuTimeout(),req.getWallTimeout());
		Cluster.reserveNodes(req.getId(), start, end);
		//boolean approved = Requests.removeQueueReservation(req.getId());
		//Cluster.updateTempChanges();

		
		if(!Users.isAdmin(queueUserId)) {
			// Notify user they've been approved	
			Mail.sendReservationResults(req, true);
			log.info(String.format("User [%s] has finished the approval process.", Users.get(req.getUserId()).getFullName()));
			
		} else if (Users.isAdmin(queueUserId)) {
			log.info(String.format("Admin has finished the add queue process."));
			
		} else {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error approving the queue reservation");
		}

		if (newQueueId <= 0) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the queue to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
			response.addCookie(new Cookie("New_ID", String.valueOf(newQueueId)));
		    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));	
		}		
	}
}
