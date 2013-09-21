package org.starexec.servlets;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.User;
import org.starexec.util.Mail;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;

/**
 * Servlet which handles requests for registration 
 * @author Todd Elvers & Tyler Jensen
 */
@SuppressWarnings("serial")
public class ReserveQueue extends HttpServlet {
	private static final Logger log = Logger.getLogger(ReserveQueue.class);	
	
	// Param strings for processing
	private static final String name = "name";
	private static final String msg = "msg";
	private static final String sid = "sid";
	private static final String node = "node";
	private static final String start = "start";
	private static final String end = "end";
	

	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {				
		QueueRequest queueRequest = constructQueueRequest(request);
		
		if(queueRequest == null){
		    response.sendRedirect(Util.docRoot("secure/reserve/queue.jsp?result=requestNotSent"));
			return;
		}
		
		boolean added = Requests.addQueueRequest(queueRequest);
		if(added){
			// Send the invite to the admins of the community 
			Mail.sendQueueRequest(queueRequest);
			//Mail.sendCommunityRequest(user, comRequest);
			response.sendRedirect(Util.docRoot("secure/reserve/queue.jsp?sid=" + queueRequest.getSpaceId() + "&result=requestSent"));
		} else {
			// There was a problem
		    response.sendRedirect(Util.docRoot("secure/reserve/queue.jsp?sid=" + queueRequest.getSpaceId() + "&result=requestNotSent"));
		}
	}
	
	/**
	 * Builds an request object
	 * 
	 * @param user the user to create the invite for
	 * @param request the servlet containing the invite information
	 * @return the invite constructed
	 */
	private QueueRequest constructQueueRequest(HttpServletRequest request){
		try {
			String queue_Name = request.getParameter(name);
			String message = request.getParameter(msg);
			int user_id = SessionUtil.getUserId(request);
			int space_id = Integer.parseInt((String)request.getParameter(sid));
			int node_count = Integer.parseInt((String)request.getParameter(node));
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			String start_date = request.getParameter(start);
			String end_date = request.getParameter(end);
			
			Date start = new Date(sdf.parse(start_date).getTime());
			Date end = new Date(sdf.parse(end_date).getTime());
			
			QueueRequest req = new QueueRequest();
			req.setUserId(user_id);
			req.setSpaceId(space_id);
			req.setQueueName(queue_Name);
			req.setNodeCount(node_count);
			req.setStartDate(start);
			req.setEndDate(end);
			req.setMessage(message);
			req.setCode(UUID.randomUUID().toString());
			return req;	
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return null;
	}
}
	