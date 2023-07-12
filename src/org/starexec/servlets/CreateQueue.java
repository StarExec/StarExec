package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.Queues;
import org.starexec.data.security.QueueSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Queue;
import org.starexec.logger.StarLogger;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Servlet which handles incoming requests adding new queues
 *
 * @author Wyatt Kaiser
 */
public class CreateQueue extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(CreateQueue.class);

	// Request attributes
	private static final String name = "name";
	private static final String nodes = "node";
	private static final String maxCpuTimeout = "cpuTimeout";
	private static final String maxWallTimeout = "wallTimeout";
	private static final String numberOfJobsPerQueue = "numberOfJobs";

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * handles the post request 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			//make sure that the user supplied values are valid
			ValidatorStatusCode status = isRequestValid(request);
			if (!status.isSuccess()) {
				//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
				response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
				response.sendError(HttpServletResponse.SC_FORBIDDEN, status.getMessage());
				return;
			}


			//get all the node data required to re-assign the selected nodes from their original queues
			List<Integer> nodeIds = Util.toIntegerList(request.getParameterValues(nodes));
			log.debug("nodeIds = " + nodeIds);
			LinkedList<String> nodeNames = new LinkedList<>();
			LinkedList<String> queueNames = new LinkedList<>();
			if (nodeIds != null) {
				for (int id : nodeIds) {
					log.debug("id = " + id);
					Queue q = Cluster.getQueueForNode(id);
					nodeNames.add(Cluster.getNodeNameById(id));
					if (q == null) {
						queueNames.add(null);
					} else {
						queueNames.add(q.getName());
					}
				}
			}

			//creates the queue
			String queue_name = (String) request.getParameter(name);
			log.debug("queue_name: " + queue_name);
			String qName = queue_name + ".q";
			Integer jobsPerQueue = Integer.parseInt(request.getParameter(numberOfJobsPerQueue));
			String[] nNames = nodeNames.toArray(new String[nodeNames.size()]);
			String[] qNames = queueNames.toArray(new String[queueNames.size()]);
			boolean backend_success = R.BACKEND.createQueueWithSlots(qName, nNames, qNames, jobsPerQueue);
			log.debug("backend_success: " + backend_success);


			//reloads worker nodes and queues
			Cluster.loadWorkerNodes();
			Cluster.loadQueueDetails();

			//DatabaseChanges, sets the timeOut values, and the descriptions
			int queueId = Queues.getIdByName(qName);
			log.debug("just added new queue with id = " + queueId);
			Integer cpuTimeout = Integer.parseInt(request.getParameter(maxCpuTimeout));
			Integer wallTimeout = Integer.parseInt(request.getParameter(maxWallTimeout));
			String description = request.getParameter("description");
			boolean success = Queues.updateQueueCpuTimeout(queueId, cpuTimeout);
			success = success && Queues.updateQueueWallclockTimeout(queueId, wallTimeout);
			success &= Queues.updateQueueDesc(queueId, description);
			if (!success) {
				response.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"There was an internal error adding the queue to the starexec database"
				);
			} else {
				// On success, redirect to the space explorer so they can see changes
				response.sendRedirect(Util.docRoot("secure/admin/cluster.jsp"));
			}
		} catch (Exception e) {
			log.error("Caught Exception in CreateQueue.doPost" + e.getMessage());
		}
	}

	/*
	 * Given an http request, make sure that the user supplied parameters are valid, 
	 */
	private static ValidatorStatusCode isRequestValid(HttpServletRequest request) {
		try {
			int userId = SessionUtil.getUserId(request);
			String queueName = request.getParameter(name);
			if (!Validator.isValidPosInteger(request.getParameter(maxCpuTimeout)) ||
					!Validator.isValidPosInteger(request.getParameter(maxWallTimeout))) {
				return new ValidatorStatusCode(false, "Timeouts need to be valid integers");
			}

			Integer cpuTimeout = Integer.parseInt(request.getParameter(maxCpuTimeout));
			Integer wallTimeout = Integer.parseInt(request.getParameter(maxWallTimeout));
			if (cpuTimeout <= 0 || wallTimeout <= 0) {
				return new ValidatorStatusCode(false, "Timeouts need to be greater than 0.");
			}

			Integer jobsPerQueue = Integer.parseInt(request.getParameter(numberOfJobsPerQueue));
			if (jobsPerQueue != 1 && jobsPerQueue != 2) {
				return new ValidatorStatusCode(false, "Number of jobs must be 1 or 2");
			}

			return QueueSecurity.canUserMakeQueue(userId, queueName);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return new ValidatorStatusCode(false, "Internal error processing queue creation request");
	}
}
