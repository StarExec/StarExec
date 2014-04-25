<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.GeneralSecurity,org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Status.StatusCode"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int pairId = Integer.parseInt(request.getParameter("id"));
		
		JobPair jp = JobPairs.getPairDetailed(pairId);
		
		if(jp == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist");	
		} else if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
			Job j = Jobs.getShallow(jp.getJobId());
			
			User u = Users.get(j.getUserId());
			String output=GeneralSecurity.getHTMLSafeString(GridEngineUtil.getStdOut(jp,100));
			String log=GeneralSecurity.getHTMLSafeString(JobPairs.getJobLog(jp.getId()));
			request.setAttribute("pair", jp);
			request.setAttribute("job", j);
			request.setAttribute("usr", u);
			request.setAttribute("output",output);
			request.setAttribute("log",log);
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
					<td>solver</td>			
					<td><star:solver value="${pair.solver}" /></td>
				</tr>
				<tr>
					<td>configuration</td>			
					<td><star:config value="${pair.solver.configurations[0]}" /></td>
				</tr>				
				<tr>
					<td>ran by</td>			
					<td><star:user value="${usr}" /></td>
				</tr>
				<tr>
					<td>cpu timeout</td>			
					<td>${pair.cpuTimeout} seconds</td>
				</tr>
				<tr>
					<td>wallclock timeout</td>			
					<td>${pair.wallclockTimeout} seconds</td>
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
			<table id="pairStats" class="shaded">
				<thead>
					<tr>
						<th>property</th>
						<th>value</th>				
					</tr>		
				</thead>	
				<tbody>
					<tr title="the cpu time usage in seconds">
						<td>cpu usage</td>
						<td>${pair.cpuTime}</td>
					</tr>
					<tr title="the total amount of time spent executing in user mode, expressed in microseconds">
						<td>user time</td>
						<td>${pair.userTime}</td>
					</tr>
					<tr title="the total amount of time spent executing in kernel mode, expressed in microseconds">
						<td>system time</td>
						<td>${pair.systemTime}</td>
					</tr>
					<tr title="the amount of data transferred in input/output operations">
						<td>io data usage</td>
						<td>${pair.ioDataUsage}</td>
					</tr>
					<tr title="the io wait time in seconds">
						<td>io data wait</td>
						<td>${pair.ioDataWait}</td>
					</tr>
					<tr title="the integral memory usage in Gbyte seconds">
						<td>memory usage</td>
						<td>${pair.memoryUsage}</td>
					</tr>
					<tr title="the maximum vmem size in bytes">
						<td>max virtual memory</td>
						<td>${pair.maxVirtualMemory}</td>
					</tr>
					<tr title="the maximum resident set size used (in kilobytes)">
						<td>max residence set size</td>
						<td>${pair.maxResidenceSetSize}</td>
					</tr>
					<tr title="the number of page faults serviced without any io activity; here io activity is avoided by 'reclaiming' a page frame from the list of pages awaiting reallocation">
						<td>page reclaims</td>
						<td>${pair.pageReclaims}</td>
					</tr>
					<tr title="the number of page faults serviced that required io activity">
						<td>page faults</td>
						<td>${pair.pageFaults}</td>
					</tr>
					<tr title="the number of times the file system had to perform input">
						<td>block input</td>
						<td>${pair.blockInput}</td>
					</tr>
					<tr title="the number of times the file system had to perform output">
						<td>block output</td>
						<td>${pair.blockOutput}</td>
					</tr>
					<tr title="the number of times a context switch resulted due to a process voluntarily giving up the processor before its time slice was completed (usually to await availability of a resource)">
						<td>voluntary context switches</td>
						<td>${pair.voluntaryContextSwitches}</td>
					</tr>
					<tr title="the number of times a context switch resulted due to a higher priority process becoming runnable or because the current process exceeded its time slice">
						<td>involuntary context switches</td>
						<td>${pair.involuntaryContextSwitches}</td>
					</tr>
				</tbody>
			</table>
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
	</fieldset>
	
</star:template>