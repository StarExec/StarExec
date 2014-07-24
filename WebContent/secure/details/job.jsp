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
				List<WorkerNode> nodes = Cluster.getNodesForQueue(q.getId());
				if (nodes.size() == 0) {
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
				boolean isAdminPaused = Jobs.isSystemPaused();
				boolean isKilled = (status.getCode() == JobStatusCode.STATUS_KILLED);
				boolean isRunning = (status.getCode() == JobStatusCode.STATUS_RUNNING);
				boolean isProcessing = (status.getCode() == JobStatusCode.STATUS_PROCESSING);
				boolean isComplete = (status.getCode() == JobStatusCode.STATUS_COMPLETE);
				int wallclock=Jobs.getWallclockTimeout(jobId);
				int cpu=Jobs.getCpuTimeout(jobId);
				long memory=Jobs.getMaximumMemory(jobId);
				Space s=Spaces.getJobSpace(jobSpaceId);
				User u=Users.get(j.getUserId());
				
				request.setAttribute("isAdmin",Users.isAdmin(userId));
				request.setAttribute("usr",u);
				request.setAttribute("job", j);
				request.setAttribute("jobspace",s);
				request.setAttribute("isPaused", isPaused);
				request.setAttribute("isAdminPaused", isAdminPaused);
				request.setAttribute("isKilled", isKilled);
				request.setAttribute("isRunning", isRunning);
				request.setAttribute("isComplete", isComplete);
				request.setAttribute("queueIsEmpty", queueIsEmpty);
				request.setAttribute("isProcessing", isProcessing);
				request.setAttribute("postProcs", ListOfPostProcessors);
				
				request.setAttribute("queues", Queues.getQueuesForUser(userId));
				request.setAttribute("queueExists", queueExists);
				request.setAttribute("userId",userId);
				request.setAttribute("cpu",cpu);
				request.setAttribute("wallclock",wallclock);
				request.setAttribute("maxMemory",Util.bytesToGigabytes(memory));
				request.setAttribute("seed",j.getSeed());

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

<star:template title="${job.name}" js="util/sortButtons, common/delaySpinner, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared, details/job">			
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
			<p id="displayJobSpaceID" class="accent">id  = ${job.primarySpace}</p>
			
			<fieldset id="statsErrorField">
			<legend>solver summary</legend>
			<p> There are too many job pairs in this space hierarchy to efficiently compile them into stats and graphs. Please navigate to a subspace with fewer pairs</p>
			</fieldset>
						 
			<fieldset id="subspaceSummaryField">
				<legend class="expd" id="subspaceExpd">subspace summaries</legend>
				<fieldset id="panelActions" class="tableActions">
						<button id="popoutPanels">Popout</button>
						<button id="collapsePanels">Collapse All</button>
						<button id="openPanels">Open All</button>
						<button class="changeTime">Use CPU Time</button>
				</fieldset>
			</fieldset>
			
			<fieldset id="solverSummaryField">
			<legend>solver summary</legend>
			<fieldset id="statActions" class="tableActions">
				<button class="changeTime">Use CPU Time</button>
				<button id="compareSolvers">compare selected solvers</button>
			</fieldset>
				<table id="solveTbl" class="shaded">
					<thead>
						<tr>
							<th class="solverHead">solver</th>
							<th class="configHead">config</th>
							<th class="solvedHead"><span title="Number of job pairs for which the result matched the expected result, or those attributes are undefined, over the number of job pairs that completed without any system errors. If the result is starexec-unknown it is not counted">solved</span></th>
							<th class="wrongHead"><span title="Number of job pairs that completed successfully and without resource errors, but for which the result did not match the expected result. If the result is starexec-unknown it is not counted.">wrong</span></th>
							<th class="resourceHead"><span title="Number of job pairs for which there was a timeout or memout">resource out</span></th>							
							<th class="unknownHead"><span title="Number of job pairs that had the result starexec-unknown">unknown</span></th>
							<th class="incompleteHead"><span title="Number of job pairs that are still waiting to run, are running right now, or had a system error, which does not include timeouts and memouts">incomplete</span></th>
							<th class="timeHead"><span title="total wallclock or cpu time for all job pairs run that were solved correctly">time</span></th>
						</tr>
					</thead>
					<tbody>
							<!-- This will be populated by the job pair pagination feature -->
					</tbody>
				</table>
			</fieldset>
			
				<fieldset id="graphField">
			<legend>graphs</legend> 
			<img id="spaceOverview" src="/${starexecRoot}/images/loadingGraph.png" width="300" height="300" /> 
				
				<img id="solverComparison" width="300" height="300" src="/${starexecRoot}/images/loadingGraph.png" usemap="#solverComparisonMap" />
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
				<fieldset id="pairActions" class="tableActions">
					<button class="changeTime">Use CPU Time</button>
					<button title="sorts pairs by their ids, which is the order they are submitted to be run" asc="true" class="sortButton" id="idSort" value="6">sort by id</button>
					<button title="sorts pairs in the order they finished running" asc="true" class="sortButton" id="completionSort" value="7">sort by completion order</button>
					<button title="show only job pairs that have been solved by every solver/configuration combination in this space" id="syncResults">synchronize results</button>
				</fieldset>
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
						<tr title="${isComplete ? 'this job has no pending pairs for execution' : 'this job has 1 or more pairs pending execution'}">
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
								<td>${isComplete ? 'complete' : 'incomplete'}</td>
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
						<tr title="the date/time the job was completed">
							<td>completed</td>			
							<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a" value="${job.completeTime}" /></td>
						</tr>			
						<tr title="the preprocessor that was used to process benchmarks for this job">
							<td>preprocessor</td>
							<c:if test="${not empty job.preProcessor}">			
							<td title="${job.preProcessor.description}">${job.preProcessor.name}</td>
							</c:if>
							<c:if test="${empty job.preProcessor}">			
							<td>none</td>
							</c:if>
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
						<tr title="the random seed given to the preprocessor used by each job pair">
							<td>random seed</td>
							<td>${seed}</td>
						
						</tr>
					</tbody>
				</table>	
			</fieldset>
			
			
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a id="jobOutputDownload" href="/${starexecRoot}/secure/download?type=j_outputs&id=${job.id}" >job output</a></li>
					<li><a id="jobXMLDownload" href="/${starexecRoot}/secure/download?type=jobXML&id=${job.id}" >job xml download</a></li>
					<li><a id="jobDownload" href="/${starexecRoot}/secure/download?type=job&id=${job.id}">job information</a></li>
					<c:if test="${isAdmin}">
						<button type="button" id="clearCache">clear cache</button>
					</c:if>
					
					<c:if test="${job.userId == userId or isAdmin}"> 
						<li><button type="button" id="deleteJob">delete job</button></li>
						<li><a href="/${starexecRoot}/secure/edit/resubmitPairs.jsp?id=${job.id}" id="rerunPairs">rerun pairs</a></li>
							<c:if test="${isRunning}">
								<li><button type="button" id="pauseJob">pause job</button></li>
							</c:if>
						
						
							<c:if test="${isComplete}">
								<li><button type="button" id="postProcess">run new postprocessor</button></li>
							</c:if>
						
						<c:if test="${isPaused and queueExists and (not queueIsEmpty)}">
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
					<input type="checkbox" name="includeids" id="includeids" checked="checked"/>include ids<br>
					<input type="checkbox" name="getcompleted" id="getcompleted" />completed pairs only<br></p>
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