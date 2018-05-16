<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.apache.commons.io.FileUtils, org.starexec.data.database.Permissions, org.starexec.data.database.Solvers,org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Configuration, org.starexec.data.to.Solver, org.starexec.util.SessionUtil, org.starexec.util.Util, java.io.File, org.starexec.logger.StarLogger"
        session="true" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	final StarLogger log = StarLogger.getLogger(getClass());

	try {
		// Grab relevant user id & configuration info
		int configId = Integer.parseInt((String) request.getParameter("id"));
		int userId = SessionUtil.getUserId(request);
		Configuration con = Solvers.getConfiguration(configId);
		Solver solver = null;

		// Gets parent solver if user has permission to view parent solver
		if (con != null && Permissions.canUserSeeSolver(con.getSolverId(), userId)) {
			solver = Solvers.get(con.getSolverId());
		} else {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"the configuration does not exist or is restricted"
			);
			return;
		}

		// The solver is valid...
		if (solver == null) {
			log.error("Cannot find solver", "configId: " + configId + "\tuserId: " + userId);
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"the solver does not exist or is restricted"
			);
			return;
		}

		// Build the configuration file path
		File configFile = new File(
				Util.getSolverConfigPath(solver.getPath(), con.getName()));

		// Ensure the configuration file exists on disk before assigning attributes
		if (!configFile.exists()) {
			log.error("configFile does not exist", "configId: " + configId + "\tuserId: " + userId + "\tconfigFile: " + configFile.getPath());
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"the configuration file path points to a location that does not exist on disk"
			);
			return;
		}

		con.setDescription(GeneralSecurity.getHTMLSafeString(
				con.getDescription()));
		String contents = FileUtils.readFileToString(configFile);
		contents = GeneralSecurity.getHTMLSafeString(contents);
		request.setAttribute("ownerId", solver.getUserId());
		request.setAttribute("config", con);
		request.setAttribute("solver", solver);
		request.setAttribute("contents", contents);
		request.setAttribute("isBinary", Util.isBinaryFile(configFile));
	} catch (Exception e) {
		log.error("Exception", e);
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
%>
<star:template title="${config.name}"
               css="details/configuration, details/shared, prettify/prettify"
               js="details/shared, lib/prettify, details/configuration">
	<input type="hidden" id="solverId" value="${solver.id}"/>
	<fieldset>
		<legend>details</legend>
		<table id="detailsTbl" class="shaded">
			<thead>
			<tr>
				<th class="label">attribute</th>
				<th>current value</th>
			</tr>
			</thead>
			<tbody>
			<tr>
				<td>name</td>
				<td>${config.name}</td>
			</tr>
			<tr>
				<td>description</td>
				<td>${config.description}</td>
			</tr>
			<tr>
				<td>owning solver</td>
				<td><star:solver value='${solver}'/></td>
			</tr>
			</tbody>
		</table>
	</fieldset>
	<fieldset id="configContents">
		<legend>contents</legend>
		<star:displayTextContents text="${contents}" isBinary="${isBinary}"/>
	</fieldset>
	<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"
		         style="float:left; margin:0 7px 20px 0;"></span><span
		         id="dialog-confirm-delete-txt"></span></p>
	</div>
	<c:if test="${ownerId == user.id}">
		<a id="deleteConfig">delete</a>
		<a id="editLink"
		   href="${starexecRoot}/secure/edit/configuration.jsp?id=${config.id}">edit</a>
	</c:if>
	<a href="${starexecRoot}/secure/details/solver.jsp?id=${solver.id}"
	   id="returnLink<c:if test="${ownerId != user.id}">Margin</c:if>">
		back to ${solver.name}</a>
</star:template>
