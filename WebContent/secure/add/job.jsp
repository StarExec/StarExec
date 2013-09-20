<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
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
		if(!p.canAddJob()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to create a job here");
		} else {
			request.setAttribute("space", Spaces.get(spaceId));
			request.setAttribute("jobNameLen", R.JOB_NAME_LEN);
			request.setAttribute("jobDescLen", R.JOB_DESC_LEN);
			List<String> listOfDefaultSettings = Communities.getDefaultSettings(spaceId);
			List<Processor> ListOfPostProcessors = Processors.getByCommunity(Spaces.GetCommunityOfSpace(spaceId),ProcessorType.POST);
			request.setAttribute("queues", Queues.getUserQueues(userId));
			request.setAttribute("solvers", Solvers.getBySpaceDetailed(spaceId));
			request.setAttribute("benchs", Benchmarks.getBySpace(spaceId));
			//This is for the currently shuttered select from hierarchy
			//request.setAttribute("allBenchs", Benchmarks.getMinForHierarchy(spaceId, userId));
			request.setAttribute("preProcs", Processors.getAll(ProcessorType.PRE));
			request.setAttribute("postProcs", ListOfPostProcessors);
			request.setAttribute("defaultPPName", listOfDefaultSettings.get(1));
			request.setAttribute("defaultCpuTimeout", listOfDefaultSettings.get(2));
			request.setAttribute("defaultClockTimeout", listOfDefaultSettings.get(3));
			request.setAttribute("defaultPPId", listOfDefaultSettings.get(4));
			
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<jsp:useBean id="now" class="java.util.Date" />
<star:template title="run ${space.name}" css="common/delaySpinner, common/table, add/job" js="common/delaySpinner, lib/jquery.validate.min, add/job, lib/jquery.dataTables.min, lib/jquery.qtip.min">
	<form id="addForm" method="post" action="/${starexecRoot}/secure/add/job">	
		<input type="hidden" name="sid" value="${space.id}"/>
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
					<tr class="noHover" title="how do you want this job to be displayed in StarExec?">
						<td class="label"><p>job name</p></td>
						<td><input length="${jobNameLen}" id="txtJobName" name="name" type="text" value="${space.name} <fmt:formatDate pattern="MM-dd-yyyy HH.mm" value="${now}" />"/></td>
					</tr>
					<tr class="noHover" title="are there any additional details that you want to document with the job?">
						<td class="label"><p>description</p></td>
						<td><textarea length="${jobDescLen}" id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
					</tr>
					<tr class="noHover" title="do you want to extract any custom attributes from the job results?">
						<td class="label"><p>post processor</p></td>
						<td>					
							<select id="postProcess" name="postProcess" default="${defaultPPId}">
								<option value="-1">none</option>
								<c:forEach var="proc" items="${postProcs}">
										<option value="${proc.id}">${proc.name} (${proc.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="the maximum wallclock time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
						<td class="label"><p>wallclock timeout</p></td>
						<td>	
							<input type="text" name="wallclockTimeout" id="wallclockTimeout" value="${defaultClockTimeout}"/>
						</td>
					</tr>
					<tr class="noHover" title="the maximum CPU time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
						<td class="label"><p>cpu timeout</p></td>
						<td>	
							<input type="text" name="cpuTimeout" id="cpuTimeout" value="${defaultCpuTimeout}"/>
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
	                                <option value="${q.id}">${q.name} (${q.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="How would you like to traverse the job pairs?">
						<td class="label"><p>Job-Pair Traversal</p></td>
						<td>
							Depth-First<input type="radio" id="radioDepth" name="traversal" value="depth"/> 	
							Round-Robin<input type="radio" id="radioRobin" name="traversal" value="robin"/>	
						</td>
					</tr>								
				</tbody>					
			</table>
		</fieldset>
		<%-- <fieldset id="fieldStep2"> --%>
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
					<tr id="runChoose">
						<td><input type="hidden" name="runChoice" value="choose" />choose</td>
						<td>you will choose which solvers/configurations to run from ${space.name} only.</td>
					</tr>
				</tbody>
			</table>
		</fieldset>
		<%--<fieldset id="fieldStep22"> --%>
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
					<tr id="allBenchInHierarchy">
						<td><input type="hidden" name="benchChoice" value="runAllBenchInHierarchy" />all in hierarchy</td>
						<td>this will run chosen solvers/configurations on all benchmarks in the hierarchy</td>
					</tr>
						<tr id="someBenchInSpace">
						<td><input type="hidden" name="benchChoice" value="runChosenFromSpace" />choose in ${space.name}</td>
						<td>this will run chosen solvers/configurations on your selection of benchmarks in the hierarchy</td>
					</tr>
					<%--<tr id="someBenchInHierarchy">
						<td><input type="hidden" name="runChoice" value="runChosenFromHierarchy" />choose in hierarchy</td>
						<td>this will run chosen solvers/configurations on all benchmarks in the hierarchy</td>
					</tr>--%>
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
					<tr id="solver_${s.id}">
							<td>
								<input type="hidden" name="solver" value="${s.id}"/>
								<star:solver value='${s}'/></td>
							<td>
								<div class="selectConfigs">
									<c:forEach var="c" items="${s.configurations}">
										<input type="checkbox" name="configs" value="${c.id}" title="${c.description}">${c.name} </input><br />
									</c:forEach>
								</div>
							</td>
					</tr>
				</c:forEach>			
				</tbody>						
			</table>				
			<div class="selectWrap">
				<p class="selectAll"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectDefault"><span class="ui-icon ui-icon-circlesmall-plus"></span>all default</p> | <p class="selectNone"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
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
				<c:forEach var="b" items="${benchs}">
					<tr id="bench_${b.id}">
						<td>
							<input type="hidden" name="bench" value="${b.id}"/>
							<star:benchmark value='${b}'/></td>
						<td>
							<p>${b.type.name}</p>							
						</td>																		
					</tr>
				</c:forEach>
				</tbody>					
			</table>	
			<div class="selectWrap">
				<p class="selectAll"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectNone"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
			</div>	
		</fieldset>
	<%--	<fieldset id="fieldSelectBenchHierarchy">
			<legend>benchmark selection from hierarchy</legend>
			<table id="tblBenchHier" class="contentTbl">
				<thead>
					<tr>
						<th>benchmark</th>					
					</tr>
				</thead>	
				<tbody>
				<c:forEach var="b" items="${allBenchs}">
					<tr id="bench_${b.id}">
						<td>
							<input type="hidden" name="bench" value="${b.id}"/>
							<star:benchmark value='${b}'/></td>																
					</tr>
				</c:forEach>
				</tbody>					
			</table>	
			<div class="selectWrap">
				<p class="selectAll"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectNone"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
			</div>	
		</fieldset>--%>
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnNext">next</button>			
			<button type="button" class="round" id="btnPrev">Prev</button>	
			<button type="button" class="round" id="btnBack">Cancel</button>			
		</div>			
	</form>		
</star:template>