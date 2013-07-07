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
		
		if(Permissions.canUserSeeJob(jobId,userId) &&  Permissions.canUserSeeSpace(spaceId,userId)) {	
			
			Space s=Spaces.get(spaceId);

			if (s.isPublic() || Users.isMemberOfSpace(userId,spaceId)) {
				request.setAttribute("jobVisible",true);
				j = Jobs.getDetailedWithoutJobPairs(jobId);
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
					List<SolverStats> stats=Jobs.processPairsToSolverStats(pairs,0 , -1, true , 0 , "" , jobId , new int [1]);
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
			} else {
				request.setAttribute("jobVisible",false);
			}
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);
			request.setAttribute("jobId", jobId);
			request.setAttribute("spaceId",spaceId);
			
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

<star:template title="${job.name}" js="lib/jquery.cookie, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/spaceSummary, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, explore/common, details/shared, details/spaceSummary">			
	<span style="display:none" id="jobId" value="${jobId}" > </span>
	<span style="display:none" id="spaceId" value="${spaceId}"></span>
	<div id="explorer">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	<div id="detailPanel">
		<c:if test="${pairCount>0 && jobVisible}">
			<fieldset id="solverSumamryField"><legend>solver
			summary</legend>
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
					
				</tbody>
			</table>
			</fieldset>
		</c:if> 
		<c:if test="${pairCount>0 && jobVisible}">
			<fieldset id="graphField"><legend>graphs</legend> <a
				id="spaceOverviewLink" href="${spaceOverviewPath}600"><img
				id="spaceOverview" src="${spaceOverviewPath}" width="300"
				height="300" /></a> <c:if test="${stats.size()>=2}">
				<a id="solverComparisonLink" href="${solverComparisonPath}600"><img
					id="solverComparison" width="300" height="300"
					src="${solverComparisonPath}" usemap="#solverComparisonMap" /></a>
					${imageMap}
				</c:if>
			<fieldset id="optionField"><legend>options</legend> <input
				type="checkbox" id="logScale" checked="checked" /><span>log
			scale</span> <c:if test="${stats.size()>=2}">
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
			</c:if></fieldset>
			</fieldset>
		</c:if>	
		</div>
</star:template>