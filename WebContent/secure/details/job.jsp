<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		
		Job j = null;
		if(Permissions.canUserSeeJob(jobId, userId)) {
			j = Jobs.getDetailed(jobId);
		}
		
		if(j != null) {			
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);
			request.setAttribute("pairStats", Statistics.getJobPairOverview(j.getId()));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name}" js="lib/jquery.dataTables.min, details/shared, details/job" css="common/table, details/shared, details/job">			
	<fieldset>
		<legend>details</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>property</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>status</td>			
					<td>${pairStats.pendingPairs == 0 ? 'complete' : 'incomplete'}</td>
				</tr>
				<tr>
					<td>description</td>			
					<td>${job.description}</td>
				</tr>
				<tr>
					<td>owner</td>			
					<td><star:user value="${usr}" /></td>
				</tr>							
				<tr>
					<td>created</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a" value="${job.createTime}" /></td>
				</tr>
				<tr>
					<td>runtime</td>			
					<td>${pairStats.runtime / 1000} ms</td>
				</tr>					
				<tr>
					<td>preprocessor</td>
					<c:if test="${not empty job.preProcessor}">			
					<td title="${job.preProcessor.description}">${job.preProcessor.name}</td>
					</c:if>
					<c:if test="${empty job.preProcessor}">			
					<td>none</td>
					</c:if>
				</tr>		
				<tr>
					<td>postprocessor</td>
					<c:if test="${not empty job.postProcessor}">			
					<td title="${job.postProcessor.description}">${job.postProcessor.name}</td>
					</c:if>
					<c:if test="${empty job.postProcessor}">			
					<td>none</td>
					</c:if>
				</tr>
				<tr>
					<td>queue</td>	
					<c:if test="${not empty job.queue}">
					<td><a href="/starexec/secure/explore/cluster.jsp">${job.queue.name} <img class="extLink" src="/starexec/images/external.png"/></a></td>
					</c:if>
					<c:if test="${empty job.queue}">
					<td>unknown</td>
					</c:if>						
				</tr>				
			</tbody>
		</table>	
	</fieldset>		
	<fieldset>
	<legend>job pairs</legend>	
	<c:if test="${empty job.jobPairs}">
		<p>none</p>
	</c:if>		
	<c:if test="${not empty job.jobPairs}">		
		<table id="pairTbl" class="shaded">
			<thead>
				<tr>
					<th>benchmark</th>
					<th>solver</th>
					<th>config</th>
					<th>status</th>
					<th>result</th>				
				</tr>		
			</thead>	
			<tbody>
			<c:forEach var="pair" items="${job.jobPairs}">
				<tr>					
					<td>
						<input type="hidden" name="pid" value="${pair.id}"/>
						<star:benchmark value="${pair.bench}" />
					</td>
					<td><star:solver value="${pair.solver}" /></td>
					<td><star:config value="${pair.solver.configurations[0]}" /></td>				
					<td title="${pair.status.description}">${pair.status}</td>					
					<td>${pair.shortResult}</td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</c:if>		
	</fieldset>		
</star:template>