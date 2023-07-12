<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Queues, org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Queue,org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int queueId = Integer.parseInt(request.getParameter("id"));

		Queue q = null;
		if (GeneralSecurity.hasAdminReadPrivileges(userId)) {
			q = Queues.get(queueId);
		}

		if (q != null) {
			request.setAttribute("queue", q);
		} else {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"You do not have permission to view this page."
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given queue id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>

<star:template title="edit ${queue.name}"
               js="lib/jquery.validate.min, edit/queue"
               css="edit/shared, edit/queue">
	<form id="editQueueForm">
		<input id="name" type="hidden" name="name" value="${queue.name}">
		<fieldset>
			<legend>queue details</legend>
			<table class="shaded">
				<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td class="label">cpu timeout</td>
					<td><input id="cpuTimeout" type="text" name="cpuTimeout"
					           value="${queue.cpuTimeout}"/></td>
				</tr>
				<tr>
					<td class="label">wallclock timeout</td>
					<td><input id="wallTimeout" type="text" name="wallTimeout"
					           value="${queue.wallTimeout}"/></td>
				</tr>
				<tr>
					
					<td class="label">
						<div>
							<span>description</span>
							<br>
							<span id="descCharRemaining" ></span>
						</div>
					</td>
					<td><textarea name="description" rows="4" cols="50" maxlength="200" id="descTextBox" oninput="onDescBoxUpdate()" value="" >${queue.getDesc()}</textarea>
				</tr>
				</tbody>
			</table>
			<button type="button" id="update">update</button>
		</fieldset>
	</form>

</star:template>
