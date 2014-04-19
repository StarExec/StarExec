<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("jobid"));
		int jobSpaceId=Integer.parseInt(request.getParameter("spaceid"));
		
		Job j=null;
		if(Permissions.canUserSeeJob(jobId,userId)) {
			j=Jobs.get(jobId);
			
			Space s=Spaces.getJobSpace(jobSpaceId);
			User u=Users.get(j.getUserId());	
			request.setAttribute("job", j);
			request.setAttribute("jobspace",s);
			request.setAttribute("userId",userId);
		} else {
			if (Jobs.isJobDeleted(jobId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
			}
		}
	} catch (NumberFormatException nfe) {
		nfe.printStackTrace();
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name}" js="lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/jobPanelView, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, explore/common, details/shared, details/jobPanelView">			
	<span style="display:none" id="jobId" value="${job.id}" > </span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	<div id="mainPanel">
					 
		<fieldset id="subspaceSummaryField">
				<legend class="expd" id="subspaceExpd">subspace summaries</legend>
				<fieldset id="panelActions">
				<button id="collapsePanels">Collapse All</button>
				<button id="openPanels">Open All</button>
				</fieldset>
			</fieldset>
					
	</div>
</star:template>