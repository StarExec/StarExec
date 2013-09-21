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
	private static final String name = "name";
	private static final String nodes = "node";

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
		// Make sure the request is valid
		if(!isValid(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The create queue request was malformed");
			return;
		}
		String queueCode = request.getParameter(code);

		  
		// Make the space to be added and set it's basic information
		String queueName = request.getParameter(name);
		List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));

		int newQueueId = Queues.add(queueName, nodeIds);
		log.debug("queueCode = " + queueCode);

		if (queueCode != "") {
			log.debug("This is an approval of a queue_request");
			int queueUserId = Integer.parseInt((String)request.getParameter(userId));
			int queueSpaceId = Integer.parseInt((String)request.getParameter(spaceId));
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			String start_date = request.getParameter(start);
			String end_date = request.getParameter(end);
			
			Date start = null;
			Date end = null;
			try {
				start = new Date(sdf.parse(start_date).getTime());
				end = new Date(sdf.parse(end_date).getTime());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			
			// create a recurring task to check whether or not the start_date is today and then 
			// will finish the queue process by associating the nodes with the queue
		    final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(5);	
			Runnable checkDate = createRunnable(start, nodeIds, newQueueId);
			taskScheduler.scheduleAtFixedRate(checkDate, 0, R.CREATE_QUEUE_PERIOD, TimeUnit.MINUTES);
			log.debug("successfully created a recurring task");

			

			
			QueueRequest req = new QueueRequest();
			req.setCode(queueCode);
			req.setQueueName(queueName);
			req.setSpaceId(queueSpaceId);
			req.setUserId(queueUserId);
			req.setNodeCount(nodeIds.size());
			req.setStartDate(start);
			req.setEndDate(end);
			boolean approved = Requests.approveQueueReservation(req, newQueueId);
			if(approved) {
				
				// Notify user they've been approved	
				Mail.sendReservationResults(req, true);
				
				log.info(String.format("User [%s] has finished the approval process.", Users.get(req.getUserId()).getFullName()));
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error approving the queue reservation");
			}
			
			for (int nodeId : nodeIds) {
				String reservedQueueCode = Requests.getQueueReservedCode(newQueueId);
				Cluster.updateNodeDate(nodeId, newQueueId, start, end, reservedQueueCode);
				log.debug("Successfully updated Node reservation dates");
			}
		} else {
			//if this was a simple createQueue (not a reservation approval), immediately associate the nodes to the queue and set status to "ACTIVE"
			Cluster.associateNodes(newQueueId, nodeIds);
			Queues.setStatus(Queues.get(newQueueId).getName(), "ACTIVE");
		}
			
		if (newQueueId <= 0) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "There was an internal error adding the queue to the starexec database");
		} else {
			// On success, redirect to the space explorer so they can see changes
			response.addCookie(new Cookie("New_ID", String.valueOf(newQueueId)));
		    response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));	
		}		
	}
	
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

	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param spaceRequest The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private boolean isValid(HttpServletRequest queueRequest) {
		try {
			
			// Ensure the queue name is valid (alphanumeric < SPACE_NAME_LEN chars)
			if(!Validator.isValidPrimName((String)queueRequest.getParameter(name))) {
				return false;
			}
			
			// Passed all checks, return true
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		// Return false control flow is broken and ends up here
		return false;
	}
}
