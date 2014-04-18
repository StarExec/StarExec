<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, java.text.SimpleDateFormat,java.util.Date, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		//List<Queue> queues = Queues.getAllAdmin();
		List<Queue> queues = Queues.getAllNonPermanent();
		
		Date latest = Cluster.getLatestNodeDate();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		String date1 = sdf.format(latest);
		
		if (!u.getRole().equals("admin")) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} else {
			request.setAttribute("queues", queues);
			request.setAttribute("defaultQueueName", R.DEFAULT_QUEUE_NAME);
			request.setAttribute("latest", date1);
		}		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="manage nodes" js="admin/nodes, lib/jquery-ui-1.8.16.custom.min, lib/jquery.dataTables.min, lib/jquery.jeditable, lib/jquery.validate.min, lib/jquery.dataTables.editable, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/table, details/shared, admin/admin">
	<style>
		.statusConflict { color: red; }
		.statusClear {color : green; }
		.statusZero {color : yellow; }
		.statusNeutral { color : black; }
		
	</style>
	<div id="actionBar">
			<button type="button" class="round" id="btnBack">cancel</button>
			<button type="button" class="update" id="btnUpdate">update</button>			
	</div>
	<div style="width: 100%; overflow: auto;">
			<fieldset id="fieldStep1">
			<legend>Adjust Dates</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr class="noHover" title="view to this date: ">
						<td class="label"><p>View to this date:</p></td>
						<td><input id="last" name="last" type="text" value="${latest}"/> </td>											
					</tr>		
				</tbody>
			</table>
			<div>
				<button type="button" class="update" id="btnDateChange">update</button>
			</div>
	</fieldset>
	<fieldset  id="nodeField">
		<legend class="expd" id="nodeExpd">nodes</legend>
		<table id="nodes" class="manage">
			<thead>
				<tr>
					<th style="width: 100px;">date</th>
					<th style="width: 100px">${defaultQueueName}</th>
					<c:forEach items="${queues}" var="queue"> 
						<th style="width: 100px">${queue.name}</th>
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