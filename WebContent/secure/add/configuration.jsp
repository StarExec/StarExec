<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB,org.starexec.data.database.Solvers,org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Solver, org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		// Gather solver and user information
		int solverId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		request.setAttribute("configNameLen", DB.CONFIGURATION_NAME_LEN - 4);
		request.setAttribute("configDescLen", DB.CONFIGURATION_DESC_LEN);
		// Verify this user is the owner of the solver they are trying to upload configurations to
		if (solver.getUserId() == userId ||
				GeneralSecurity.hasAdminReadPrivileges(userId)) {
			request.setAttribute("solver", solver);
		} else {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"You do not have permission to add configurations here."
			);
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The parent solver id was not in the correct format."
		);
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"You do not have permission to upload configurations to this solver or the solver does not exist"
		);
	}
%>
<star:template title="add to ${solver.name}" css="add/configuration"
               js="lib/jquery.validate.min, add/configuration">
	<form method="POST" enctype="multipart/form-data"
	      action="${starexecRoot}/secure/upload/configurations"
	      id="uploadConfigForm">
		<input type="hidden" name="solverId" value="${solver.id}"/>
		<fieldset id="upload">
			<legend>upload a configuration</legend>
			<table id="uploadConfigTable" class="shaded">
				<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>file location</td>
					<td><input id="configFile" name="file" type="file"
					           size="64"/></td>
				</tr>
				<tr>
					<td>configuration name</td>
					<td><input id="uploadConfigName" name="uploadConfigName"
					           type="text" size="64"
					           maxlength="${configNameLen}"/></td>
				</tr>
				<tr>
					<td>configuration description</td>
					<td><textarea id="uploadConfigDesc" name="uploadConfigDesc"
					              rows="6" cols="40"
					              maxlength="${configDescLen}"></textarea></td>
				</tr>
				</tbody>
			</table>
			<button class="cancelBtn" type="button">cancel</button>
			<button class="uploadBtn" type="submit">upload</button>
		</fieldset>
	</form>
	<form method="POST" action="${starexecRoot}/secure/save/configurations"
	      id="saveConfigForm">
		<input type="hidden" name="solverId" value="${solver.id}"/>
		<fieldset id="save">
			<legend>write a configuration</legend>
			<table id="saveConfigTable" class="shaded">
				<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>configuration name</td>
					<td><input id="saveConfigName" name="saveConfigName"
					           type="text" size="64"
					           maxlength="${configNameLen}"/></td>
				</tr>
				<tr>
					<td>configuration description</td>
					<td><textarea id="saveConfigDesc" name="saveConfigDesc"
					              rows="6" cols="40"
					              maxlength="${configDescLen}"></textarea></td>
				</tr>
				<tr>
					<td>configuration contents</td>
					<td><textarea id="saveConfigContents"
					              name="saveConfigContents" rows="6" cols="40">
					</textarea></td>
				</tr>
				</tbody>
			</table>
			<button class="cancelBtn" type="button">cancel</button>
			<button class="saveBtn" type="submit">save</button>
		</fieldset>
	</form>
</star:template>
