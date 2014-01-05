<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		if (!u.getRole().equals("admin")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} else {
			
		}		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/cluster, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">
	<div id="explorer">
		<h3>queues</h3>
		<ul id="exploreList"></ul>
		<div id="explorerAction">
			<ul id="exploreActions">
				<li><a type="btnRun" id="newQueue" href="/${starexecRoot}/secure/add/queue.jsp">Add New Queue</a></li>
				<li><a type="btnRun" id="newPermanent" href="/${starexecRoot}/secure/add/permanentQueue.jsp">Add Permanent Queue</a></li>
			</ul>
		</div>
	</div>

	<div id="detailPanel">
		<fieldset>
		<legend>actions</legend>
			<ul id="actionList">
				<li><button type="button" id="removeQueue">remove queue</button></li>
				<li><a type="btnRun" id="manageNodes" href="/${starexecRoot}/secure/admin/nodes.jsp">Manage Nodes</a></li>
				<li><button type="button" id="makePermanent">make queue permanent</button></li>
							
			</ul>
		</fieldset>	
		<fieldset  id="reservationField">
			<legend class="expd" id="reservationExpd"><span>0</span> pending queue reservations</legend>
			<table id="qreserves">
				<thead>
					<tr>
						<th>user</th>
						<th>space</th>
						<th>nodes</th>
						<th>start</th>
						<th>end</th>
						<th>approve\decline</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
		<fieldset id="reservedField">
			<legend class="expd" id="reservedExpd"><span>0</span> currently reserved queues</legend>
			<table id="qreserved">
				<thead>
					<tr>
						<th>space</th>
						<th>queue</th>
						<th>nodes</th>
						<th>start</th>
						<th>end</th>
						<th>cancel</th>
					</tr>
				</thead>
			</table>
		</fieldset>
	</div>	
	<div id="dialog-confirm-remove" title="confirm removal">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-remove-txt"></span></p>
	</div>
	<div id="dialog-confirm-permanent" title="confirm make queue permanent">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-permanent-txt"></span></p>
	</div>
</star:template>