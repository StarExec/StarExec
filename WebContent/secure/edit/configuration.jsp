<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.GeneralSecurity,org.starexec.constants.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
	// Grab relevant user id & configuration info
	request.setAttribute("configNameLen", R.CONFIGURATION_NAME_LEN);
	request.setAttribute("configDescLen", R.CONFIGURATION_DESC_LEN);
	int configId = Integer.parseInt((String)request.getParameter("id"));
	int userId = SessionUtil.getUserId(request);
	Configuration con = Solvers.getConfiguration(configId);
	Solver solver = Solvers.get(con.getSolverId());
	
	// Only permit the editing of the configuration file if the user
	// attempting the edit owns the parent solver
	if(solver.getUserId() != userId){
		solver = null;
	}
	
	// The solver and configuration objects are valid...
	if(con != null && solver != null) {
		
		// Build the configuration file path
		File configFile = new File(Util.getSolverConfigPath(solver.getPath(), con.getName()));
		
		// Ensure the configuration file exists on disk before assigning attributes
		if(configFile.exists()){
			con.setDescription(GeneralSecurity.getHTMLSafeString(con.getDescription()));
			String contents=GeneralSecurity.getHTMLSafeString(FileUtils.readFileToString(configFile));
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
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${config.name}" css="edit/configuration" js="lib/jquery.validate.min, edit/configuration">
	<form id="editConfigForm">
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
					<td><input id="name" type="text" name="name" maxlength="${configNameLen}" value="${config.name}"/></td>
				</tr>
				<tr>
					<td>description</td>
					<td><textarea id="description" name="description" length="${configDescLen}" >${config.description}</textarea></td>
				</tr>
				<tr>
					<td>contents</td>
					<td><textarea id="contents" name="contents">${contents}</textarea></td>
				</tr>
			</tbody>	
		</table>
	</fieldset>
	</form>
	<a type="button" id="cancelConfig" href="/${starexecRoot}/secure/details/configuration.jsp?id=${config.id}">cancel</a>
	<a type="button" id="updateConfig" href="#">update</a>
</star:template>