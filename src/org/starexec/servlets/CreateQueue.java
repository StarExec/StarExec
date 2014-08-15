package org.starexec.servlets;


import java.io.IOException;
import java.sql.Date;

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
import org.starexec.data.database.Users;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;



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

		ValidatorStatusCode status=isRequestValid(request);
		if (!status.isSuccess()) {
			//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
			response.sendError(HttpServletResponse.SC_FORBIDDEN, status.getMessage());
			return;
		}
		Integer requestId=  Integer.parseInt(request.getParameter(id));
		QueueRequest req = Requests.getQueueRequest(requestId);
		
		String queue_name = req.getQueueName();
		int queueUserId = req.getUserId();
		Date start = req.getStartDate();
		Date end = req.getEndDate();

		//Add the queue, reserve the nodes, and approve the reservation
		int newQueueId = Queues.add(queue_name + ".q",req.getCpuTimeout(),req.getWallTimeout());
		Cluster.reserveNodes(req.getId(), start, end);
		
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
	
	private static ValidatorStatusCode isRequestValid(HttpServletRequest request) {
		try {
			int userId=SessionUtil.getUserId(request);
			if (!Validator.isValidInteger(request.getParameter(id))) {
				return new ValidatorStatusCode(false, "The request id needs to be a valid integer");
			}
			Integer requestId=  Integer.parseInt(request.getParameter(id));
			QueueRequest req = Requests.getQueueRequest(requestId);
			if (req==null) {
				return new ValidatorStatusCode(false, "The queue request you are referencing could not be found");
			}
			String queueName = req.getQueueName();
			// Make sure that the queue has a unique name
			if(Queues.notUniquePrimitiveName(queueName)) {
				return new ValidatorStatusCode(false,"The requested queue name is already in use. Please select another.");
			}
			
			return QueueSecurity.canUserMakeQueue(userId, queueName);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return new ValidatorStatusCode(false, "There was an internal error processing your request");
	}
}
