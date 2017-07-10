package org.starexec.servlets;

import com.google.gson.Gson;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.logger.StarLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;

/**
 * Returns Analytics information
 */
@SuppressWarnings("serial")
public class Analytics extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(Analytics.class);
	private static Gson gson = new Gson();

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String start = request.getParameter("start");
			String end   = request.getParameter("end");

			Date startDate, endDate;

			try {
				startDate = Date.valueOf(start);
			} catch (IllegalArgumentException e) {
				startDate = new Date(116,04,01); // 2017-04-01
			}

			try {
				endDate = Date.valueOf(end);
			} catch (IllegalArgumentException e) {
				endDate = new Date(130,04,01); // 2030-04-01
			}

			org.starexec.data.database.Analytics.saveToDB();

			response.setContentType("application/json");
			response.getWriter().write(gson.toJson(
				org.starexec.data.to.AnalyticsResults.getAllEvents(startDate, endDate)
			));
		} catch (Exception e) {
			log.warn("Caught Exception in doGet", e);
			throw e;
		}
	}

	/**
	 * Validates the request to make sure the requested data is of the right format
	 * @param request The request need to be validated.
	 * @return true if the request is valid.
	 */
	private static ValidatorStatusCode validateRequest(HttpServletRequest request) {
		try {
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		return new ValidatorStatusCode(false, "");
	}
}
