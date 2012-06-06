<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%	
	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("space", Spaces.get(spaceId));
		
		// Verify this user can add jobs to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if(!p.canAddJob()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to create a job here");
		} else {
			request.setAttribute("queues", Queues.getAll());
			request.setAttribute("solvers", Solvers.getBySpaceDetailed(spaceId));
			request.setAttribute("benchs", Benchmarks.getBySpace(spaceId));
			request.setAttribute("preProcs", Processors.getAll(ProcessorType.PRE));
			request.setAttribute("postProcs", Processors.getAll(ProcessorType.POST));
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<jsp:useBean id="now" class="java.util.Date" />
<star:template title="run ${space.name}" css="common/table, add/job" js="lib/jquery.validate.min, add/job, lib/jquery.dataTables.min">
	<form id="addForm" method="post" action="/starexec/secure/add/job">	
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
					<tr title="how do you want this job to be displayed in starexec?">
						<td class="label"><p>job name</p></td>
						<td><input id="txtJobName" name="name" type="text" value="${space.name} <fmt:formatDate pattern="MM-dd-yyyy HH.mm" value="${now}" />"/></td>
					</tr>
					<tr title="are there any additional details that you want to document with the job?">
						<td class="label"><p>description</p></td>
						<td><textarea id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
					</tr>
					<!-- <tr title="do you want to pre-process benchmark inputs before they are fed to the solver?">
						<td class="label"><p>pre processor</p></td>
						<td>
							<select id="preProcess" name="preProcess">
							<option value=""></option>
							<c:forEach var="proc" items="${preProcs}">
									<option value="${proc.id}">${proc.name}</option>
							</c:forEach>
							</select>
						</td>
					</tr>-->
					<tr title="do you want to extract any custom attributes from the job results?">
						<td class="label"><p>post processor</p></td>
						<td>					
							<select id="postProcess" name="postProcess">
							<option value="-1">none</option>
							<c:forEach var="proc" items="${postProcs}">
									<option value="${proc.id}">${proc.name}</option>
							</c:forEach>
							</select>
						</td>
					</tr>
					<tr title="the maximum wallclock time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
						<td class="label"><p>wallclock timeout</p></td>
						<td>	
							<input type="text" name="wallclockTimeout" id="wallclockTimeout" value="-1"/>
						</td>
					</tr>
					<tr title="the maximum CPU time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
						<td class="label"><p>cpu timeout</p></td>
						<td>	
							<input type="text" name="cpuTimeout" id="cpuTimeout" value="-1"/>
						</td>
					</tr>
					<tr title="which queue should this job be submitted to?">
						<td class="label"><p>worker queue</p></td>
						<td>
							<select id="workerQueue" name="queue">							
								<c:forEach var="q" items="${queues}">
	                                <option value="${q.id}">${q.name}</option>
								</c:forEach>
							</select>
						</td>
					</tr>										
				</tbody>					
			</table>
		</fieldset>
		<fieldset id="fieldStep2">
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
							<select id="config_${s.id}" name="configs" multiple="multiple">								
								<c:forEach var="c" items="${s.configurations}">
									<option value="${c.id}" title="${c.description}">${c.name}</option>
								</c:forEach>
							</select>
						</td>																			
					</tr>
				</c:forEach>			
				</tbody>						
			</table>				
			<div class="selectWrap">
				<p class="selectAll"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | <p class="selectNone"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
			</div>
			<h6>please ensure the solver(s) you have selected are highlighted (yellow) before proceeding</h6>
		</fieldset>
		<fieldset id="fieldStep3">
			<legend>benchmark selection</legend>
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
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnNext">next</button>			
			<button type="button" class="round" id="btnPrev">back</button>				
		</div>			
	</form>		
</star:template>