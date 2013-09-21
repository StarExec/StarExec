<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, java.text.SimpleDateFormat, java.sql.Date, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	String code = request.getParameter("code");
	int userId = SessionUtil.getUserId(request);
	List<WorkerNode> nodes = Queues.getNodes(1);
	QueueRequest req = Requests.getQueueRequest(code);

	User u = Users.get(userId);
	if (! u.getRole().equals("admin")) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
	} else {
		request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
		request.setAttribute("nodes", nodes);
		if (code != null) {
			
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
			Date start = req.getStartDate();
			Date end = req.getEndDate();
			String start1 = sdf.format(start);
			String end1 = sdf.format(end);

			request.setAttribute("queueName", req.getQueueName());
			request.setAttribute("message", req.getMessage());
			request.setAttribute("code", req.getCode());
			request.setAttribute("userId", req.getUserId());
			request.setAttribute("spaceId", req.getSpaceId());
			request.setAttribute("nodeCount" , req.getNodeCount());
			request.setAttribute("start", start1);
			request.setAttribute("end", end1);
			nodes = Cluster.getUnReservedNodes(start, end);
			request.setAttribute("nodes", nodes);
		}

	}		
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="create queue" css="explore/common, common/table, add/queue" js="lib/jquery.validate.min, add/queue, lib/jquery.dataTables.min, lib/jquery.qtip.min">	
	<style>
		.statusConflict { color: red; }
		.statusClear {color : green; }
		.statusNeutral { color : black; }
	</style>
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/add/queue" class="queue">	
		<input type="hidden" name="spaceId" value="${spaceId}"/>
		<input type="hidden" name="userId" value="${userId}"/>
		<input type="hidden" name="code" value="${code}"/>
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
						<c:if test="${not empty param.code}">
							<td><input id="name" type="text" name="name" value="${queueName}"></td>
						</c:if>
						<c:if test="${empty param.code}">
							<td><input length="${queueNameLen}" id="txtQueueName" name="name" type="text"/></td>
						</c:if>
					</tr>
					<c:if test="${not empty param.code}">
						<tr class="noHover" title="user's reason for requesting this queue.">
							<td class="label"><p>message</p></td>
							<td id="msg">${message}</td>
						</tr>
						<tr class="noHover" title="when would you like to begin your reservation?">
						<td class="label"><p>start date</p></td>
						<td>
							<SCRIPT ID="js1">
								var now = new Date();
								var cal1 = new CalendarPopup();
								cal1.addDisabledDates(null,formatDate(now,"yyyy-MM-dd"));
							</SCRIPT>
							<INPUT TYPE="text" ID="start" NAME="start" VALUE="${start}" SIZE=25>
							<A HREF="#" onClick="cal1.select(document.forms[0].start,'anchor1','MM/dd/yyyy'); return false;" NAME="anchor1" ID="anchor1">select</A>		
						</td>						
						</tr>
						<tr class="noHover" title="when would you like to end your reservation?">
							<td class="label"><p>end date</p></td>
							<td>
								<SCRIPT ID="js2">
									var cal2 = new CalendarPopup();
								</SCRIPT>
								<INPUT TYPE="text" ID="end" NAME="end" VALUE="${end}" SIZE=25>
								<A HREF="#" onClick="cal1.select(document.forms[0].end,'anchor2','MM/dd/yyyy',(document.forms[0].end.value=='')?document.forms[0].start.value:null); return false;" NAME="anchor2" ID="anchor2">select</A>		 
							</td>						
						</tr>
					</c:if>
						
				</tbody>
			</table>
	</fieldset>	
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
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnNext">next</button>			
			<button type="button" class="round" id="btnPrev">prev</button>	
			<button type="button" class="round" id="btnBack">cancel</button>
			<c:if test="${not empty param.code}">
				<button type="button" class="round" id="btnDecline">decline</button>
			</c:if>							
		</div>	
	</form>

	<c:if test="${not empty param.result and param.result == 'requestSent'}">			
		<div class='success message'>request sent successfully - you will receive an email when a leader of that community approves/declines your request</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">			
		<div class='error message'>you are already a member of that community, or have already requested to be and are awaiting approval</div>
	</c:if>
</star:template>