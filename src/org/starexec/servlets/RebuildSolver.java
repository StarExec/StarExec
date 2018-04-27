package org.starexec.servlets;

import org.starexec.data.database.Common;
import org.starexec.data.database.Solvers;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Solver;
import org.starexec.jobs.JobManager;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.Integer;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * Rebuild an existing solver.
 * This can be usefull if StarExec's version of GCC is updated
 */
public class RebuildSolver extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(RebuildSolver.class);
	private static final Gson gson = new Gson();

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.getWriter().write(
			gson.toJson(new ValidatorStatusCode(false, "Method not allowed"))
		);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String method = "doPost";
		response.setContentType("application/json;charset=UTF-8");
		int userId = SessionUtil.getUserId(request);
		int trySolverId = 0;
		try {
			trySolverId = new Integer(request.getParameter("id"));
		} catch (Exception e) {
			log.debug(method, e);
			response.setStatus(400);
			response.getWriter().write(
				gson.toJson(new ValidatorStatusCode(false, "bad id"))
			);
			return;
		}
		final int solverId = trySolverId;

		Solver solver = Solvers.get(solverId);
		if (solver == null || solver.getUserId() != userId) {
			response.setStatus(404);
			response.getWriter().write(
				gson.toJson(new ValidatorStatusCode(false, "Solver cannot be found"))
			);
			return;
		}

		List<Integer> spaces = Solvers.getAssociatedSpaceIds(solverId);
		if (spaces == null) {
			log.error(method, "spaces == null");
			response.setStatus(404);
			response.getWriter().write(
				gson.toJson(new ValidatorStatusCode(false, "Cannot find solver's parent space"))
			);
			return;
		}

		log.info(method, "Rebuilding solver: " + solverId);

		try {
			Common.update("{CALL RebuildSolver(?)}", p -> {
				p.setInt(1, solverId);
			});
		} catch (SQLException e) {
			log.error(method, e);
			response.setStatus(500);
			response.getWriter().write(
				gson.toJson(new ValidatorStatusCode(false, "Database error"))
			);
			return;
		}

		JobManager.addBuildJob(solverId, spaces.get(0));

		response.setStatus(200);
			response.getWriter().write(
				gson.toJson(new ValidatorStatusCode(true, ""))
			);
	}
}
