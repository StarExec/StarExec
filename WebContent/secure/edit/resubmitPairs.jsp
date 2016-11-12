<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.JobSecurity,org.starexec.data.to.*,java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@ page import="org.starexec.data.security.ValidatorStatusCode" %>
<%@ page import="org.starexec.data.security.GeneralSecurity" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
				
		
		Job j=null;
		ValidatorStatusCode status = JobSecurity.canUserSeeRerunPairsPage(jobId,userId);
		if(status.isSuccess()) {
			boolean isComplete=Jobs.isJobComplete(jobId);
			List<Status.StatusCode> filteredCodes=Status.rerunCodes();
			for (Status.StatusCode code : filteredCodes) {
				code.setCount(Jobs.countPairsByStatus(jobId,code.getVal()));
			}
			request.setAttribute("codes",filteredCodes);
			request.setAttribute("jobId",jobId);
			request.setAttribute("isComplete", isComplete);
			request.setAttribute("timelessCount",Jobs.countTimelessPairs(jobId));
		} else {
			if (Jobs.isJobDeleted(jobId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, status.getMessage());
			}
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="rerun pairs for ${job.name}" js="lib/jquery.jstree, lib/jquery.dataTables.min, edit/resubmitPairs, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="edit/resubmitPairs, common/table">			
	<p id="displayJobID" class="accent">id  = ${jobId}</p>
	<span style="display:none" id="jobId" value="${jobId}" > </span>
		<div id="detailPanel">
	
			<fieldset id="detailField">
				<legend>select status</legend>
				<select id="statusCodeSelect">
					<c:forEach var="code" items="${codes}">
						<option value="${code.getVal()}">${code.getStatus()} (${code.getVal()})-- ${code.getCount()}</option>
					</c:forEach>
				
				</select>
				
			</fieldset>
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a class="rerun" id="rerunPairs" >rerun pairs with selected status</a></li>
					<li><a class="rerun" id="rerunTimelessPairs" title="reruns all completed pairs and resource-out pairs in this job that have a wallclock or cpu time of 0">rerun pairs with time 0 (${timelessCount} pairs)</a></li>
					<c:if test="${isComplete}">
						<li><a class="rerun" id="rerunAllPairs" title="reruns every pair in this job">rerun all pairs</a></li>
					</c:if>
				</ul>
			</fieldset>	
		</div>	
</star:template>