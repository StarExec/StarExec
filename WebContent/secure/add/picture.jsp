<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		User user = SessionUtil.getUser(request);
		int userId = user.getId();
		
		String type = request.getParameter("type").toString();
		String Id =  request.getParameter("Id").toString();
		
		if (Validator.isValidPictureType(type) && Validator.isValidInteger(Id)) {
			request.setAttribute("userId", userId);
			request.setAttribute("Id", Id);
			request.setAttribute("type", type);
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The image parameters were invalid");
		}
		

	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
	}
%>

<star:template title="upload a picture" css="add/picture" js="lib/jquery.validate.min, add/picture, lib/jquery.qtip.min">
	<form method="POST" enctype="multipart/form-data" action="${starexecRoot}/secure/upload/pictures" id="upForm">
			<input type="hidden" name="type" value="${type}"/>
	        <input type="hidden" name="Id" value="${Id}"/>
		<fieldset>
			<legend>picture information</legend>		
			<table id="tblSolver" class="shaded">
				<tr></tr>
				<tr>
					<td>picture location</td>
					<td><input id="uploadPic" name="f" type="file" /></td>
				</tr>
					<td colspan="2"><button id="btnUpload" type="submit">upload</button></td>
				</tr>
			</table>																	
		</fieldset>
	</form>
</star:template>