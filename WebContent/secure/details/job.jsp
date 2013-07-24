<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		
		
		Job j=null;
		if(Permissions.canUserSeeJob(jobId,userId)) {
			j=Jobs.get(jobId);
			int jobSpaceId=j.getPrimarySpace();
			//this means it's an old job and we should run the backwards-compatibility routine
			//to get everything set up first
			if (jobSpaceId==0) {
				jobSpaceId=Jobs.setupJobSpaces(jobId);
			}
			if (jobSpaceId>0) {
				j=Jobs.get(jobId);
				boolean isPaused = Jobs.isJobPaused(jobId);
				boolean isKilled = Jobs.isJobKilled(jobId);
				Space s=Spaces.getJobSpace(jobSpaceId);
				User u=Users.get(j.getUserId());
				request.setAttribute("usr",u);
				request.setAttribute("job", j);
				request.setAttribute("jobspace",s);
				request.setAttribute("pairStats", Statistics.getJobPairOverview(j.getId()));
				request.setAttribute("isPaused", isPaused);
				request.setAttribute("isKilled", isKilled);

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

<star:template title="${job.name}" js="common/delaySpinner, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared, details/job">			
	<p id="displayJobID" class="accent">id  = ${job.id}</p>
	<span style="display:none" id="jobId" value="${job.id}" > </span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	
	<div id="explorer" class="jobDetails">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	<div id="detailPanel" class="jobDetails">
			<h3 id="spaceName">${jobspace.name}</h3>
			<fieldset id="solverSummaryField">
			<legend>solver summary</legend>
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
			<img id="spaceOverview" src="" width="300" height="300" /> 
				
				<img id="solverComparison" width="300" height="300" src="" usemap="#solverComparisonMap" />
				<fieldset id="optionField">
				<legend>options</legend> 
					<input type="checkbox" id="logScale" checked="checked" /> <span>log scale</span> 
					<select multiple size="5" id="spaceOverviewSelections">
					
					</select>
					
					<select id="solverChoice1">
					
					</select>
					<select id="solverChoice2">
					</select>
				</fieldset>
			</fieldset>
			<fieldset id="errorField">
				<legend>job pairs</legend>
				<p>There are too many job pairs in this space hierarchy to display. Please navigate to a subspace with fewer pairs.</p>
			</fieldset>
			<fieldset id="pairTblField">
				<legend>job pairs</legend>	
				<table id="pairTbl" class="shaded">
					<thead>
						<tr>
							<th class="benchHead">benchmark</th>
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
		
			
			<fieldset id="detailField">
				<legend>job overview</legend>
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
								<td>paused</td>
							</c:if>
							<c:if test="${isKilled}">
								<td>killed</td>
							</c:if>
							<c:if test="${not isPaused && not isKilled}">	
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
			
			
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a id="jobOutputDownload" href="/${starexecRoot}/secure/download?type=j_outputs&id=${jobId}" >job output</a></li>
					<li><a id="jobDownload" href="/${starexecRoot}/secure/download?type=job&id=${jobId}">job information</a></li>
					<c:if test="${j.userId == userId}"> 
						<li><button type="button" id="deleteJob">delete job</button></li>
					</c:if>
					
					<c:if test="${pairStats.pendingPairs > 0}">
						<c:if test="${j.userId == userId}">
							<c:if test="${not isPaused and not isKilled}">
								<li><button type="button" id="pauseJob">pause job</button></li>
							</c:if>
						</c:if>
					</c:if>
					<c:if test="${j.userId == userId}">
						<c:if test="${isPaused}">
							<li><button type="button" id="resumeJob">resume job</button></li>
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
				<div id="dialog-return-ids" title="return ids">
					<p><span id="dialog-return-ids-txt"></span></p>
				</div>
				<div id="dialog-solverComparison" title="solver comparison chart">
					<img src="" id="bigSolverComparison" usemap="#bigSolverComparisonMap"/>
					<map id="bigSolverComparisonMap"></map>
				</div>
				<div id="dialog-spaceOverview" title="space overview chart">
					<img src="" id="bigSpaceOverview"/>
				</div>
			</fieldset>		
		</div>
</star:template>