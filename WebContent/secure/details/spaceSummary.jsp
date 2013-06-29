<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
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
				if (curStats.size()>0) {
					subspaceStats.put(sub,curStats);
				}
			}
			
			request.setAttribute("subspaceStats",subspaceStats);
			request.setAttribute("subspaceCount",subspaceStats.keySet().size());
			List<JobPair> pairs=Jobs.getPairsDetailedInSpace(jobId,spaceId);
			
			if (pairs.size()>0) {
				List<JobPair> completedPairs=new ArrayList<JobPair>();
				for (JobPair jp : pairs) {
					if (jp.getStatus().getCode().getVal()==Status.StatusCode.STATUS_COMPLETE.getVal()) {
						completedPairs.add(jp);
					}
				}
				request.setAttribute("pairCount", completedPairs.size());
				String spaceOverviewPath=Statistics.makeSpaceOverviewChart(completedPairs,false,true);
				request.setAttribute("spaceOverviewPath",spaceOverviewPath);
				List<JobSolver> stats=Jobs.processPairsToJobSolvers(pairs,0 , -1, true , 0 , "" , jobId , new int [1]);
				if (stats.size()>=2) {
					int default1=stats.get(0).getConfiguration().getId();
					int default2=stats.get(1).getConfiguration().getId();
					
					request.setAttribute("defaultSolver1",default1);
					request.setAttribute("defaultSolver2",default2);
					
					List<JobPair> pairs1=new ArrayList<JobPair>();
					List<JobPair> pairs2=new ArrayList<JobPair>();
					for (JobPair jp: completedPairs ) {
						if (jp.getConfiguration().getId()==default1) {
							pairs1.add(jp);
						} 
						if (jp.getConfiguration().getId()==default2) {
							pairs2.add(jp);
						}
						
					}
					List<String> solverComparisonChart=Statistics.makeSolverComparisonChart(pairs1,pairs2);
					String solverComparisonPath=solverComparisonChart.get(0);
					String imageMap=solverComparisonChart.get(1);
					System.out.println(solverComparisonPath);
					request.setAttribute("solverComparisonPath",solverComparisonPath);
					request.setAttribute("imageMap",imageMap);
					
				}
				request.setAttribute("stats",stats);
			} else {
				request.setAttribute("pairCount",0);
			}
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

<star:template title="${space.name}" js="lib/jquery.dataTables.min, details/shared, details/spaceSummary, lib/jquery.ba-throttle-debounce.min" css="common/table, details/shared, details/spaceSummary">			
	<span style="display:none" id="jobId" value="${jobId}" > </span>
	<span style="display:none" id="spaceId" value="${space.id}"></span>
	<c:if test="${pairCount>0}">
		<fieldset id="solverSumamryField">
	
			<legend>solver summary</legend>
			<table id="solveTbl" class="shaded">
				<thead>
				
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
					<c:forEach var="cs" items="${stats}">
						<tr id="statRow">
							<td><a href="/${starexecRoot}/secure/details/pairsInSpace.jsp?id=${jobId}&configid=${cs.configuration.id}&sid=${space.id}" target="_blank">${cs.solver.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
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
	</c:if>
	
	<c:if test="${pairCount>0}">
		<fieldset id="graphField">
			<legend>graphs</legend>	
			<img id="spaceOverview" src="${spaceOverviewPath}"/>
			<c:if test="${stats.size()>=2}">
				<img id="solverComparison" src="${solverComparisonPath}" usemap="#solverComparisonMap"/>
				${imageMap}
			</c:if>
			<fieldset id="optionField">
				<legend>options</legend>
				<input type="checkbox" id="logScale" checked="checked"/><span>log scale</span>
				<c:if test="${stats.size()>=2}">
					<select id="solverChoice1" default="${defaultSolver1}">
						<c:forEach var="js" items="${stats}">
							<option value="${js.getConfiguration().id}">${js.getSolver().name}/${js.getConfiguration().name}</option>
						</c:forEach>
					</select>
					<select id="solverChoice2" default="${defaultSolver2}">
						
						<c:forEach var="js" items="${stats}">
							<option value="${js.getConfiguration().id}">${js.getSolver().name}/${js.getConfiguration().name}</option>
						</c:forEach>
					</select>
					
				</c:if>
			</fieldset>
		</fieldset>
	</c:if>
	<c:if test="${subspaceCount>0}">
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
	</c:if>
	
	
	<fieldset>
	<legend>actions</legend>
	<c:if test="${job.primarySpace!=space.id}">
		<a id="goToParent" href="/${starexecRoot}/secure/details/spaceSummary.jsp?id=${jobId}&sid=${parentSpaceId}">return to parent</a>
	</c:if>
	</fieldset>		
</star:template>