<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Analytics, org.starexec.util.JspHelpers" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		JspHelpers.handleJobPage(request, response);
		if (response.getStatus() == 200) {
			Analytics.JOB_DETAILS.record();
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given job id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="${pageTitle}"
               js="util/sortButtons, util/jobDetailsUtilityFunctions, util/datatablesUtility, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min"
               css="common/table, common/delaySpinner, explore/common, details/shared, details/job">
	<script>
		star = star || {};
		<c:if test="${!isLocalJobPage and !isPublicUser && !isComplete}">
		star.isUserSubscribedToJob = ${isUserSubscribedToJob};
		</c:if>
	</script>
	<c:if test="${!isAnonymousPage}">
		<p id="displayJobID" class="accent">job id = ${job.id}</p>
		<span style="display:none" id="jobId" value="${job.id}"> </span>
	</c:if>
	<span style="display:none" id="isAnonymousPage"
	      value="${ isAnonymousPage }"></span>
	<span style="display:none" id="primitivesToAnonymize"
	      value="${ primitivesToAnonymize }"></span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	<span style="display:none" id="starexecUrl" value="${starexecUrl}"></span>
	<c:if test="${isLocalJobPage}">
		<span style="display:none" id="isLocalJobPage"
		      value="${isLocalJobPage}"></span>
		<%-- Get the JSON Data for the space explorer --%>
		<span style="display:none" id="jobSpaceTreeJson"
		      value='${jobSpaceTreeJson}'></span>
		<%-- This is the json with all the subspace data --%>
		<c:forEach var="jsIdKey"
		        items="${jobSpaceIdToSubspaceJsonMap.keySet()}">
			<span style='display:none' id='subspacePanelJson${jsIdKey}'
			    value='${jobSpaceIdToSubspaceJsonMap.get(jsIdKey)}'></span>
		</c:forEach>
		<%-- This space has all the solver stats json. They are all of the format
			 "solverStats" + time + "____cludeUnknown. see below for examples"
		--%>
		<%-- This is the solver stats with cpu time and unknowns--%>
		<c:forEach var="jsIdKey"
		           items="${jobSpaceIdToCpuTimeSolverStatsJsonMapIncludeUnknowns.keySet()}">
			<span style='display:none' id='solverStatsCpuTimeIncludeUnknowns${jsIdKey}'
			      value='${jobSpaceIdToCpuTimeSolverStatsJsonMapIncludeUnknowns.get(jsIdKey)}'></span>
		</c:forEach>
		<%-- This is the solver stats with wallclock time and unknowns--%>
		<c:forEach var="jsIdKey"
		           items="${jobSpaceIdToWallclockTimeSolverStatsJsonMapIncludeUnknowns.keySet()}">
			<span style='display:none'
			      id='solverStatsWallTimeIncludeUnknowns${jsIdKey}'
			      value='${jobSpaceIdToWallclockTimeSolverStatsJsonMapIncludeUnknowns.get(jsIdKey)}'></span>
		</c:forEach>

		<%-- This is the solver stats with cpu time and NO unknowns--%>
		<c:forEach var="jsIdKey"
		           items="${jobSpaceIdToCpuTimeSolverStatsJsonMapExcludeUnknowns.keySet()}">
			<span style='display:none' id='solverStatsCpuTimeExcludeUnknowns${jsIdKey}'
			      value='${jobSpaceIdToCpuTimeSolverStatsJsonMapExcludeUnknowns.get(jsIdKey)}'></span>
		</c:forEach>
		<%-- This is the solver stats with wallclock time and NO unknowns--%>
		<c:forEach var="jsIdKey"
		           items="${jobSpaceIdToWallclockTimeSolverStatsJsonMapExcludeUnknowns.keySet()}">
			<span style='display:none'
			      id='solverStatsWallTimeExcludeUnknowns${jsIdKey}'
			      value='${jobSpaceIdToWallclockTimeSolverStatsJsonMapExcludeUnknowns.get(jsIdKey)}'></span>
		</c:forEach>
	</c:if>

	<div id="explorer" class="jobDetails">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	<div id="detailPanel" class="jobDetails">
		<h3 class="spaceName">${initialSpaceName}</h3>
		<c:if test="${!isAnonymousPage}">
			<p id="displayJobSpaceID" class="accent"
			   title="The job space is a snapshot of the space hierarchy used to create the job. It exists independently of the actual space hierarchy.">
				job space id = ${job.primarySpace}</p>
			<button id="matrixViewButton" type="button">matrix view</button>
			<button id="jobPairAttributes" type="button">attributes summary
			</button>
		</c:if>
		<button id="includeUnknown">include unknown status</button>
		<c:if test="${isAnonymousPage && (job.userId == userId || isAdmin) }">
			<button id="solverNameKeyButton" type="button">solver name key
			</button>
		</c:if>
		<fieldset id="statsErrorField">
			<legend>solver summary</legend>
			<p> There are too many job pairs in this space hierarchy to
				efficiently compile them into stats and graphs. Please navigate
				to a subspace with fewer pairs</p>
		</fieldset>
		<fieldset id="subspaceSummaryField">
			<legend class="expd" id="subspaceExpd">subspace summaries</legend>
			<fieldset id="panelActions" class="tableActions">
				<c:if test="${!isAnonymousPage && !isLocalJobPage}">
					<button id="popoutPanels">Popout</button>
				</c:if>
				<button id="collapsePanels">Collapse All</button>
				<button id="openPanels">Open All</button>
				<button class="changeTime">Use CPU Time</button>
				<label class="stageSelectorLabel"
				       for="subspaceSummaryStageSelector">Stage: </label>
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
				<label class="stageSelectorLabel"
				       for="solverSummaryStageSelector">Stage: </label>
				<select id="solverSummaryStageSelector" class="stageSelector">
					<option value="0">Primary</option>
					<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
						<option value="${i}">${i}</option>
					</c:forEach>
				</select>
				<c:if test="${ !isAnonymousPage }">
					<button id="compareSolvers">compare selected solvers
					</button>
				</c:if>
			</fieldset>
			<c:choose>
				<c:when test="${isLocalJobPage}">
					<table id="${jobspaceIdKey}solveTbl" class="shaded">
						<thead>
						<tr>
							<th class="solverHead">solver</th>
							<th class="configHead">config</th>
							<th class="solvedHead"><span
									title="Number of job pairs for which the result matched the expected result, or those attributes are undefined, over the number of job pairs that completed without any system errors. If either the actual or the expected result is starexec-unknown, it is not counted">solved</span>
							</th>
							<th class="wrongHead"><span
									title="Number of job pairs that completed successfully and without resource errors, but for which the result did not match the expected result. If the actual or expected result is starexec-unknown, it is not counted.">wrong</span>
							</th>
							<th class="resourceHead"><span
									title="Number of job pairs for which there was a timeout or memout">resource out</span>
							</th>
							<th class="failedHead"><span
									title="Number of job pairs that failed due to some sort of internal error, such as job script or benchmark errors">failed</span>
							</th>
							<th class="unknownHead"><span
									title="Number of job pairs that had the result starexec-unknown">unknown</span>
							</th>
							<th class="incompleteHead"><span
									title="Number of job pairs that are still waiting to run or are running right now">incomplete</span>
							</th>
							<th class="timeHead"><span
									title="total wallclock or cpu time for all job pairs run that were solved correctly">time</span>
							</th>
							<th class="conflictsHead"><span
									title="Number of job pairs that had conflicting results for this solver/config.">conflics</span>
							</th>
						</tr>
						</thead>
						<tbody>
							<%-- this will get filled out by the js on a local job page--%>
						</tbody>
						
					</table>
				</c:when>
				<c:otherwise>
					<table id="solveTbl" class="shaded">
						<thead>
						<tr>
							<th class="solverHead">solver</th>
							<th class="configHead">config</th>
							<th class="solvedHead"><span
									title="Number of job pairs for which the result matched the expected result, or those attributes are undefined, over the number of job pairs that completed without any system errors. If either the actual or the expected result is starexec-unknown, it is not counted">solved</span>
							</th>
							<th class="wrongHead"><span
									title="Number of job pairs that completed successfully and without resource errors, but for which the result did not match the expected result. If the actual or expected result is starexec-unknown, it is not counted.">wrong</span>
							</th>
							<th class="resourceHead"><span
									title="Number of job pairs for which there was a timeout or memout">resource out</span>
							</th>
							<th class="failedHead"><span
									title="Number of job pairs that failed due to some sort of internal error, such as job script or benchmark errors">failed</span>
							</th>
							<th class="unknownHead"><span
									title="Number of job pairs that had the result starexec-unknown">unknown</span>
							</th>
							<th class="incompleteHead"><span
									title="Number of job pairs that are still waiting to run or are running right now">incomplete</span>
							</th>
							<th class="timeHead"><span
									title="total wallclock or cpu time for all job pairs run that were solved correctly">time</span>
							</th>
							<th class="conflictsHead"><span
									title="Number of job pairs that had conflicting results for this solver/config.">conflics</span>
							</th>
						</tr>
						</thead>
						<tbody>
							<%-- This will be populated by the job pair pagination feature --%>
						</tbody>
					</table>
				</c:otherwise>
			</c:choose>
		</fieldset>

		<c:if test="${!isLocalJobPage}">
			<fieldset id="graphField">
				<legend>graphs</legend>
				<fieldset style="width:95%" id="graphActions" class="tableActions">
					<button id="selectSpaceOverview" type="button">Show Space Overview</button>
					<button id="selectSolverComparison" type="button">Show Solver Comparison</button>
					<button id="selectPairTimeGraph" type="button">Show Pairs vs. Time</button>
				</fieldset>
				<img id="spaceOverview"
			     		src="${starexecRoot}/images/loadingGraph.png" width="300"
			     		height="300"/>
				<img id="solverComparison300" width="300" height="300"
			     		src="${starexecRoot}/images/loadingGraph.png"
			     		usemap="#solverComparisonMap300"/>
				<img id="pairTimeGraph" width="300" height="300" 
			     		src="${starexecRoot}/images/loadingGraph.png"/>
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
					<fieldset id="pairTimeOptionField">
						<legend>pair vs time options</legend>
						<button id="pairTimeUpdate" type="button">Update</button>
					</fieldset>
				</fieldset>
			</fieldset>
		</c:if>

		<fieldset id="errorField">
			<legend>job pairs</legend>
			<p>There are too many job pairs in this space to display. Please
				navigate to a subspace with fewer pairs.</p>
		</fieldset>
		<fieldset id="pairTblField">
			<legend>job pairs</legend>
			<fieldset  style="width:95%" id="pairActions" class="tableActions">
				<button class="changeTime">Use CPU Time</button>
				<c:if test="${!isLocalJobPage}">
					<button title="sorts pairs by their ids, which is the order they are submitted to be run"
					        asc="true" class="sortButton" id="idSort" value="6">
						sort by id
					</button>
					<button title="sorts pairs in the order they finished running"
					        asc="true" class="sortButton" id="completionSort"
					        value="7">sort by completion order
					</button>
					<button title="show only job pairs that have been solved by every solver/configuration combination in this space"
					        id="syncResults">synchronize results
					</button>
				</c:if>
				<label class="stageSelectorLabel"
				       for="subspaceSummaryStageSelector">Stage: </label>
				<select id="pairTableStageSelector" class="stageSelector">
					<option value="0">Primary</option>
					<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
						<option value="${i}">${i}</option>
					</c:forEach>
				</select>
			</fieldset>
			<c:choose>
				<c:when test="${isLocalJobPage}">
					<c:forEach var="jsId"
					           items="${jobSpaceIdToPairMap.keySet()}">
						<table id="${jsId}pairTbl" class="shaded">
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
							<c:forEach var="pair"
							           items="${jobSpaceIdToPairMap.get(jsId)}">
								<tr>
									<td>${pair.getBench().getName()}
										<input type="hidden" value="${pair.getId()}" name="pid">
									</td>
									<td>${pair.getPrimarySolver().getName()}</td>
									<td>${pair.getPrimaryConfiguration().getName()}</td>
									<td>${pair.getPrimaryStage().getStatus().getStatus()}
										(${pair.getPrimaryStage().getStatus().getCode().getVal()})
									</td>
									<td>
										<span class="wallclockTime">${pair.getPrimaryWallclockTime()}</span>
										<span class="cpuTime">${pair.getPrimaryCpuTime()}</span>
										s
									</td>

									<td>${pair.getPrimaryStage().getStarexecResult()}</td>
								</tr>
							</c:forEach>
								<%-- This will be populated by the job pair pagination feature --%>
							</tbody>
						</table>
					</c:forEach>
				</c:when>
				<c:otherwise>
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
							<%-- This will be populated by the job pair pagination feature --%>
						</tbody>
					</table>
				</c:otherwise>
			</c:choose>
		</fieldset>
		<c:if test="${ !isAnonymousPage }">
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
						<td id="jobNameTitle">name (click to edit)</td>
						<td id="jobName">
							<span id="jobNameText">${job.name}</span>
							<span id="editJobNameWrapper">
										<input id="editJobName" type="text"
										       value="${job.name}"></input>
										<button id="editJobNameButton">change</button>
									</span>
						</td>
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
						<td id="jobDescriptionTitle">description (click to
							edit)
						</td>
						<td>
							<span id="jobDescriptionText">${job.description}</span>
							<span id="editJobDescriptionWrapper">
										<textarea id="editJobDescription"
										          value="${job.description}"></textarea>
										<button id="editJobDescriptionButton">change</button>
									</span>
						</td>
					</tr>
					<tr title="the user who submitted this job">
						<td>owner</td>
						<c:choose>
							<c:when test="${!isLocalJobPage}">
								<td><star:user value="${usr}"/></td>
							</c:when>
							<c:otherwise>
								<td>${usr}</td>
							</c:otherwise>
						</c:choose>
					</tr>
					<tr title="the benchmarking framework used to run the job">
						<td>benchmarking framework</td>
						<td>${job.benchmarkingFramework.toString().toLowerCase()}</td>
					</tr>
					<tr title="the date/time the job was created on StarExec">
						<td>created</td>
						<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a"
						                    value="${job.createTime}"/></td>
					</tr>
					<tr title="the date/time the job was completed">
						<td>completed</td>
						<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a"
						                    value="${job.completeTime}"/></td>
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
							<td>
								<a href="${starexecRoot}/secure/explore/cluster.jsp">
										${job.queue.name}
								</a>
							</td>
						</c:if>
						<c:if test="${empty job.queue}">
							<td>unknown</td>
						</c:if>
					</tr>
					<c:if test="${job.softTimeLimit > 0}">
						<tr title="">
							<td>soft time limit</td>
							<td>${job.softTimeLimit}</td>
						</tr>
					</c:if>
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
					<tr title="the amount of disk space this job is using">
						<td>disk usage</td>
						<td>${diskUsage}</td>
					</tr>
					<c:if test="${job.killDelay > 0}">
						<tr title="">
							<td>kill delay</td>
							<td>${job.killDelay}</td>
						</tr>
					</c:if>
					</tbody>
				</table>
			</fieldset>
			<fieldset id="actionField">
				<legend>actions</legend>
				<ul id="actionList">
					<li><a id="jobOutputDownload"
					       href="${starexecRoot}/secure/download?type=j_outputs&id=${job.id}">job
						output</a></li>
					<li><a id="jobDownload"
					       href="${starexecRoot}/secure/download?type=job&id=${job.id}">job
						information</a></li>
					<c:if test="${job.userId == userId or isAdmin}">
						<li>
							<button type="button" id="deleteJob">delete job
							</button>
						</li>
						<c:if test="${not buildJob}">
							<li>
								<a href="${starexecRoot}/secure/edit/resubmitPairs.jsp?id=${job.id}"
								   id="rerunPairs">rerun pairs</a></li>
						</c:if>
						<c:if test="${isRunning}">
							<li>
								<button type="button" id="pauseJob">pause job
								</button>
							</li>
						</c:if>
						<c:if test="${isPaused and queueExists and (not queueIsEmpty)}">
							<li>
								<button type="button" id="resumeJob">resume
									job
								</button>
							</li>
						</c:if>
					</c:if>
				</ul>
			</fieldset>
			<fieldset id="advancedActionField">
				<legend>advanced actions</legend>
				<ul class='actionList'>
					<li><a id="jobXMLDownload"
					       href="${starexecRoot}/secure/download?type=jobXML&id=${job.id}">job
						xml download</a></li>
					<li><a id="downloadJobPageButton" type="button">download job
						page</a></li>
					<c:if test="${job.userId == userId or isAdmin}">
						<c:if test="${(isPaused or isComplete) and (not buildJob)}">
							<li><a id="addJobPairs"
							       href="${starexecRoot}/secure/add/jobPairs.jsp?jobId=${job.id}">add/delete
								job pairs</a></li>
						</c:if>
						<li><a id="anonymousLink">get anonymous link</a></li>
					</c:if>
					<c:if test="${isAdmin}">
						<li><a id="clearCache">clear cache</a></li>
						<li><a id="recompileSpaces">recompile spaces</a></li>
					</c:if>
					<c:if test="${job.userId == userId or isAdmin}">
						<c:if test="${isComplete}">
							<li><a id="postProcess">run new postprocessor</a>
							</li>
						</c:if>
					</c:if>
					<c:if test="${isPaused or isAdminPaused}">
						<li><a id="changeQueue">Change Queue</a></li>
					</c:if>
					<c:if test="${!isHighPriority}">
						<li><a id="setHighPriority">set as high priority</a>
						</li>
					</c:if>
					<c:if test="${isHighPriority}">
						<li><a id="setLowPriority">set as low priority</a></li>
					</c:if>
				</ul>
			</fieldset>
			<div id="dialog-confirm-delete" title="confirm delete"
			     class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span
						id="dialog-confirm-delete-txt"></span></p>
			</div>
			<div id="dialog-return-ids" title="return ids" class="hiddenDialog">
				<p><span id="dialog-return-ids-txt"></span></p>
				<input type="checkbox" name="includeids" id="includeids"
				       checked="checked"/>include ids<br>
				<input type="checkbox" name="getcompleted" id="getcompleted"/>completed
				pairs only<br>
			</div>
			<div id="dialog-solverComparison" title="solver comparison chart"
			     class="hiddenDialog">
				<img src="" id="solverComparison800"
				     usemap="#solverComparisonMap800"/>
				<map id="solverComparisonMap800"></map>
			</div>
			<div id="dialog-warning" title="warning" class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span
						id="dialog-warning-txt"></span></p>
			</div>
			<div id="dialog-postProcess" title="run new postprocessor"
			     class="hiddenDialog">
				<p><span id="dialog-postProcess-txt"></span></p><br/>
				<p>
					<label for="postProcessorSelection">Post Processor</label>
					<select id="postProcessorSelection">
						<c:forEach var="proc" items="${postProcs}">
							<option value="${proc.id}">${proc.name}
								(${proc.id})
							</option>
						</c:forEach>
					</select></p>
				<p>
					<label class="noPrimaryStage stageSelectorLabel"
					       for="postProcessorStageSelector">Stage: </label>
					<select id="postProcessorStageSelector"
					        class="stageSelector">
						<c:forEach var="i" begin="1"
						           end="${jobspace.maxStages}">
							<option value="${i}">${i}</option>
						</c:forEach>
					</select>
				</p>
			</div>
			<div id="dialog-changeQueue" title="change queue"
			     class="hiddenDialog">
				<p><span id="dialog-changeQueue-txt"></span></p><br/>
				<p><select id="changeQueueSelection">
					<c:forEach var="q" items="${queues}">
						<option value="${q.id}">${q.name} (${q.id})</option>
					</c:forEach>
				</select></p>
			</div>
			<div id="dialog-spaceOverview" title="space overview chart"
			     class="hiddenDialog">
				<img src="" id="bigSpaceOverview"/>
			</div>
			<div id="dialog-pairTimeGraph" title="completed pairs vs time chart"
			     class="hiddenDialog">
				<img src="" id="bigPairTimeGraph"/>
			</div>
			<div id="dialog-show-anonymous-link" title="anonymous link"
			     class="hiddenDialog">
				<p>
					<span class="ui-icon ui-icon-info"></span>
					<span id="dialog-show-anonymous-link-txt"></span>
				</p>
			</div>
			<div id="dialog-confirm-anonymous-link"
			     title="confirm anonymous link" class="hiddenDialog">
				<p><span class="ui-icon ui-icon-info"></span><span
						id="dialog-confirm-anonymous-link-txt"></span></p>
			</div>
		</c:if>
	</div>
</star:template>
