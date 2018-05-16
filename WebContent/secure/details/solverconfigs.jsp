<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Permissions,org.starexec.data.database.Solvers, org.starexec.data.to.Configuration, org.starexec.data.to.Solver, org.starexec.util.SessionUtil, java.io.OutputStreamWriter, java.util.List" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- handles requests to print out solver configurations
currently only used in StarexecCommand --%>
<%
	try {
		int solverid = Integer.parseInt(request.getParameter("solverid"));
		int userId = SessionUtil.getUserId(request);
		if (!Permissions.canUserSeeSolver(solverid, userId)) {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN, "Invalid Permissions");
			return;
		} else {
			int limit = Integer.parseInt(request.getParameter("limit"));


			Solver s = Solvers.get(solverid);

			List<Configuration> cs = Solvers.getConfigsForSolver(solverid);

			StringBuilder str = new StringBuilder();
			int count = 0;

			for (Configuration c : cs) {
				str.append("id=").append(c.getId()).append(" : name=")
				   .append(c.getName()).append("\n");
				count++;
				if (count == limit) {
					break;
				}
			}

			OutputStreamWriter writer =
					new OutputStreamWriter(response.getOutputStream());
			writer.write(str.toString());
			writer.flush();
			writer.close();
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given solver id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Life, Jim, but not as we know it"
		);
		return;
	}
%>
