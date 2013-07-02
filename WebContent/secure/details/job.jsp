<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		boolean isPaused = Jobs.isJobPaused(jobId);
		Job j=null;
		if(Permissions.canUserSeeJob(jobId, userId)) {
			j = Jobs.getDetailedWithoutJobPairs(jobId);
		}
		
		
		if(j != null) {	
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);
			request.setAttribute("jobId", jobId);
			request.setAttribute("pairStats", Statistics.getJobPairOverview(j.getId()));
			request.setAttribute("userId",userId);
			request.setAttribute("isPaused", isPaused);
		} else {
				if (Jobs.isJobDeleted(jobId)) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
				} else {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
				}
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name}" js="common/delaySpinner, lib/jquery.cookie, lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, details/shared, details/job">			
	<span style="display:none" id="jobId" value="${jobId}" > </span>
	<fieldset>
		<legend>details</legend>
		<table id="detailTbl" class="shaded">
			<thead>
				<tr>
					<th>property</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr title="${pairStats.pendingPairs == 0 ? 'this job has no pending pairs for execution' : 'this job has 1 or more pairs pending execution'}">
					<td>status</td>		
					<c:if test="${isPaused}">
						<td>'paused'</td>
					</c:if>
					<c:if test="${not isPaused}">	
						<td>${pairStats.pendingPairs == 0 ? 'complete' : 'incomplete'}</td>
					</c:if>
				</tr>
				<tr title="the job creator's description for this job">
					<td>description</td>			
					<td>${job.description}</td>
				</tr>
				<tr title="the user who submitted this job">
					<td>owner</td>			
					<td><star:user value="${usr}" /></td>
				</tr>							
				<tr title="the date/time the job was created on StarExec">
					<td>created</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a" value="${job.createTime}" /></td>
				</tr>					
				<tr title="the postprocessor that was used to process output for this job">
					<td>postprocessor</td>
					<c:if test="${not empty job.postProcessor}">			
					<td title="${job.postProcessor.description}">${job.postProcessor.name}</td>
					</c:if>
					<c:if test="${empty job.postProcessor}">			
					<td>none</td>
					</c:if>
				</tr>
				<tr title="the execution queue this job was submitted to">
					<td>queue</td>	
					<c:if test="${not empty job.queue}">
					<td><a href="/${starexecRoot}/secure/explore/cluster.jsp">${job.queue.name} <img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
					</c:if>
					<c:if test="${empty job.queue}">
					<td>unknown</td>
					</c:if>						
				</tr>				
			</tbody>
		</table>	
	</fieldset>
	
	
	<fieldset id="solveTblField">
	<legend>summary</legend>	
		<table id="solveTbl" class="shaded">
			<thead>
				<tr>
					<th>solver</th>
					<th id="configHead">configuration</th>
					<th id="completeHead">solved</th>
					<th id="incompleteHead">incomplete</th>
					<th>wrong</th>
					<th>failed</th>	
					<th>time</th>
				</tr>		
			</thead>	
			<tbody>
				
			</tbody>
		</table>
	</fieldset>
			
	<fieldset id="#pairTblField">
	
	<legend>job pairs</legend>	
		<table id="pairTbl" class="shaded">
			<thead>
				<tr>
					<th id="benchHead">benchmark</th>
					<th>solver</th>
					<th>config</th>
					<th>status</th>
					<th>time</th>
					<th>result</th>	
					<th>space</th>			
				</tr>		
			</thead>	
			<tbody>
				<!-- This will be populated by the job pair pagination feature -->
			</tbody>
		</table>
	</fieldset>
	
	
	
	<fieldset>
	<legend>actions</legend>
		<ul id="actionList">
			<li><a id="jobOutputDownload" href="/${starexecRoot}/secure/download?type=j_outputs&id=${jobId}" >job output</a></li>
			<li><a id="jobDownload" href="/${starexecRoot}/secure/download?type=job&id=${jobId}">job information</a></li>
			<li><a id="spaceSummary" href="/${starexecRoot}/secure/details/spaceSummary.jsp?sid=${job.primarySpace}&id=${jobId}" >space details</a></li>
			<c:if test="${job.userId == userId}"> 
				<li><button type="button" id="deleteJob">delete job</button></li>
			</c:if>
			<c:if test="${pairStats.pendingPairs > 0}">
				<c:if test="${job.userId == userId}">
					<c:if test="${not isPaused}">
						<li><button type="button" id="pauseJob">pause job</button></li>
					</c:if>
				</c:if>
				<c:if test="${job.userId == userId}">
					<c:if test="${isPaused}">
						<li><button type="button" id="resumeJob">resume job</button></li>
					</c:if>
				</c:if>
			</c:if>
		</ul>
		<div id="dialog-confirm-delete" title="confirm delete">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
		</div>	
		<div id="dialog-confirm-pause" title="confirm pause">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-pause-txt"></span></p>
		</div>	
		<div id="dialog-confirm-resume" title="confirm resume">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-resume-txt"></span></p>
		</div>	
	</fieldset>		
</star:template>