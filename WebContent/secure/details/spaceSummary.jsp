<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		int jobSpaceId=Integer.parseInt(request.getParameter("sid"));
		
		
		Job j=null;
		if(Permissions.canUserSeeJob(jobId,userId)) {
			
			//this means it's an old job and we should run the backwards-compatibility routine
			//to get everything set up first
			if (jobSpaceId==0) {
				jobSpaceId=Jobs.setupJobSpaces(jobId);
			}
			if (jobSpaceId>0) {
				j=Jobs.get(jobId);
				Space s=Spaces.getJobSpace(jobSpaceId);
				request.setAttribute("job", j);
				request.setAttribute("jobspace",s);
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The details for this job could not be obtained");
			}
			
			
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

<star:template title="${job.name}" js="lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/spaceSummary, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, explore/common, details/shared, details/spaceSummary">			
	<span style="display:none" id="jobId" value="${job.id}" > </span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	<div id="explorer" class="jobDetails">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	<div id="detailPanel" class="jobDetails">
			<h3 id="spaceName">${jobspace.name}</h3>
			<fieldset id="solverSumamryField"><legend>solver summary</legend>
			<table id="solveTbl" class="shaded">
				<thead>
					<tr>
						<th class="solverHead">solver</th>
						<th class="configHead">configuration</th>
						<th class="completeHead">solved</th>
						<th class="incompleteHead">incomplete</th>
						<th class="wrongHead">wrong</th>
						<th class="failedHead">failed</th>
						<th class="timeHead">time</th>
					</tr>
				</thead>
				<tbody>
						<!-- This will be populated by the job pair pagination feature -->
				</tbody>
			</table>
			</fieldset>
		
		
			<fieldset id="graphField">
			<legend>graphs</legend> 
			<a id="spaceOverviewLink" href=""><img id="spaceOverview" src="" width="300"
				height="300" /></a> 
				
				<a id="solverComparisonLink" href=""><img id="solverComparison" width="300" height="300" src="" usemap="#solverComparisonMap" /></a>
			<fieldset id="optionField"><legend>options</legend> <input
				type="checkbox" id="logScale" checked="checked" /><span>log
			scale</span> 
				<select id="solverChoice1">
					
				</select>
				<select id="solverChoice2">
				</select>
			</fieldset>
			</fieldset>
		
		</div>
</star:template>