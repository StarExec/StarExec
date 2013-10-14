package org.starexec.servlets;


import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import org.starexec.data.to.QueueRequest;
import org.starexec.data.to.User;
import org.starexec.util.GridEngineUtil;
import org.starexec.util.Mail;
import org.starexec.util.RobustRunnable;
import org.starexec.util.Util;
import org.starexec.util.Validator;



/**
 * Servlet which handles incoming requests adding new spaces
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class CreateQueue extends HttpServlet {		
	private static final Logger log = Logger.getLogger(AddSpace.class);	

	// Request attributes
	private static final String queueName = "queueName";
	private static final String nodeCount = "nodecount";

	private static final String code = "code";
	private static final String userId = "userId";
	private static final String spaceId = "spaceId";
	private static final String start = "start";
	private static final String end = "end";
	
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
		String queueCode = request.getParameter(code);
		QueueRequest req = Requests.getQueueRequest(queueCode);
		
		String queue_name = req.getQueueName();
		int node_count = req.getNodeCount();
		int queueUserId = req.getUserId();
		int queueSpaceId = req.getSpaceId();
		Date start = req.getStartDate();
		Date end = req.getEndDate();
	
		
		
		
		//Add the queue, reserve the nodes, and approve the reservation
		int newQueueId = Queues.add(queue_name);
		log.debug(newQueueId);
		Cluster.reserveNodes(newQueueId, node_count, start, end);
		boolean approved = Requests.approveQueueReservation(req, newQueueId);
		
		
		User u = Users.get(queueUserId);
		if(approved && !u.getRole().equals("admin")) {
			// Notify user they've been approved	
			Mail.sendReservationResults(req, true);
			log.info(String.format("User [%s] has finished the approval process.", Users.get(req.getUserId()).getFullName()));
			
		} else if (approved && u.getRole().equals("admin")) {
			log.info(String.format("Admin has finished the add queue process."));
			
		} else {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error approving the queue reservation");
		}
		
		/*
		// create a recurring task to check whether or not the start_date is today and then 
		// will finish the queue process by associating the nodes with the queue
	    final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(5);	
		Runnable checkDate = createRunnable(start, nodeIds, newQueueId);
		taskScheduler.scheduleAtFixedRate(checkDate, 0, R.CREATE_QUEUE_PERIOD, TimeUnit.MINUTES);
		log.debug("successfully created a recurring task");

		
		for (int nodeId : nodeIds) {
			String reservedQueueCode = Requests.getQueueReservedCode(newQueueId);
			Cluster.updateNodeDate(nodeId, newQueueId, start, end, reservedQueueCode);
			log.debug("Successfully updated Node reservation dates");
		}
			*/
		if (newQueueId <= 0) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the queue to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
			response.addCookie(new Cookie("New_ID", String.valueOf(newQueueId)));
		    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));	
		}		
	}
	
	/*
	private Runnable createRunnable(final Date start_date, final List<Integer> nodeIds, final int queueId) {
		
		Runnable aRunnable = new Runnable() {
			public void run() {
				associateNodes(start_date, nodeIds, queueId);
			}
		};
		
		return aRunnable;
	}
	
	
	private void associateNodes(Date start_date, List<Integer> nodeIds, int queueId) {
		java.util.Date today = new java.util.Date();
		SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
		boolean is_today = fmt.format(start_date).equals(fmt.format(today));
		if (is_today) {
			Cluster.associateNodes(queueId, nodeIds);
			Queues.setStatus(Queues.get(queueId).getName(), "ACTIVE");
		}
	}
	
	*/

}
