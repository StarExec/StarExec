<%@page contentType="text/html" pageEncoding="UTF-8" 
import="org.apache.commons.io.FileUtils,
		org.starexec.data.database.*, 
		org.starexec.data.to.*,
		org.starexec.constants.*, 
		org.starexec.util.*, 
		java.io.File,
		java.text.ParseException,
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
		
		File pastReportsDirectory = new File(R.STAREXEC_DATA_DIR, "/reports/");
		List<File> pastReports = (List)FileUtils.listFiles(pastReportsDirectory, new String[]{"txt"}, false);

		// Get the list into the correct order so most recent reports will be on top on the page.
		Collections.sort(pastReports);
		Collections.reverse(pastReports);

		// Get the name of the last report and remove the file ending to get a string representation of the last
		// reports date. 
		String lastReportDay = "";
		if (!pastReports.isEmpty()) { 
			File lastReport = pastReports.get(0);
			lastReportDay = lastReport.getName().replace(".txt", "");
		}


		// Only add "since" to the title suffix if their is a last report.	
		String titleSuffix = "";
		if (!lastReportDay.equals("")) {
			titleSuffix = "since " + lastReportDay;
		}

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
		request.setAttribute("pastReports", pastReports);
		request.setAttribute("titleSuffix", titleSuffix);
		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="Reports ${titleSuffix}" js="explore/reports" css="explore/reports, common/table, details/shared,explore/jquery.qtip, explore/common">			
	<div id="mainPanel">
		<span id="userId" value="${userId}"></span>
		<div id="subscribeUnsubscribeButtonContainer" class="center">
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
		
		<div id="pastReports">
			<fieldset>
				<legend>past reports</legend>
				<table>
					<thead>
						<th>file</th>
					</thead>
					<tbody>
						<c:forEach items="${pastReports}" var="report">
						<tr class="center">
							<td><a href="/${starexecRoot}/services/reports/past/${report.getName()}"><c:out value="${report.getName()}"/></a></td>
						</tr>
						</c:forEach>
					</tbody>
				</table>
			</fieldset>
		</div>
	</div>	
</star:template>
