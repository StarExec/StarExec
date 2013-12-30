<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, java.text.SimpleDateFormat, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	String code = request.getParameter("code");
	int userId = SessionUtil.getUserId(request);
	QueueRequest req = Requests.getQueueRequest(code);
	//List<Queue> queues = Queues.getAllAdmin();
	List<Queue> queues = Queues.getAllNonPermanent();



	User u = Users.get(userId);
	if (! u.getRole().equals("admin")) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
	} else {
		request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
		request.setAttribute("queues", queues);
			
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		java.util.Date start = req.getStartDate();
		java.util.Date end = req.getEndDate();
		String start1 = sdf.format(start);
		String end1 = sdf.format(end);
		java.util.Date today = new java.util.Date();

		if (start.before(today)) {
			request.setAttribute("isExpired", true);
		}
		request.setAttribute("queueName", req.getQueueName());
		request.setAttribute("message", req.getMessage());
		request.setAttribute("code", req.getCode());
		request.setAttribute("userId", req.getUserId());
		request.setAttribute("spaceId", req.getSpaceId());
		request.setAttribute("nodeCount" , req.getNodeCount());
		request.setAttribute("start", start1);
		request.setAttribute("end", end1);
	}		
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="create queue" css="admin/admin, explore/common, common/table, add/queue" js="admin/queue, lib/jquery-ui-1.8.16.custom.min, lib/jquery.dataTables.min, lib/jquery.jeditable, lib/jquery.validate.min, lib/jquery.dataTables.editable, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min">	
	<style>
		.statusConflict { color: red; }
		.statusClear {color : green; }
		.statusNeutral { color : black; }
	</style>
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/add/queue" class="queue">	
		<input type="hidden" name="spaceId" value="${spaceId}"/>
		<input type="hidden" name="userId" value="${userId}"/>
		<input type="hidden" name="code" value="${code}"/>
		<input type="hidden" name="nodecount" value="${nodeCount}"/>
		<input type="hidden" name="start" value="${start}"/>
		<input type="hidden" name="end" value="${end}"/>
		<input type="hidden" name="queueName" value="${queueName}"/>
		<c:if test="${isExpired}" >
			<p id="expireNote"> THIS REQUEST HAS EXPIRED. ADJUST DATES ACCORDINGLY <p>
		</c:if>
		<fieldset id="fieldStep1">
			<legend>Add a Queue</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr class="noHover" title="what would you like to name your queue?">
						<td class="label"><p>queue name</p></td>
						<td><input id="queueName" name="queueName" type="text" value="${queueName}"/> </td>					
					</tr>
					<tr class="noHover" title="user's reason for requesting this queue.">
						<td class="label"><p>message</p></td>
						<td id="msg">${message}</td>
					</tr>
					<tr class="noHover" title="number of nodes to reserve">
						<td class="label"><p>max node count</p></td>
						<td><input id="nodeCount" name="nodeCount" type="text" value="${nodeCount}"/> </td>											
					</tr>
					<tr class="noHover" title="when would you like to begin your reservation?">
						<td class="label"><p>start date</p></td>
						<td><input id="start" name="start" type="text" value="${start}"/> </td>											
					</tr>
					<tr class="noHover" title="when would you like to end your reservation?">
						<td class="label"><p>end date</p></td>
						<td><input id="end" name="end" type="text" value="${end}"/> </td>					
					</tr>		
				</tbody>
			</table>
			<div>
				<button type="button" class="update" id="btnUpdate">update</button>
			</div>
	</fieldset>
	
	<div style="width: 100%; overflow: auto; margin-bottom: 20px" id="nodeTableDiv">
	<fieldset>
		<legend class="expd" id="nodeExpd">nodes</legend>
		<table id="nodes">
			<thead>
				<tr>				
					<th style="width: 100px;">date</th>
					<c:forEach items="${queues}" var="queue"> 
						<th style="width: 100px;">${queue.name}</th>
					</c:forEach>
					<th style="width: 100px;" id="qName">${code}</th>
					<th>total</th>
					<th class="statusConflict">conflict</th>
				</tr>
			</thead>			
		</table>
	</fieldset>
	</div>
	<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnBack">cancel</button>
			<button type="button" class="round" id="btnDecline">decline</button>
	</div>	
	</form>

	<c:if test="${not empty param.result and param.result == 'requestSent'}">			
		<div class='success message'>request sent successfully - you will receive an email when a leader of that community approves/declines your request</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">			
		<div class='error message'>you are already a member of that community, or have already requested to be and are awaiting approval</div>
	</c:if>
	<div id="dialog-warning" title="warning">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>
</star:template>