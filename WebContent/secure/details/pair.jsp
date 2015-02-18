<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.JobSecurity, org.starexec.data.security.GeneralSecurity,org.starexec.data.database.*, org.starexec.data.to.*,org.starexec.data.to.pipelines.*, org.starexec.util.*, org.starexec.data.to.Status.StatusCode"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int pairId = Integer.parseInt(request.getParameter("id"));
		
		JobPair jp = JobPairs.getPairDetailed(pairId);
		for (JoblineStage stage : jp.getStages()) {
			System.out.println(stage.getStageId());
		}
		
		
		if(jp == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist");	
		} else if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
			Job j = Jobs.get(jp.getJobId());
			
			User u = Users.get(j.getUserId());
			String output=GeneralSecurity.getHTMLSafeString(JobPairs.getStdOut(jp,100));
			String log=GeneralSecurity.getHTMLSafeString(JobPairs.getJobLog(jp.getId()));
			boolean canRerun=(JobSecurity.canUserRerunPairs(j.getId(),userId,jp.getStatus().getCode().getVal()).isSuccess());
			request.setAttribute("pair", jp);
			request.setAttribute("job", j);
			request.setAttribute("usr", u);
			request.setAttribute("output",output);
			request.setAttribute("log",log);
			request.setAttribute("rerun",canRerun);
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this job pair");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given pair id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name} pair #${pair.id}" js="lib/jquery.dataTables.min, details/pair, details/shared" css="common/table, details/shared">			
	<span id="pairId" value="${pair.id}"></span>
	<fieldset id="fieldDetails">
		<legend>details</legend>
		<table id="detailTable" class="shaded">
			<thead>
				<tr>
					<th>property</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr title="${pair.status.getDescription()}">
					<td>status</td>
					<td>${pair.status.getStatus()}</td>
				</tr>
				<tr>
					<td>benchmark</td>			
					<td><star:benchmark value="${pair.bench}" /></td>
				</tr>				
				<tr>
					<td>ran by</td>			
					<td><star:user value="${usr}" /></td>
				</tr>
				<tr>
					<td>cpu timeout</td>			
					<td>${job.cpuTimeout} seconds</td>
				</tr>
				<tr>
					<td>wallclock timeout</td>			
					<td>${job.wallclockTimeout} seconds</td>
				</tr>
				<tr>
					<td>memory limit</td>
					<td>${job.maxMemory} bytes</td>
				</tr>
				<c:if test="${pair.status.code == 'STATUS_COMPLETE'}">
				<tr>
					<td>execution host</td>
					<td><a href="/${starexecRoot}/secure/explore/cluster.jsp">${pair.node.name}  <img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
				</tr>				
				<tr>
					<td>runtime (wallclock)</td>			
					<td>${pair.wallclockTime} seconds</td>
				</tr>			
				</c:if>
				<tr>
					<td>space</td>
					<td>${pair.jobSpaceName}</td>
				</tr>											
			</tbody>
		</table>	
	</fieldset>		
	<fieldset id="fieldStats">
	<legend>run statistics</legend>	
	<c:choose>
		<c:when test="${pair.status.code == 'STATUS_WAIT_RESULTS'}">
			<p>waiting for results. try again in 2 minutes.</p>
		</c:when>
		<c:when test="${pair.status.code == 'STATUS_COMPLETE'}">
			<c:forEach var="stage" items="${pair.getStages()}">
				
				<table id="pairStats" class="shaded">
					<thead>
						<tr>
							<th>property</th>
							<th>value</th>				
						</tr>		
					</thead>	
					<tbody>
						<tr>
							<td>solver</td>			
							<td><star:solver value="${stage.solver}" /></td>
						</tr>
						<tr>
							<td>configuration</td>			
							<td><star:config value="${stage.solver.configurations[0]}" /></td>
						</tr>
						<tr title="the cpu time usage in seconds">
							<td>cpu usage</td>
							<td>${stage.cpuTime}</td>
						</tr>
						<tr title="the total amount of time spent executing in user mode, expressed in microseconds">
							<td>user time</td>
							<td>${stage.userTime}</td>
						</tr>
						<tr title="the total amount of time spent executing in kernel mode, expressed in microseconds">
							<td>system time</td>
							<td>${stage.systemTime}</td>
						</tr>
						
						<tr title="the integral memory usage in Gbyte seconds">
							<td>memory usage</td>
							<td>${stage.memoryUsage}</td>
						</tr>
						<tr title="the maximum vmem size in bytes">
							<td>max virtual memory</td>
							<td>${stage.maxVirtualMemory}</td>
						</tr>
						<tr title="the maximum resident set size used (in kilobytes)">
							<td>max residence set size</td>
							<td>${stage.maxResidenceSetSize}</td>
						</tr>
					</tbody>
				</table>
			</c:forEach>
			
		</c:when>				
		<c:otherwise>		
			<p>unavailable</p>
		</c:otherwise>
	</c:choose>		
	</fieldset>		
	
	<fieldset id="fieldAttrs">
	<legend>pair attributes</legend>	
	<c:choose>
		<c:when test="${pair.status.code == 'STATUS_WAIT_RESULTS'}">
			<p>waiting for results. try again in 2 minutes.</p>
		</c:when>
		<c:when test="${pair.status.code == 'STATUS_COMPLETE' && empty pair.attributes}">
			<p>none</p>
		</c:when>
		<c:when test="${pair.status.code == 'STATUS_COMPLETE'}">
			<table id="pairAttrs" class="shaded">
				<thead>
					<tr>
						<th>key</th>
						<th>value</th>				
					</tr>		
				</thead>	
				<tbody>
					<c:forEach var="entry" items="${pair.attributes}">
					<tr>
						<td>${entry.key}</td>
						<td>${entry.value}</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</c:when>				
		<c:otherwise>		
			<p>unavailable</p>
		</c:otherwise>
	</c:choose>		
	</fieldset>
	
	<fieldset id="fieldOutput">		
			<legend><img alt="loading" src="/${starexecRoot}/images/loader.gif"> output</legend>			
			<textarea class=contentTextarea id="jpStdout" readonly="readonly">${output}</textarea>	
			<a href="/${starexecRoot}/services/jobs/pairs/${pair.id}/stdout?limit=-1" target="_blank" class="popoutLink">popout</a>
			<p class="caption">output may be truncated. 'popout' for the full output.</p>
	</fieldset>
	
	<fieldset id="fieldLog">
		<legend><img alt="loading" src="/${starexecRoot}/images/loader.gif"> job log</legend>			
		<textarea class=contentTextarea id="jpLog" readonly="readonly">${log}</textarea>
		<a href="/${starexecRoot}/services/jobs/pairs/${pair.id}/log" target="_blank" class="popoutLink">popout</a>			
	</fieldset>
	<fieldset id="fieldActions">
	<legend>actions</legend>
		<a href="/${starexecRoot}/secure/download?type=jp_output&id=${pair.id}" id="downLink">all output</a>
		<a href="/${starexecRoot}/secure/details/job.jsp?id=${job.id}" id="returnLink">return to ${job.name}</a>
		<c:if test="${rerun}">
			<button id="rerunPair">rerun pair</button>
		</c:if>
	</fieldset>
	
</star:template>