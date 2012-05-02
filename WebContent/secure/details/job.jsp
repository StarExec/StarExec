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
			request.setAttribute("jobId", jobId);
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
					<td>${pairStats.pendingPairs == 0 ? 'complete' : 'incomplete'}</td>
				</tr>
				<tr title="the job creator's description for this job">
					<td>description</td>			
					<td>${job.description}</td>
				</tr>
				<tr title="the user who submitted this job">
					<td>owner</td>			
					<td><star:user value="${usr}" /></td>
				</tr>							
				<tr title="the date/time the job was created on starexec">
					<td>created</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a" value="${job.createTime}" /></td>
				</tr>
				<tr title="the total wallclock elapsed time of the job. calculated by taking the difference between the start time of earliest completed pair and the end time of the latest compelted pair">
					<td>elapsed time</td>			
					<td>${pairStats.runtime / 1000} ms</td>
				</tr>					
				<tr title="the preprocessor that was used to process input for this job">
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
					<th>time</th>				
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
					<c:choose>
						<c:when test="${pair.status.code == 7}">
							<td>${pair.wallclockTime / 1000} ms</td>
						</c:when>
						<c:otherwise>
							<td>--</td>
						</c:otherwise>						
					</c:choose>
				</tr>
			</c:forEach>
			</tbody>
		</table>
	</c:if>		
	</fieldset>
	<a href="/starexec/secure/download?type=job&id=${jobId}" id="jobdownload">download</a>
</star:template>