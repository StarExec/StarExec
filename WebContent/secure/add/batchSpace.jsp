<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		
		// Verify this user can add spaces to this space
		Permission userPerm = SessionUtil.getPermission(request, spaceId);
		if(userPerm.canAddSpace()) {
			request.setAttribute("space", Spaces.get(spaceId));
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add spaces here");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to upload spaces to this space or the space does not exist");		
	}
%>
<star:template title="upload XML representation of space hierarchy to ${space.name}" css="common/delaySpinner, add/solver" js="common/delaySpinner, lib/jquery.cookie, lib/jquery.validate.min, add/batchSpace">
	<form method="POST" enctype="multipart/form-data" action="/${starexecRoot}/secure/upload/space" id="upForm">
		<input type="hidden" name="space" value="${space.id}"/>
		<fieldset>
			<legend>upload your compressed file </legend>		
			<table id="tblXML" class="shaded">
				<tr></tr>
				<tr>
					<td>file location</td>
					<td><input id="fileUpload" name="f" type="file" /></td>
				</tr>
					<td colspan="2"><button id="btnUpload" type="submit">upload</button></td>
				</tr>
			</table>																	
		</fieldset>
	</form>
</star:template>