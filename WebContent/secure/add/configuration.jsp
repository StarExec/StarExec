<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		// Gather solver and user information
		int solverId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		
		// Verify this user is the owner of the solver they are trying to upload configurations to
		if(solver.getUserId() == userId) {
			request.setAttribute("solver", solver);
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add configurations here.");
		}
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent solver id was not in the correct format.");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to upload configurations to this solver or the solver does not exist");		
	}
%>
<star:template title="add to ${solver.name}" css="add/configuration" js="lib/jquery.validate.min, add/configuration">
	<form method="POST" enctype="multipart/form-data" action="/starexec/secure/upload/configurations" id="uploadConfigForm">
		<input type="hidden" name="solverId" value="${solver.id}"/>
		<fieldset>
			<legend>configuration information</legend>		
			<table id="configTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>configuration location</td>
						<td><input id="configFile" name="file" type="file" size="64"/></td>
					</tr>
					<tr>
						<td>configuration name</td>
						<td><input id="configName" name="name" type="text" size="64"/></td>
					</tr>
					<tr>
						<td>configuration description</td>
						<td><textarea id="configDesc" name="description" rows="6" cols="40"></textarea></td>
					</tr>
				</tbody>
			</table>	
			<button id="cancelBtn" type="button">cancel</button>																
			<button id="uploadBtn" type="submit">upload</button>
		</fieldset>
	</form>
</star:template>