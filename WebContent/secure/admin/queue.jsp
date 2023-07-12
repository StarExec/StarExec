<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB, org.starexec.constants.R, org.starexec.data.database.Cluster, org.starexec.data.database.Spaces, org.starexec.data.to.Space, org.starexec.data.to.WorkerNode, org.starexec.util.SessionUtil, java.util.List" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		List<Space> spaces = Spaces.getAllSpaces();
		List<WorkerNode> nodes = Cluster.getAllNodes();
		request.setAttribute("queueNameLen", DB.QUEUE_NAME_LEN);
		request.setAttribute("nodes", nodes);
		request.setAttribute("defaultTimeout", R.DEFAULT_MAX_TIMEOUT);
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given user id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="create queue"
               js="add/queue, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min"
               css="common/table, details/shared, explore/common, explore/spaces, admin/admin">
	<form id="addForm" method="POST" action="${starexecRoot}/secure/add/queue"
	      class="queue">
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
				<tr id="queueName" class="noHover"
				    title="what would you like to name your queue?">
					<td class="label"><p>queue name</p></td>
					<td><input length="${queueNameLen}" id="txtQueueName"
					           name="name" type="text"/></td>

				</tr>
				<tr class="noHover"
				    title="the maximum cpu timeout that can be set for any job using this queue">
					<td class="label"><p>cpu timeout</p></td>
					<td><input value="${defaultTimeout}" name="cpuTimeout"
					           type="text" id="cpuTimeoutText"/></td>
				</tr>
				<tr class="noHover"
				    title="the maximum wallclock timeout that can be set for any job using this queue">
					<td class="label"><p>wall timeout</p></td>
					<td><input value="${defaultTimeout}" name="wallTimeout"
					           type="text" id="wallTimeoutText"/></td>
				</tr>
				<tr title="the description you would like for the queue">
					<td>
						<div>
							<span>description</span>
							<br>
							<span id="descCharRemaining"></span>
						</div>
					</td>
					<td>
						<textarea name="description" rows="4" cols="50" maxlength="200" id="descTextBox" oninput="onDescBoxUpdate()" value="" >${queue.getDesc()}</textarea>
					</td>
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
		<fieldset id="numberOfJobsField">
			<legend>jobs per node</legend>
			Select number of jobs run per node:
			<select id="numberOfJobs" name="numberOfJobs">
				<option value="1">1</option>
				<option selected="selected" value="2">2</option>
			</select>
		</fieldset>
		<div id="actionBar">
			<fieldset>
				<legend>actions</legend>
				<ul id="actionList">
					<li>
						<button type="submit" id="btnDone">Submit</button>
					</li>
				</ul>
			</fieldset>
		</div>
	</form>
	<c:if test="${not empty param.result and param.result == 'requestSent'}">
		<div class='success message'>request sent successfully - you will
			receive an email when a leader of that community approves/declines
			your request
		</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">
		<div class='error message'>you are already a member of that community,
			or have already requested to be and are awaiting approval
		</div>
	</c:if>
</star:template>
