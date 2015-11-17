<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	int userId = SessionUtil.getUserId(request);
	if (Users.hasAdminReadPrivileges(userId)) {
		int id = Integer.parseInt(request.getParameter("id"));	
		Queue q = Queues.get(id);
		
		List<WorkerNode> nodes = Cluster.getNonAttachedNodes(id);
		request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
		request.setAttribute("queueName", q.getName());
		request.setAttribute("nodes", nodes);
	} else {
		response.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid permissions");
	}
	
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="move nodes to queue" js="util/selectBetween, admin/moveNodes, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
	<form id="addForm" method="POST" action="${starexecRoot}/secure/move/nodes" class="queue">
		<fieldset id="fieldStep1">
			<legend>Move Nodes to Queue</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr id="queueName" class="noHover" title="what would you like to name your queue?">
						<td class="label"><p>queue name</p></td>
						<td>
							<input type="hidden" name="name" value="${queueName}"/>
							<p>${queueName}</p></td>
					</tr>						
				</tbody>
			</table>
	</fieldset>
	<fieldset id="fieldSelectNodeSpace"> 
			<legend>node selection</legend>
			<table id="tblNodes" class="contentTbl">
				<thead>
					<tr>
						<th>node</th>
						<th>queue</th>
					</tr>
				</thead>	
				<tbody>
				<c:forEach var="n" items="${nodes}">
					<tr id="node_${n.id}">
						<td>
							<input type="hidden" name="node" value="${n.id}"/>
							<p>${n.name}</p>							
							
						</td>		
						<td>
							<p>${n.queue.name}</p>
						</td>																
					</tr>
				</c:forEach>
				</tbody>					
			</table>		
			<span id="selectBetween">Select Rows Between</span>
		</fieldset>
	<div id="actionBar">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><button type="submit" id="btnDone">Move Nodes</button></li>
			</ul>
		</fieldset>		
	</div>
		
</form>

	<c:if test="${not empty param.result and param.result == 'requestSent'}">			
		<div class='success message'>request sent successfully - you will receive an email when a leader of that community approves/declines your request</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">			
		<div class='error message'>you are already a member of that community, or have already requested to be and are awaiting approval</div>
	</c:if>
</star:template>
