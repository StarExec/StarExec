<%@page contentType="text/html" pageEncoding="UTF-8" import="java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*,org.starexec.data.security.GeneralSecurity, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
		
	// Grab relevant user id & configuration info
	int configId = Integer.parseInt((String)request.getParameter("id"));
	int userId = SessionUtil.getUserId(request);
	Configuration con = Solvers.getConfiguration(configId);
	Solver solver = null;
	
	// Gets parent solver if user has permission to view parent solver
	if(Permissions.canUserSeeSolver(con.getSolverId(), userId)){
		solver = Solvers.get(con.getSolverId());
	}
	
	// The solver and configuration objects are valid...
	if(con != null && solver != null) {
		
		// Build the configuration file path
		File configFile = new File(Util.getSolverConfigPath(solver.getPath(), con.getName()));
		
		// Ensure the configuration file exists on disk before assigning attributes
		if(configFile.exists()){
			con.setDescription(GeneralSecurity.getHTMLSafeString(con.getDescription()));
			String contents=GeneralSecurity.getHTMLSafeString(FileUtils.readFileToString(configFile));
			request.setAttribute("ownerId", solver.getUserId());
			request.setAttribute("config", con);
			request.setAttribute("solver", solver);
			request.setAttribute("contents", contents);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "the configuration file path points to a location that does not exist on disk");
		}
	} else {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "the configuration does not exist or is restricted");
	}
} catch (Exception e) {
	e.printStackTrace();
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${config.name}" css="details/configuration, details/shared" js="details/configuration, details/shared">
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
		<textarea id="contents" readonly="readonly">${contents}</textarea>	
	</fieldset>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<c:if test="${ownerId == user.id}">
		<a id="deleteConfig">delete</a>
		<a id="editLink" href="/${starexecRoot}/secure/edit/configuration.jsp?id=${config.id}">edit</a>
	</c:if>
	<a href="/${starexecRoot}/secure/details/solver.jsp?id=${solver.id}" id="returnLink<c:if test="${ownerId != user.id}">Margin</c:if>">back to ${solver.name}</a>
</star:template>