<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.JobSecurity,org.starexec.data.to.*,java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
				
		
		Job j=null;
		//if(JobSecurity.canUserRerunPairs(jobId,userId)==0) {
			
			List<Status.StatusCode> filteredCodes=Status.rerunCodes();
			
			request.setAttribute("codes",filteredCodes);
			request.setAttribute("jobId",jobId);
			
			
		//} else {
			//if (Jobs.isJobDeleted(jobId)) {
			//	response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
			//} else {
			//	response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
			//}
		//}
	} catch (NumberFormatException nfe) {
		nfe.printStackTrace();
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="rerun pairs for ${job.name}" js="lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, edit/resubmitPairs, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="edit/resubmitPairs, common/table">			
	<p id="displayJobID" class="accent">id  = ${jobId}</p>
	<span style="display:none" id="jobId" value="${jobId}" > </span>
		<div id="detailPanel">
	
			<fieldset id="detailField">
				<legend>select status</legend>
				<select id="statusCodeSelect">
					<c:forEach var="code" items="${codes}">
						<option value="${code.getVal()}">${code.getStatus()} (${code.getVal()})</option>
					</c:forEach>
				
				</select>
				
			</fieldset>
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a id="rerunPairs" >rerun pairs with selected status</a></li>
					<li><a id="rerunTimelessPairs" >rerun pairs with time 0</a></li>
					
				</ul>
			</fieldset>	
		</div>	
</star:template>