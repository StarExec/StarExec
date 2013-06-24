<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		int spaceId=Integer.parseInt(request.getParameter("sid"));
		
		
		Job j=null;
		
		if(Permissions.canUserSeeJob(jobId,userId) && Permissions.canUserSeeSpace(spaceId,userId) ) {	
			j = Jobs.getDetailedWithoutJobPairs(jobId);
			Space s=Spaces.get(spaceId);
			List<Space> subspaces=Spaces.getSubSpaces(spaceId,userId,false);
			HashMap<Space,List<JobSolver>> subspaceStats=new HashMap<Space,List<JobSolver>>();
			for (Space sub : subspaces) {
				List<JobSolver> curStats=Jobs.getAllJobStatsInSpaceHierarchy(jobId,sub.getId(),userId);
				if (curStats.size()>=0) {
					subspaceStats.put(sub,curStats);
				}
				
			}
			
			request.setAttribute("subspaceStats",subspaceStats);
			List<JobPair> pairs=Jobs.getCompletedJobPairsInSpace(jobId,spaceId);
			request.setAttribute("pairCount", pairs.size());
			String solverComparisonPath=Statistics.makeSolverComparisonChart(pairs);
			
			List<JobSolver> stats=Jobs.getAllJobStatsInSpace(jobId,spaceId);
			request.setAttribute("stats",stats);
			request.setAttribute("solverComparisonPath",solverComparisonPath);
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);
			request.setAttribute("jobId", jobId);
			int parentSpaceId=Spaces.getParentSpace(spaceId);
			request.setAttribute("parentSpaceId",parentSpaceId);
			
			
			request.setAttribute("space",s);
		} else {
			if (Jobs.isJobDeleted(jobId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
			}
		}
	} catch (NumberFormatException nfe) {
		nfe.printStackTrace();
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${space.name}" js="common/delaySpinner, lib/jquery.cookie, lib/jquery.dataTables.min, details/shared, details/spaceSummary, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, details/shared, details/spaceSummary">			
	<span style="display:none" id="jobId" value="${jobId}" > </span>
	<span style="display:none" id="spaceId" value="${space.id}"></span>
	<fieldset id="solverSumamryField">
		<legend>solver summary</legend>
		<table id="solveTbl" class="shaded">
			<thead>
				
				<tr>
					<th>solver</th>
					<th id="configHead">configuration</th>
					<th id="completeHead">solved</th>
					<th id="incompleteHead">incomplete</th>
					<th>wrong</th>
					<th>failed</th>	
					<th>time</th>
				</tr>		
			</thead>	
			<tbody>
				<c:forEach var="cs" items="${stats}">
					<tr id="statRow">
						<td><a href="/${starexecRoot}/secure/details/solver.jsp?id=${cs.solver.id}" target="_blank">${cs.solver.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
						<td><a href="/${starexecRoot}/secure/details/configuration.jsp?id=${cs.configuration.id}" target="_blank">${cs.configuration.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
						<td>${cs.completeJobPairs} </td>
						<td>${cs.incompleteJobPairs} </td>
						<td>${cs.incorrectJobPairs}</td>
						<td>${cs.errorJobPairs}</td>
						<td>${cs.time}</td>
					</tr>
				</c:forEach>
			</tbody>
			</table>
	</fieldset>
	
	<c:if test="${pairCount>0}">
		<fieldset id="graphField">
		<legend>graphs</legend>	
		<img src="${solverComparisonPath}"/>
		</fieldset>
	</c:if>
			
	<fieldset id="subspaceField">
	<legend>subspaces</legend>
	<c:forEach var="sub" items="${subspaceStats.keySet()}">
		<table class="subspaceTable" class="shaded">
			<thead>
				<tr>
					<th colspan="7" class="spaceHeader"><a href="/${starexecRoot}/secure/details/spaceSummary.jsp?id=${jobId}&sid=${sub.id}">${sub.name}</a></th>
				</tr>
				<tr>
					<th>solver</th>
					<th class="configHead">configuration</th>
					<th class="completeHead">solved</th>
					<th class="incompleteHead">incomplete</th>
					<th>wrong</th>
					<th>failed</th>	
					<th>time</th>
				</tr>		
			</thead>
			<tbody>
			<c:forEach var="cs" items="${subspaceStats.get(sub)}">
					<tr id="statRow">
						<td><a href="/${starexecRoot}/secure/details/solver.jsp?id=${cs.solver.id}" target="_blank">${cs.solver.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
						<td><a href="/${starexecRoot}/secure/details/configuration.jsp?id=${cs.configuration.id}" target="_blank">${cs.configuration.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
						<td>${cs.completeJobPairs} </td>
						<td>${cs.incompleteJobPairs} </td>
						<td>${cs.incorrectJobPairs}</td>
						<td>${cs.errorJobPairs}</td>
						<td>${cs.time}</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:forEach>
	</fieldset>
	
	
	
	<fieldset>
	<legend>actions</legend>
	<c:if test="${job.primarySpace!=space.id}">
		<a id="goToParent" href="/${starexecRoot}/secure/details/spaceSummary.jsp?id=${jobId}&sid=${parentSpaceId}">return to parent</a>
	</c:if>
	</fieldset>		
</star:template>