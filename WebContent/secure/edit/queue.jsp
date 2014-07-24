<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int queueId = Integer.parseInt(request.getParameter("id"));

		Queue q= null;
		if(Users.isAdmin(userId)) {
			q=Queues.get(queueId);
		}

		if(q != null) {
			request.setAttribute("queue", q);
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this page.");

		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given queue id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="edit ${queue.name}" js="lib/jquery.validate.min, edit/queue" css="edit/shared, edit/queue">				
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
						<td><input id="cpuTimeout" type="text" name="cpuTimeout" value="${queue.cpuTimeout}"/></td>
					</tr>
					<tr>
						<td class="label">wallclock timeout</td>			
						<td><input id="wallTimeout" type="text" name="wallTimeout" value="${queue.wallTimeout}"/></td>
					</tr>																		
				</tbody>
			</table>	
			<button type="button" id="update">update</button>
		</fieldset>		
	</form>

</star:template>