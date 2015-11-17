<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
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
				boolean isPaused = (status.getCode() == JobStatusCode.STATUS_PAUSED);
				boolean isAdminPaused = Jobs.isSystemPaused();
				boolean isKilled = (status.getCode() == JobStatusCode.STATUS_KILLED);
				boolean isRunning = (status.getCode() == JobStatusCode.STATUS_RUNNING);
				boolean isProcessing = (status.getCode() == JobStatusCode.STATUS_PROCESSING);
				boolean isComplete = (status.getCode() == JobStatusCode.STATUS_COMPLETE);
				int wallclock=j.getWallclockTimeout();
				int cpu=j.getCpuTimeout();
				long memory=j.getMaxMemory();
				JobSpace s=Spaces.getJobSpace(jobSpaceId);
				User u=Users.get(j.getUserId());

				String jobSpaceTreeJson = RESTHelpers.getJobSpacesJson(jobSpaceId,j.getId(), true, userId);
				//List<JobSpace> subspaces = Spaces.getSubSpacesForJob(jobSpaceId, true);
				
				request.setAttribute("jobSpaceTreeJson", jobSpaceTreeJson);
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
				Processor stage1PostProc=j.getStageAttributesByStageNumber(1).getPostProcessor();
				Processor stage1PreProc=j.getStageAttributesByStageNumber(1).getPreProcessor();
				request.setAttribute("firstPostProc",stage1PostProc);
				request.setAttribute("firstPreProc",stage1PreProc);
				request.setAttribute("queues", Queues.getQueuesForUser(userId));
				request.setAttribute("queueExists", queueExists);
				request.setAttribute("userId",userId);
				request.setAttribute("cpu",cpu);
				request.setAttribute("wallclock",wallclock);
				request.setAttribute("maxMemory",Util.bytesToGigabytes(memory));
				request.setAttribute("seed",j.getSeed());

				List<SolverStats> solverTableStats = Jobs.getAllJobStatsInJobSpaceHierarchy(jobId, jobSpaceId, 1);
				request.setAttribute("solverTableStats", solverTableStats);
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
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name}" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared, details/job">			
	<p id="displayJobID" class="accent" >job id  = ${job.id}</p>
	<span style="display:none" id="jobId" value="${job.id}" > </span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	<c:if test="${isLocalJobPage}">
		<span style="display:none" id="isLocalJobPage" value="${isLocalJobPage}"></span>
		<span style="display:none" id="jobSpaceTreeJson" value="${jobSpaceTreeJson}"></span>
	</c:if>
	
	<div id="explorer" class="jobDetails">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	<div id="detailPanel" class="jobDetails">
			<h3 id="spaceName">${jobspace.name}</h3>
			<p id="displayJobSpaceID" class="accent" title="The job space is a snapshot of the space hierarchy used to create the job. It exists independently of the actual space hierarchy.">job space id  = ${job.primarySpace}</p>
			
			<button id="matrixViewButton" type="button">Matrix View</button>
			<button id="downloadJobPageButton" type="button" style="display:none">Download Job Page</button>
			
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
						<label class="stageSelectorLabel" for="subspaceSummaryStageSelector">Stage: </label>
						<select id="subspaceSummaryStageSelector" class="stageSelector">
							<option value="0">Primary</option>
							<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
								<option value="${i}">${i}</option>
							</c:forEach>
							
						</select> 
				</fieldset>
			</fieldset>
			
			<fieldset id="solverSummaryField">
			<legend>solver summary</legend>
			<fieldset id="statActions" class="tableActions">
				<button class="changeTime">Use CPU Time</button>
				<label class="stageSelectorLabel" for="solverSummaryStageSelector">Stage: </label>
				<select id="solverSummaryStageSelector" class="stageSelector">
						<option value="0">Primary</option>
							<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
								<option value="${i}">${i}</option>
							</c:forEach>	
							
				</select> 
				<button id="compareSolvers">compare selected solvers</button>
				
			</fieldset>
				<table id="solveTbl" class="shaded">
					<thead>
						<tr>
							<th class="solverHead">solver</th>
							<th class="configHead">config</th>
							<th class="solvedHead"><span title="Number of job pairs for which the result matched the expected result, or those attributes are undefined, over the number of job pairs that completed without any system errors. If either the actual or the expected result is starexec-unknown, it is not counted">solved</span></th>
							<th class="wrongHead"><span title="Number of job pairs that completed successfully and without resource errors, but for which the result did not match the expected result. If the actual or expected result is starexec-unknown, it is not counted.">wrong</span></th>
							<th class="resourceHead"><span title="Number of job pairs for which there was a timeout or memout">resource out</span></th>							
							<th class="failedHead"><span title="Number of job pairs that failed due to some sort of internal error, such as job script or benchmark errors">failed</span></th>
							
							<th class="unknownHead"><span title="Number of job pairs that had the result starexec-unknown">unknown</span></th>
							<th class="incompleteHead"><span title="Number of job pairs that are still waiting to run or are running right now">incomplete</span></th>
							<th class="timeHead"><span title="total wallclock or cpu time for all job pairs run that were solved correctly">time</span></th>
						</tr>
					</thead>
					<tbody>
						<!-- This will be populated by the job pair pagination feature -->
						<c:if test="${isLocalJobPage}">
							<c:forEach var="stats" items="${solverTableStats}">
								<tr>
									<td>${stats.getSolver().getName()}</td>
									<td>${stats.getConfiguration().getName()}</td>
									<td>${stats.getCorrectOverCompleted()}</td>
									<td>${stats.getIncorrectJobPairs()}</td>
									<td>${stats.getResourceOutJobPairs()}</td>
									<td>${stats.getFailedJobPairs()}</td>
									<td>${stats.getUnknown()}</td>
									<td>${stats.getIncompleteJobPairs()}</td>
									<td>${Math.round(js.getWallTime()*100)/100.0}</td>
								</tr>
							</c:forEach>
						</c:if>
					</tbody>
				</table>
			</fieldset>
			
			<fieldset id="graphField">
			<legend>graphs</legend> 
			<img id="spaceOverview" src="${starexecRoot}/images/loadingGraph.png" width="300" height="300" /> 
				
				<img id="solverComparison" width="300" height="300" src="${starexecRoot}/images/loadingGraph.png" usemap="#solverComparisonMap" />
				<br>
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
					<label class="stageSelectorLabel" for="subspaceSummaryStageSelector">Stage: </label>
						<select id="pairTableStageSelector" class="stageSelector">
							<option value="0">Primary</option>
							<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
								<option value="${i}">${i}</option>
							</c:forEach>
							
						</select> 
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
						<tr title="the job's name">
							<td>name (click to edit)</td>
							<td id="jobName">
								<span id="jobNameText">${job.name}</span>
								<span id="editJobNameWrapper">
									<input id="editJobName" type="text" value="${job.name}"></input>
									<button id="editJobNameButton">change</button>
								</span>
							</td>
						</tr>
						</tr>
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
							<td>description (click to edit)</td>			
							<td>
								<span id="jobDescriptionText">${job.description}</span>
								<span id="editJobDescriptionWrapper">
									<textarea id="editJobDescription" value="${job.description}"></textarea>
									<button id="editJobDescriptionButton">change</button>
								</span>
							</td>
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
							<c:if test="${not empty firstPreProc}">			
							<td title="${firstPreProc.description}">${firstPreProc.name}</td>
							</c:if>
							<c:if test="${empty firstPreProc}">			
							<td>none</td>
							</c:if>
						</tr>
						<tr title="the postprocessor that was used to process output for this job">
							<td>postprocessor</td>
							<c:if test="${not empty firstPostProc}">			
							<td title="${firstPostProc.description}">${firstPostProc.name}</td>
							</c:if>
							<c:if test="${empty firstPostProc}">			
							<td>none</td>
							</c:if>
						</tr>
						<tr title="the execution queue this job was submitted to">
							<td>queue</td>	
							<c:if test="${not empty job.queue}">
							<td><a href="${starexecRoot}/secure/explore/cluster.jsp">${job.queue.name} <img class="extLink" src="${starexecRoot}/images/external.png"/></a></td>
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
					<li><a id="jobOutputDownload" href="${starexecRoot}/secure/download?type=j_outputs&id=${job.id}" >job output</a></li>
					<li><a id="jobXMLDownload" href="${starexecRoot}/secure/download?type=jobXML&id=${job.id}" >job xml download</a></li>
					<li><a id="jobDownload" href="${starexecRoot}/secure/download?type=job&id=${job.id}">job information</a></li>
					<c:if test="${isAdmin}">
						<li><button type="button" id="clearCache">clear cache</button></li>
						<li><button type="button" id="recompileSpaces">recompile spaces</button></li>
					</c:if>
					
					<c:if test="${job.userId == userId or isAdmin}"> 
						<li><button type="button" id="deleteJob">delete job</button></li>
						<li><a href="${starexecRoot}/secure/edit/resubmitPairs.jsp?id=${job.id}" id="rerunPairs">rerun pairs</a></li>
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
					
					<p>
					<label for="postProcessorSelection">Post Processor</label>
					<select id="postProcessorSelection">
						<c:forEach var="proc" items="${postProcs}">
							<option value="${proc.id}">${proc.name} (${proc.id})</option>
						</c:forEach>
					</select></p>
					<p>
						<label class="noPrimaryStage stageSelectorLabel" for="postProcessorStageSelector">Stage: </label>
						<select id="postProcessorStageSelector" class="stageSelector">
							<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
								<option value="${i}">${i}</option>
							</c:forEach>
							
						</select> 
					
					
					</p>
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
