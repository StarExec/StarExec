<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		List<Queue> queues = Queues.getAll();
		
		if (!u.getRole().equals("admin")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} else {
			request.setAttribute("queues", queues);
		}		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/nodes, lib/jquery-ui-1.8.16.custom.min.js, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">
	<style>
		.statusConflict { color: red; }
		.statusClear {color : green; }
		.statusNeutral { color : black; }
	</style>
	<div id="explorer">
		<h3>queues</h3>
		<ul id="exploreList"></ul>
	</div>
	<div class="edit" id="div_1">Edit Me</div>
	<div id="detailPanel">
	<fieldset  id="nodeField">
		<legend class="expd" id="nodeExpd">nodes</legend>
		<table id="nodes">
			<thead>
				<tr>
					<th>date</th>
					<c:forEach items="${queues}" var="queue"> 
						<th>${queue.name}</th>
					</c:forEach>
					<th>total</th>
					<th class="statusConflict">conflict</th>
				</tr>
			</thead>			
		</table>
	</fieldset>
	</div>
	<div id="dialog-confirm-move" title="confirm move">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-move-txt"></span></p>
	</div>
</star:template>