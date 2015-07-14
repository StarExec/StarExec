<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int userId = SessionUtil.getUserId(request);
		if (!Users.hasAdminReadPrivileges(userId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} 	
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/community, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared, explore/common, admin/admin">
	<div id="explorer">
		<h3>official</h3>
		<ul id="exploreList"></ul>
		<div id="explorerAction">
			<ul id="exploreActions">
				<li><a type="btnRun" id="newCommunity" href="/${starexecRoot}/secure/add/space.jsp">Add New Community</a></li>
			</ul>
		</div>
	</div>

	<div id="detailPanel">
		<fieldset>
		<legend>actions</legend>
			<ul id="actionList">
				<li><a type="btnRun" id="removeCommLeader" href="/${starexecRoot}/secure/edit/community.jsp">remove community leader</a></li>
				<li><a type="btnRun" id="promoteCommLeader" href="/${starexecRoot}/secure/edit/community.jsp">promote member to leader</a>			
			</ul>	
		</fieldset>	
		<fieldset  id="communityField">
			<legend class="expd" id="communityExpd"><span>0</span> pending community requests</legend>
			<table id="commRequests">
				<thead>
					<tr>
						<th>user</th>
						<th>community</th>
						<th>message</th>
						<th>approve</th>
						<th>decline</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
	</div>	
</star:template>
