<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		// Get parent space info for display
		long spaceId = Long.parseLong(request.getParameter("sid"));
		long userId = SessionUtil.getUserId(request);
		request.setAttribute("space", Database.getSpace(spaceId, userId));
		
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if(!p.canAddSolver()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add solvers here");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to upload solvers to this space or the space does not exist");		
	}
%>
<star:template title="upload solver to ${space.name}" css="upload_solver" js="lib/jquery.validate.min, upload_solver">
	<form method="POST" enctype="multipart/form-data" action="/starexec/UploadSolver" id="upForm">
		<input type="hidden" name="space" value="${space.id}"/>
		<fieldset>
			<legend>solver information</legend>		
			<table>
				<tr>
					<td>solver location</td>
					<td><input name="f" type="file" /></td>
				</tr>
				<tr>
					<td>solver name</td>
					<td><input name="sn" type="text" size="42" /></td>
				</tr>
				<tr>
					<td>solver description</td>
					<td><textarea rows="6" cols="40" name="desc"></textarea></td>
				</tr>
				<tr>
					<td>downloadable</td>
					<td>
						<input name="dlable" type="radio" value="true" checked="checked" /><label>yes</label>
						<input name="dlable" type="radio" value="false" /><label>no</label>
					</td>
			</table>														
			<button id="submit" type="submit" class="round">submit</button>
		</fieldset>
	</form>
</star:template>