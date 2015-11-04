<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	int userId = SessionUtil.getUserId(request);
	if (Users.hasAdminReadPrivileges(userId)) {
		List<Space> spaces = Spaces.GetAllSpaces();
		List<WorkerNode> nodes = Cluster.getAllNodes();
		request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
		request.setAttribute("nodes", nodes);
		request.setAttribute("defaultTimeout", R.DEFAULT_MAX_TIMEOUT);
	} else {
		response.sendError(HttpServletResponse.SC_FORBIDDEN,"Invalid permissions");
	}
	
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="create queue" js="add/queue, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/add/queue" class="queue">
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
					<tr id="queueName" class="noHover" title="what would you like to name your queue?">
						<td class="label"><p>queue name</p></td>
						<td><input length="${queueNameLen}" id="txtQueueName" name="name" type="text"/></td>
						
					</tr>
					<tr class="noHover" title="the maximum cpu timeout that can be set for any job using this queue">
						<td class="label"><p>cpu timeout</p></td>
						<td><input value="${defaultTimeout}" name="cpuTimeout" type="text" id="cpuTimeoutText"/></td>
					</tr>	
					<tr class="noHover" title="the maximum wallclock timeout that can be set for any job using this queue">
						<td class="label"><p>wall timeout</p></td>
						<td><input value="${defaultTimeout}" name="wallTimeout" type="text" id="wallTimeoutText"/></td>
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
					</tr>
				</thead>	
				<tbody>
				<c:forEach var="n" items="${nodes}">
					<tr id="node_${n.id}">
						<td>
							<input type="hidden" name="node" value="${n.id}"/>
							<p>${n.name}</p>							
							
						</td>																		
					</tr>
				</c:forEach>
				</tbody>					
			</table>		
		</fieldset>
	<div id="actionBar">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><button type="submit" id="btnDone">Submit</button></li>
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
