<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	int userId = SessionUtil.getUserId(request);
	List<Space> spaces = Spaces.GetAllSpaces();
	List<WorkerNode> nodes = Cluster.getAllNodes();
	request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
	request.setAttribute("nodes", nodes);
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="create permanent queue" js="add/permanentQueue, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/permanent/queue" class="queue">
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
					<tr id="queueName" class="noHover" title="what would you like to name your permanent queue?">
						<td class="label"><p>queue name</p></td>
						<td><input length="${queueNameLen}" id="txtQueueName" name="name" type="text"/></td>
					</tr>
					<tr id="nodeName" class="noHover" title="the node to reserve for this queue">
						<td class="label"><p>Node</p></td>
						<td>
							<select name="Nodes" id="nodes">
       							<c:forEach var="node" items="${nodes}">
           							 <option value="${node.name}">${node.name}</option>
        						</c:forEach>
    						</select>
    					</td>
					</tr>						
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