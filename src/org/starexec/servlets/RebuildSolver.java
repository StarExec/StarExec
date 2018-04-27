package org.starexec.servlets;

import org.starexec.constants.R;
import org.starexec.data.database.Common;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.security.JobSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Solver.ExecutableType;
import org.starexec.jobs.JobManager;
import org.starexec.logger.StarLogger;
import org.starexec.util.*;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Integer;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Rebuild an existing solver.
 * This can be usefull if StarExec's version of GCC is updated
 */
public class RebuildSolver extends HttpServlet {
	private static final StarLogger log = StarLogger.getLogger(RebuildSolver.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String method = "doPost";
		int userId = SessionUtil.getUserId(request);
		int trySolverId = 0;
		try {
			trySolverId = new Integer(request.getParameter("id"));
		} catch (Exception e) {
			log.error(method, e);
		}
		final int solverId = trySolverId;

		List<Integer> spaces = Solvers.getAssociatedSpaceIds(solverId);
		if (spaces == null) {
			log.error(method, "spaces == null");
		}

		log.info(method, "Rebuilding solver: " + solverId);

		try {
			Common.update("{CALL RebuildSolver(?)}", p-> {
				p.setInt(1, solverId);
			});
		} catch (SQLException e) {
			log.error(method, e);
		}

		JobManager.addBuildJob(solverId, spaces.get(0));

		response.setStatus(200);
		response.setContentType("text/text;charset=UTF-8");
		response.getWriter().write("");
	}
}
