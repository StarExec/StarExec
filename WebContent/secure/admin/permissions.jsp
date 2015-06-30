<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int id = Integer.parseInt(request.getParameter("id"));
		int userId = SessionUtil.getUserId(request);
		if (!Users.isAdmin(userId) && !Users.isDeveloper(userId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} else {
			request.setAttribute("userId", id);
		}		
				
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/permissions, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared, explore/common, admin/admin">
	<div id="explorer">
		<h3>official</h3>
		<ul id="exploreList"></ul>
	</div>

	<div id="actionPanel">
		<fieldset id="fieldStep1">
			<legend>permissions</legend>
			<table id="tblDefaultPerm">
				<tr>
					<th></th>
					<th>solver</th>
					<th>bench</th>
					<th>users</th>
					<th>space</th>
					<th>jobs</th>
				</tr>
				<tr>
					<td>add</td>
					<td><input type="checkbox" id ="addSolver" name="addSolver"/></td>
					<td><input type="checkbox" id = "addBench" name="addBench"/></td>
					<td><input type="checkbox" id = "addUser" name="addUser"/></td>
					<td><input type="checkbox" id = "addSpace" name="addSpace"/></td>
					<td><input type="checkbox" id = "addJob" name="addJob"/></td>
				</tr>
				<tr>
					<td>remove</td>
					<td><input type="checkbox" id = "removeSolver" name="removeSolver"/></td>
					<td><input type="checkbox" id = "removeBench" name="removeBench"/></td>
					<td><input type="checkbox" id = "removeUser" name="removeUser"/></td>
					<td><input type="checkbox" id = "removeSpace" name="removeSpace"/></td>
					<td><input type="checkbox" id = "removeJob" name="removeJob"/></td>
				</tr>
				<tr>
					<td>leader</td>
					<td><input type="checkbox" id = "isLeader" name="isLeader"/></td>
				</tr>				
			</table>
			<button type="button" id="editPermissions">Update</button>	
			<div id="dialog-confirm-update" title="confirm update">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-update-txt"></span></p>
			</div>
				
		</fieldset>	
		<fieldset id="fieldStep2">
			<legend>permissions</legend>
			<p> USER IS NOT A MEMBER OF THIS CLASS</p>
			<button type="button" id="makeMember">Make Member</button>	

		</fieldset>
	</div>	
</star:template>
