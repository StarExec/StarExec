<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*,org.starexec.data.security.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%	
	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		// Verify this user can add jobs to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		
		if (!GeneralSecurity.hasAdminReadPrivileges(userId) && (p == null || !p.canAddJob())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to create a job here");
		} else {
			User u = Users.get(userId);
			int pairsUsed = Jobs.countPairsByUser(userId);
			int remainingQuota = Math.max(0,u.getPairQuota()-pairsUsed);
			request.setAttribute("space", Spaces.get(spaceId));
			request.setAttribute("jobNameLen", R.JOB_NAME_LEN);
			request.setAttribute("jobDescLen", R.JOB_DESC_LEN);
			int communityId = Spaces.getCommunityOfSpace(spaceId);
			
			List<Processor> ListOfPostProcessors = Processors.getByCommunity(communityId,ProcessorType.POST);
			List<Processor> ListOfPreProcessors = Processors.getByCommunity(communityId,ProcessorType.PRE);
			request.setAttribute("queues", Queues.getUserQueues(userId));
			request.setAttribute("remainingPairQuota", remainingQuota);
			List<Solver> solvers = Solvers.getBySpaceDetailed(spaceId);
            Solvers.sortConfigs(solvers);
			Solvers.makeDefaultConfigsFirst(solvers);
			request.setAttribute("solvers", solvers);
			//This is for the currently shuttered select from hierarchy
			request.setAttribute("postProcs", ListOfPostProcessors);
			request.setAttribute("preProcs", ListOfPreProcessors);
			request.setAttribute("suppressTimestamp", R.SUPPRESS_TIMESTAMP_INPUT_NAME);
			List<DefaultSettings> listOfDefaultSettings=Settings.getDefaultSettingsVisibleByUser(userId);
			request.setAttribute("defaultSettings",listOfDefaultSettings);	
			Integer defaultId=Settings.getDefaultProfileForUser(userId);
			if (defaultId!=null && defaultId>0) {
				request.setAttribute("defaultProfile",defaultId);
			} else {
				request.setAttribute("defaultProfile",-1);
			}
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<jsp:useBean id="now" class="java.util.Date" />
<star:template title="run ${space.name}" css="common/delaySpinner, common/table, add/job" js="common/defaultSettings, common/delaySpinner, lib/jquery.validate.min, add/job, lib/jquery.dataTables.min, lib/jquery.qtip.min, add/sharedSolverConfigTableFunctions">
	<c:forEach items="${defaultSettings}" var="setting">
		<star:settings setting="${setting}" />
	</c:forEach>
	<span id="remainingQuota" style="display:none" value="${remainingPairQuota}"></span>
	<span id="defaultProfile" style="display:none" value="${defaultProfile}"></span>
	<form id="addForm" method="post" action="${starexecRoot}/secure/add/job">	
		<input type="hidden" name="sid" id="spaceIdInput" value="${space.id}"/>
		<fieldset id="fieldStep1">
			<legend>configure job</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr class="noHover" title="what default settings profile would you like to use?">
						<td>setting profile</td>
						<td>
							<select id="settingProfile">
									<c:if test="${empty defaultSettings}">
										<option value="" />
									</c:if>				
									<c:forEach var="setting" items="${defaultSettings}">
		                                <option value="${setting.getId()}">${setting.name}</option>
									</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="how do you want this job to be displayed in StarExec?">
						<td class="label"><p>job name</p></td>
						<td><input length="${jobNameLen}" id="txtJobName" name="name" type="text" value="${space.name} <fmt:formatDate pattern="yyyy-MM-dd HH.mm" value="${now}" />"/></td>
					</tr>
					<tr class="noHover" title="are there any additional details that you want to document with the job?">
						<td class="label"><p>description</p></td>
						<td><textarea length="${jobDescLen}" id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
					</tr>
					<tr class="noHover" title="do you want to alter benchmarks before they are fed into the solvers?">
						<td class="label"><p>pre processor</p></td>
						<td>					
							<select class="preProcessSetting" id="preProcess" name="preProcess">
								<option value="-1">none</option>
								<c:forEach var="proc" items="${preProcs}">
										<option value="${proc.id}">${proc.name} (${proc.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="do you want to extract any custom attributes from the job results?">
						<td class="label"><p>post processor</p></td>
						<td>					
							<select class="postProcessSetting" id="postProcess" name="postProcess">
								<option value="-1">none</option>
								<c:forEach var="proc" items="${postProcs}">
										<option value="${proc.id}">${proc.name} (${proc.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					
					<tr class="noHover" title="which queue should this job be submitted to?">
						<td class="label"><p>worker queue</p></td>
						<td>
							<select id="workerQueue" name="queue">
								<c:if test="${empty queues}">
									<option value="" />
								</c:if>				
								<c:forEach var="q" items="${queues}">
	                                <option cpumax="${q.cpuTimeout}" wallmax="${q.wallTimeout}" value="${q.id}">${q.name} (${q.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover">
						<td class="label"><p title="the maximum wallclock time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">wallclock timeout
						<span class="ui-icon ui-icon-help" title="Smaller values may result in faster pair scheduling in the short term. See the cluster help page for more details."></span></p></td>
						<td>	
							<input type="text" name="wallclockTimeout" id="wallclockTimeout"/>
						</td>
					</tr>
					<tr class="noHover" title="the maximum CPU time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
						<td class="label"><p>cpu timeout</p></td>
						<td>	
							<input type="text" name="cpuTimeout" id="cpuTimeout"/>
						</td>
					</tr>
					<tr class="noHover" title="the maximum memory usage (in gigabytes) that each pair can use before it is terminated. The minimum of this value and half the available memory on the nodes will be used.">
						<td class="label"><p>maximum memory</p></td>
						<td>	
							<input type="text" name="maxMem" id="maxMem"/>
						</td>
					</tr>

					<tr id="advancedOptionsRow"><td></td><td><button id="advancedOptionsButton" type="button">advanced options</button></td></tr>
					<tr class="hidden"></tr>
					<tr class="noHover advancedOptions" title="How would you like to traverse the job pairs?">
						<td class="label"><p>Job-Pair Traversal</p></td>
						<td>
							Depth-First<input type="radio" id="radioDepth" name="traversal" value="depth"/> 	
							Round-Robin<input type="radio" id="radioRobin" name="traversal" value="robin"/>	
						</td>
					</tr>	
					<tr class="noHover advancedOptions" title="Would you like to immediately pause the job upon creation?">
						<td class="label"><p>Create Paused</p></td>
						<td>
							Yes<input type="radio" id="radioYesPause" name="pause" value="yes"/> 	
							No<input type="radio" id="radioNoPause" name="pause" value="no"/>	
						</td>
					</tr>
					<tr class="noHover advancedOptions" title="a random value that will be passed into any preprocessor used for this job">
						<td class="label"><p>pre-processor seed</p></td>
						<td>
							<input type="text" name="seed" id="seed" value="0">
						</td>
					</tr>					
					<tr class="noHover advancedOptions" id="suppressTimestampsRow" title="whether to include timestamps in the stdout for the pairs in this job">
						<td>
							<p>Suppress Timestamps</p>
						</td>
						<td>
							Yes<input type="radio" id="radioYesSuppressTimestamps" name="${suppressTimestamp}" value="yes"/>
							No<input type="radio" id="radioNoSuppressTimestamps" name="${suppressTimestamp}" value="no" checked="checked"/>
						</td>
					</tr>
					<tr class="noHover advancedOptions" id="resultsIntervalRow" title="The interval, in seconds, at which to retrieve incremental results for pairs that are running. 0 means results are only obtained after pairs finish. 10 is the minimum if this is used.">
						<td>
							<p>Results Interval</p>
						</td>
						<td>
							<input type="text" name="resultsInterval" id="resultsInterval" value="0">
						</td>
					</tr>
					<tr class="noHover advancedOptions" id="saveAdditionalOutputRow" title="Whether to save solver output that is placed into the extra output directory given to each solver">
						<td>
							<p>Save Additional Output Files</p>
						</td>
						<td>
							Yes<input type="radio" id="radioYesSaveExtraOutput" name="saveOtherOutput" value="true"/>
							No<input type="radio" id="radioNoSaveExtraOutput" name="saveOtherOutput" value="false" checked="checked"/>
						</td>
					</tr>
					<tr class="noHover advancedOptions" id="benchmarkingFrameworkRow">
						<td>
							<p>Benchmarking Framework</p>
						</td>
						<td>
							<span>BenchExec<input type="radio" id="radioUseBenchexec" name="benchmarkingFramework" value="BENCHEXEC"/></span>
							<span>runsolver<input type="radio" id="radioUseRunsolver" name="benchmarkingFramework" value="RUNSOLVER" checked="checked"/></span>
						</td>
					</tr>
				</tbody>
			</table>
		</fieldset>
		<fieldset id="fieldSolverMethod">
			<legend>solver selection method</legend>
			<table id="tblSpaceSelection" class="contentTbl">
				<thead>
					<tr>
						<th>choice</th>
						<th>description</th>
					</tr>
				</thead>
				<tbody>
					<tr id="keepHierarchy">
						<td><input type="hidden" name="runChoice" value="keepHierarchy" />run and keep hierarchy structure</td>
						<td>this will run all solvers/configurations on all benchmarks in their respective spaces within the space hierarchy.</td>
					</tr>
					<tr id="runChoose">
						<td><input type="hidden" name="runChoice" value="choose" />choose</td>
						<td>you will choose which solvers/configurations to run from ${space.name} only.</td>
					</tr>
				</tbody>
			</table>
		</fieldset>
				<fieldset id="fieldBenchMethod">
			<legend>benchmark selection method</legend>
			<table id="tblBenchMethodSelection" class="contentTbl">
				<thead>
					<tr>
						<th>choice</th>
						<th>description</th>
					</tr>
				</thead>
				<tbody>
					<tr id="allBenchInSpace">
						<td><input type="hidden" name="benchChoice" value="runAllBenchInSpace" />all in ${space.name}</td>
						<td>this will run chosen solvers/configurations on all benchmarks in ${space.name}</td>
					</tr>
					<tr id="allBenchInHierarchy">
						<td><input type="hidden" name="benchChoice" value="runAllBenchInHierarchy" />all in hierarchy</td>
						<td>this will run chosen solvers/configurations on all benchmarks in the hierarchy</td>
					</tr>
						<tr id="someBenchInSpace">
						<td><input type="hidden" name="benchChoice" value="runChosenFromSpace" />choose in ${space.name}</td>
						<td>this will run chosen solvers/configurations on your selection of benchmarks in the hierarchy</td>
					</tr>
					
				</tbody>
			</table>
		</fieldset>
		<%-- <fieldset id="fieldStep3"> --%>
		<fieldset id="fieldSolverSelection">
			<legend>solver selection</legend>
			<table id="tblSolverConfig" class="contentTbl">	
				<thead>
					<tr>
						<th>solver</th>
						<th>configuration</th>						
					</tr>
				</thead>
				<tbody>
				<c:forEach var="s" items="${solvers}">
					<tr id="solver_${s.id}" class="solverRow">
							<td>
								<input type="hidden" name="solver" value="${s.id}"/>
								<star:solver value='${s}'/>
							</td>
							<td>
								 <div class="selectConfigs">
									<div class="selectWrap configSelectWrap">
										<p class="selectAll selectAllConfigs"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | 
										<p class="selectNone selectNoneConfigs"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
									</div><br />
									<c:forEach var="c" items="${s.configurations}">
									<input class="config ${c.name}" type="checkbox" name="configs" value="${c.id}" title="${c.description}">${c.name} </input><br />
									</c:forEach> 
								</div> 
							</td>
					</tr>
				</c:forEach>			
				</tbody>						
			</table>				
		   	<div class="selectWrap solverSelectWrap">
				<p class="selectAll selectAllSolvers"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectNone selectNoneSolvers"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
			</div> 
			<h6>please ensure the solver(s) you have selected are highlighted (yellow) before proceeding</h6>
		</fieldset>
		<%--<fieldset id="fieldStep4"> --%>
		 <fieldset id="fieldSelectBenchSpace"> 
			<legend>benchmark selection from space</legend>
			<table id="tblBenchConfig" class="contentTbl">
				<thead>
					<tr>
						<th>benchmark</th>
						<th>type</th>						
					</tr>
				</thead>	
				<tbody>
				</tbody>					
			</table>	
			<div class="selectWrap">
				<p class="selectAll selectAllBenchmarks"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectNoneBenchmarks"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
			</div>	
		</fieldset>
	
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnNext">next</button>			
			<button type="button" class="round" id="btnPrev">Prev</button>	
			<button type="button" class="round" id="btnBack">Cancel</button>			
		</div>			
	</form>		
</star:template>
