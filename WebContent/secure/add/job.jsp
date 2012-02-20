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
<star:template title="run ${space.name}" css="add/job" js="lib/jquery.validate.min, add/job">
	<form id="addForm" method="post" action="/starexec/secure/add/job">	
		<input type="hidden" name="sid" value="${space.id}"/>
		<fieldset id="fieldStep1">
			<legend>configure job</legend>
			<table id="tblConfig" class="shaded">
				<tr>
					<td class="label"><p>job name</p></td>
					<td><input id="txtJobName" name="name" type="text" value="${space.name} <fmt:formatDate pattern="MM-dd-yyyy HH.mm" value="${now}" />"/></td>
				</tr>
				<tr>
					<td class="label"><p>description</p></td>
					<td><textarea id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
				</tr>
				<tr>
					<td class="label"><p>pre processor</p></td>
					<td>
						<select id="preProcess" name="preProcess">
						<option value=""></option>
						<c:forEach var="proc" items="${preProcs}">
								<option value="${proc.id}">${proc.name}</option>
						</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td class="label"><p>post processor</p></td>
					<td>					
						<select id="postProcess" name="postProcess">
						<option value=""></option>
						<c:forEach var="proc" items="${postProcs}">
								<option value="${proc.id}">${proc.name}</option>
						</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td class="label"><p>worker queue</p></td>
					<td>
						<select id="workerQueue" name="queue">
							<option> </option>
							<c:forEach var="q" items="${queues}">
                                <option value="${q.id}">${q.name}</option>
							</c:forEach>
						</select>
					</td>
				</tr>										
			</table>		
		</fieldset>
		<fieldset id="fieldStep2">
			<legend>solver configurations</legend>
			<table id="tblSolverConfig" class="shaded">	
				<tr>
					<th>solver</th>
					<th>configuration</th>
					<th>action</th>
				</tr>
				<c:forEach var="s" items="${solvers}">
					<tr id="solver_${s.id}">
						<td>
							<input type="hidden" name="solver" value="${s.id}"/>
							<star:solver value='${s}'/></td>
						<td>
							<select id="config_${s.id}" name="configs">								
								<c:forEach var="c" items="${s.configurations}">
									<option value="${c.id}">${c.name}</option>
								</c:forEach>
							</select>
						</td>		
						<td>			
							<a onclick="removeSolver(this)" title="remove from job">[remove]</a>
						<td>											
					</tr>
				</c:forEach>					
			</table>		
		</fieldset>
		<fieldset id="fieldStep3">
			<legend>benchmark selection</legend>
			<table id="tblBenchConfig" class="shaded">	
				<tr>
					<th>benchmark</th>
					<th>type</th>
					<th>action</th>
				</tr>
				<c:forEach var="b" items="${benchs}">
					<tr id="bench_${b.id}">
						<td>
							<input type="hidden" name="bench" value="${b.id}"/>
							<star:benchmark value='${b}'/></td>
						<td>
							<p>${b.type.name}</p>							
						</td>		
						<td>			
							<a onclick="removeBench(this)" title="remove from job">[remove]</a>
						<td>											
					</tr>
				</c:forEach>					
			</table>		
		</fieldset>
		<div id="actionBar">
			<button type="submit" class="round" id="btnDone">submit</button>			
			<button type="button" class="round" id="btnNext">next</button>
			<button type="button" class="round" id="btnUndo">undo</button>
			<button type="button" class="round" id="btnPrev">back</button>				
		</div>			
	</form>		
</star:template>