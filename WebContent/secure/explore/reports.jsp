<%@page contentType="text/html" pageEncoding="UTF-8" 
import="org.starexec.data.database.*, 
		org.starexec.data.to.*,
		org.starexec.constants.*, 
		org.starexec.util.*, 
		java.util.*" 
%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		User currentUser = Users.get(userId);
		// Get all the report data.
		List<Report> reportsNotRelatedToQueues = Reports.getAllReportsNotRelatedToQueues();
		List<List<Report>> reportsForAllQueues = Reports.getAllReportsForAllQueues();

		String subscribeUnsubscribeButtonId = "";
		String subscribeUnsubscribeButtonMessage = "";
		// Set the subscribe/unsubscribe button's attributes depending on if user is subscribed to reports or not.
		if (currentUser.isSubscribedToReports()) {
			subscribeUnsubscribeButtonId = "unsubscribe";
			subscribeUnsubscribeButtonMessage = "unsubscribe from weekly report emails";
		} else {
			subscribeUnsubscribeButtonId = "subscribe";
			subscribeUnsubscribeButtonMessage = "subscribe to weekly report emails";
		}
		request.setAttribute("reportsNotRelatedToQueues", reportsNotRelatedToQueues);
		request.setAttribute("reportsForAllQueues", reportsForAllQueues);
		request.setAttribute("subscribeUnsubscribeButtonId", subscribeUnsubscribeButtonId);
		request.setAttribute("subscribeUnsubscribeButtonMessage", subscribeUnsubscribeButtonMessage);
		request.setAttribute("userId", userId);
		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="Reports" js="explore/reports" css="explore/reports, common/table, details/shared,explore/jquery.qtip, explore/common">			
	<div id="mainPanel">
		<span id="userId" value="${userId}"></span>
		<div id="subscribeUnsubscribeButtonContainer">
			<input id="${subscribeUnsubscribeButtonId}" type="button" value="${subscribeUnsubscribeButtonMessage}">
		</div>
		<fieldset id="mainReports">
			<legend>main reports</legend>
			<table id="mainReportsTable">
				<thead>
					<tr>
						<th>event</th>
						<th>occurrences</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${reportsNotRelatedToQueues}" var="report">
						<tr><td><c:out value="${report.getEventName()}"/></td><td><c:out value="${report.getOccurrences()}"/></td></tr>
					</c:forEach>
				</tbody>
			</table>
		</fieldset>
		<c:forEach items="${reportsForAllQueues}" var="reportsForOneQueue">
		<fieldset>
			<legend><c:out value="reports for ${reportsForOneQueue.get(0).getQueueName()}"/></legend>
			<table>
				<thead>
					<th>event</th>
					<th>occurrences</th>
				</thead>
				<tbody>
					<c:forEach items="${reportsForOneQueue}" var="report">
					<tr><td><c:out value="${report.getEventName()}"/></td><td><c:out value="${report.getOccurrences()}"/></td></tr>
					</c:forEach>
				</tbody>
			</table>
		</fieldset>
		</c:forEach>
	</div>	
</star:template>
