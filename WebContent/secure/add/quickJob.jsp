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
			List<Processor> ListOfPostProcessors = Processors.getByCommunity(Spaces.getCommunityOfSpace(spaceId),ProcessorType.POST);
			List<Processor> ListOfPreProcessors = Processors.getByCommunity(Spaces.getCommunityOfSpace(spaceId),ProcessorType.PRE);
			List<Processor> ListOfBenchProcessors = Processors.getByCommunity(Spaces.getCommunityOfSpace(spaceId),ProcessorType.BENCH);

			request.setAttribute("queues", Queues.getQueuesForUser(userId));
			//This is for the currently shuttered select from hierarchy
			//request.setAttribute("allBenchs", Benchmarks.getMinForHierarchy(spaceId, userId));
			request.setAttribute("postProcs", ListOfPostProcessors);
			request.setAttribute("preProcs", ListOfPreProcessors);
			request.setAttribute("benchProcs",ListOfBenchProcessors);
			request.setAttribute("defaultPreProcId", listOfDefaultSettings.get(1));
			request.setAttribute("defaultCpuTimeout", listOfDefaultSettings.get(2));
			request.setAttribute("defaultClockTimeout", listOfDefaultSettings.get(3));
			request.setAttribute("defaultPPId", listOfDefaultSettings.get(4));
			request.setAttribute("defaultBPId", listOfDefaultSettings.get(9));

			request.setAttribute("defaultMaxMem",Util.bytesToGigabytes(Long.parseLong(listOfDefaultSettings.get(7))));
			
			request.setAttribute("solver", Solvers.get(Integer.parseInt(listOfDefaultSettings.get(8))));
			
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<jsp:useBean id="now" class="java.util.Date" />
<star:template title="run ${space.name}" css="common/delaySpinner, common/table, add/job" js="common/delaySpinner, lib/jquery.validate.min, add/job, lib/jquery.dataTables.min, lib/jquery.qtip.min">
	<input type="hidden" name="benchChoice" value="quickJob" />
	<form id="addForm" method="post" action="/${starexecRoot}/secure/add/job">	
		<input type="hidden" name="sid" value="${space.id}"/>
		<fieldset id="baseSettings">
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
					<tr>
						<td class="label"><p>solver</p></td>
						<td><input type="hidden" name="solver" value="${solver.id}"/>${solver.name}</td>
					</tr>
					<tr class="noHover" title="what benchmark would you like to use?">
						<td>benchmark selection</td>
						<td><textarea id="benchmarkField" name="benchmark" rows="6" cols="40"></textarea></td>
					</tr>
				</tbody>
			</table>
			<fieldset id= "advancedSettings">
				<legend>advanced settings</legend>
				<table id="tblAdvancedConfig" class="shaed contentTbl">
					<thead>
						<tr>
							<th>attribute</th>
							<th>value</th>
						</tr>
					</thead>
					<tbody>
						<tr class="noHover" title="are there any additional details that you want to document with the job?">
						<td class="label"><p>description</p></td>
						<td><textarea length="${jobDescLen}" id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
					</tr>
					<tr class="noHover" title="do you want to alter benchmarks before they are fed into the solvers?">
						<td class="label"><p>pre processor</p></td>
						<td>					
							<select id="preProcess" name="preProcess" default="${defaultPreProcId}">
								<option value="-1">none</option>
								<c:forEach var="proc" items="${preProcs}">
										<option value="${proc.id}">${proc.name} (${proc.id})</option>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="do you want to extract any attributes from your benchmark?">
						<td class="label"><p>post processor</p></td>
						<td>					
							<select id="benchProcess" name="benchProcess" default="${defaultBPId}">
								<option value="-1">none</option>
								<c:forEach var="proc" items="${benchProcs}">
										<option value="${proc.id}">${proc.name} (${proc.id})</option>
								</c:forEach>
							</select>
						</td>
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
					<tr class="noHover" title="the maximum memory usage (in gigabytes) that each pair can use before it is terminated. The minimum of this value and half the available memory on the nodes will be used.">
						<td class="label"><p>maximum memory</p></td>
						<td>	
							<input type="text" name="maxMem" id="maxMem" value="${defaultMaxMem}"/>
						</td>
					</tr>
					<tr class="noHover" title="Would you like to immediately pause the job upon creation?">
						<td class="label"><p>Create Paused</p></td>
						<td>
							Yes<input type="radio" id="radioYesPause" name="pause" value="yes"/> 	
							No<input type="radio" id="radioNoPause" name="pause" value="no"/>	
						</td>
					</tr>
					
					</tbody>
				</table>
			</fieldset>
		</fieldset>
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnBack">Cancel</button>			
		</div>			
	</form>		
</star:template>