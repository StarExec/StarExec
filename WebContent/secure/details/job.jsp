<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
				
		
		Job j=null;
		if(Permissions.canUserSeeJob(jobId,userId)) {
			List<Processor> ListOfPostProcessors = Processors.getByUser(userId,ProcessorType.POST);
			j=Jobs.get(jobId);
			
			boolean queueExists = true;
			boolean queueIsEmpty = false;

			if (j.getQueue() == null) {
				queueExists = false;
			} else {
				Queue q = j.getQueue();
				if (q.getNodes() == null) {
					queueIsEmpty = true;
				}
			}
			
			
			int jobSpaceId=j.getPrimarySpace();
			//this means it's an old job and we should run the backwards-compatibility routine
			//to get everything set up first
			if (jobSpaceId==0) {
				jobSpaceId=Jobs.setupJobSpaces(jobId);
			}
			
			if (jobSpaceId>0) {
				j=Jobs.get(jobId);
				JobStatus status=Jobs.getJobStatusCode(jobId);
				List<JobPair> incomplete_pairs = Jobs.getIncompleteJobPairs(jobId);
				boolean isPaused = (status.getCode() == JobStatusCode.STATUS_PAUSED);
				boolean isAdminPaused = Jobs.isJobAdminPaused(jobId);
				boolean isKilled = (status.getCode() == JobStatusCode.STATUS_KILLED);
				boolean isRunning = (status.getCode() == JobStatusCode.STATUS_RUNNING);
				boolean isProcessing = (status.getCode() == JobStatusCode.STATUS_PROCESSING);
				boolean isComplete = (status.getCode() == JobStatusCode.STATUS_COMPLETE);
				int wallclock=Jobs.getWallclockTimeout(jobId);
				int cpu=Jobs.getCpuTimeout(jobId);
				long memory=Jobs.getMaximumMemory(jobId);
				Space s=Spaces.getJobSpace(jobSpaceId);
				User u=Users.get(j.getUserId());
				
				
				//save the integer codes for solver-related cache items. This way, 
				//if the admin decides to clear the cache for the item, we can query the server with the right code
				request.setAttribute("cacheType1",CacheType.CACHE_JOB_OUTPUT.getVal());
				request.setAttribute("cacheType2",CacheType.CACHE_JOB_CSV.getVal());
				request.setAttribute("cacheType3",CacheType.CACHE_JOB_CSV_NO_IDS.getVal());
				request.setAttribute("cacheType4",CacheType.CACHE_JOB_PAIR.getVal());
				request.setAttribute("isAdmin",Users.isAdmin(userId));
				request.setAttribute("usr",u);
				request.setAttribute("job", j);
				request.setAttribute("jobspace",s);
				request.setAttribute("pairStats", Statistics.getJobPairOverview(j.getId()));
				request.setAttribute("isPaused", isPaused);
				request.setAttribute("isAdminPaused", isAdminPaused);
				request.setAttribute("isKilled", isKilled);
				request.setAttribute("isRunning", isRunning);
				request.setAttribute("isComplete", isComplete);
				request.setAttribute("isProcessing", isProcessing);
				request.setAttribute("postProcs", ListOfPostProcessors);
				request.setAttribute("queues", Queues.getUserQueues(userId));
				request.setAttribute("queueExists", queueExists);
				request.setAttribute("userId",userId);
				request.setAttribute("cpu",cpu);
				request.setAttribute("wallclock",wallclock);
				request.setAttribute("maxMemory",Util.bytesToGigabytes(memory));

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
			<fieldset id="statsErrorField">
			<legend>solver summary</legend>
			<p> There are too many job pairs in this space hierarchy to efficiently compile them into stats and graphs. Please navigate to a subspace with fewer pairs</p>
			</fieldset>
						 
			<fieldset id="subspaceSummaryField">
				<legend class="expd" id="subspaceExpd">subspace summaries</legend>
				<button id="popoutPanels">Popout</button>
			</fieldset>
			
			<fieldset id="solverSummaryField">
			<legend>solver summary</legend>
				<table id="solveTbl" class="shaded">
					<thead>
						<tr>
							<th class="solverHead">solver</th>
							<th class="configHead">config</th>
							<th class="completeHead"><span title="Number of job pairs that ran without any errors">complete</span></th>
							<th class="incompleteHead"><span title="Number of job pairs still waiting to run on the grid engine">incomplete</span></th>
							<th class="correctHead"><span title="Number of job pairs for which the result matched the expected result">solved</span></th>
							<th class="wrongHead"><span title="Number of job pairs for which the result did not match the expected result">wrong</span></th>
							<th class="failedHead"><span title="Number of job pairs for which there was a timeout, mem-out, or internal error">failed</span></th>
							<th class="timeHead"><span title="total wallclock time for all job pairs run">time</span></th>
							<th class="pairsInSpaceHead">pairs</th>
						</tr>
					</thead>
					<tbody>
							<!-- This will be populated by the job pair pagination feature -->
					</tbody>
				</table>
			</fieldset>
			
				<fieldset id="graphField">
			<legend>graphs</legend> 
			<img id="spaceOverview" src="/${starexecRoot}/images/emptyGraph.png" width="300" height="300" /> 
				
				<img id="solverComparison" width="300" height="300" src="/${starexecRoot}/images/emptyGraph.png" usemap="#solverComparisonMap" />
				<fieldset id="optionField">
				<legend>options</legend> 
					<fieldset id="spaceOverviewOptionField">
						<legend>space overview options</legend>
						
						<input type="checkbox" id="logScale"/> <span>use log scale</span>
						 
						<select multiple size="5" id="spaceOverviewSelections">
						
						</select>
						<button id="spaceOverviewUpdate" type="button">Update</button>
					</fieldset>
					<fieldset id="solverComparisonOptionField">
						<legend>solver comparison options</legend>
						<select id="solverChoice1">
						
						</select>
						<select id="solverChoice2">
						</select>
						<button id="solverComparisonUpdate" type="button">Update</button>
					</fieldset>
					
					
					
				</fieldset>
			</fieldset>
			<fieldset id="errorField">
				<legend>job pairs</legend>
				<p>There are too many job pairs in this space to display. Please navigate to a subspace with fewer pairs.</p>
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
							<c:if test="${isAdminPaused}">
								<td>Paused(admin)</td>
							</c:if>
							<c:if test="${isKilled}">
								<td>killed</td>
							</c:if>
							<c:if test="${not isPaused && not isKilled && not isAdminPaused}">	
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
						<tr title="the wallclock timeout each pair in the job was subjected to">
							<td>wallclock timeout</td>
							<td>${wallclock}</td>
						</tr>		
						<tr title="the cpu timeout each pair in the job was subjected to">
							<td>cpu timeout</td>
							<td>${cpu}</td>
						</tr>		
						<tr title="the maximum memory each pair in the job was allowed to use, in gigabytes">
							<td>max memory</td>
							<td>${maxMemory}</td>
						</tr>
					</tbody>
				</table>	
			</fieldset>
			
			
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a id="jobOutputDownload" href="/${starexecRoot}/secure/download?type=j_outputs&id=${jobId}" >job output</a></li>
					<li><a id="jobDownload" href="/${starexecRoot}/secure/download?type=job&id=${jobId}">job information</a></li>
					<c:if test="${isAdmin}">
						<span id="cacheType1" class="cacheType" value="${cacheType1}"></span>
						<span id="cacheType2" class="cacheType" value="${cacheType2}"></span>
						<span id="cacheType3" class="cacheType" value="${cacheType3}"></span>
						<span id="cacheType4" class="cacheType" value="${cacheType4}"></span>
						<button type="button" id="clearCache">clear cache</button>
					</c:if>
					
					<c:if test="${job.userId == userId}"> 
						<li><button type="button" id="deleteJob">delete job</button></li>
						<c:if test="${pairStats.pendingPairs > 0}">
							<c:if test="${isRunning}">
								<li><button type="button" id="pauseJob">pause job</button></li>
							</c:if>
						</c:if>
						<c:if test="${pairStats.pendingPairs == 0}">
							<c:if test="${isComplete}">
								<li><button type="button" id="postProcess">run new postprocessor</button></li>
							</c:if>
						</c:if>
						<c:if test="${isPaused and queueExists and !queueIsEmpty}">
							<li><button type="button" id="resumeJob">resume job</button></li>
						</c:if>
						<c:if test="${isPaused or isAdminPaused}">
							<li><button type="button" id="changeQueue">Change Queue</button></li>	
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
				<div id="dialog-warning" title="warning">
					<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
				</div>		
				<div id="dialog-postProcess" title="run new postprocessor">
					<p><span id="dialog-postProcess-txt"></span></p><br/>
					
					<p><select id="postProcessorSelection">
						<c:forEach var="proc" items="${postProcs}">
							<option value="${proc.id}">${proc.name} (${proc.id})</option>
						</c:forEach>
					</select></p>
					
				</div>
				<div id="dialog-changeQueue" title="change queue">
					<p><span id="dialog-changeQueue-txt"></span></p><br/>
					
					<p><select id="changeQueueSelection">
						<c:forEach var="q" items="${queues}">
							<option value="${q.id}">${q.name} (${q.id})</option>
						</c:forEach>
					</select></p>
					
				</div>
				<div id="dialog-spaceOverview" title="space overview chart">
					<img src="" id="bigSpaceOverview"/>
				</div>
			</fieldset>		
		</div>
</star:template>