<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		int configId=Integer.parseInt(request.getParameter("c1"));
		int configId2=Integer.parseInt(request.getParameter("c2"));
		Job j=null;
		int jobSpaceId=Integer.parseInt(request.getParameter("sid"));
		
		if(Permissions.canUserSeeJob(jobId, userId)) {
			j = Jobs.get(jobId);
		}
		
		
		if(j != null) {	
			JobSpace s=Spaces.getJobSpace(jobSpaceId);
			request.setAttribute("space",s);
			request.setAttribute("configId1",configId);
			request.setAttribute("configId2",configId2);

			Solver solver =Solvers.getSolverByConfig(configId,true);
			Solver solver2 =Solvers.getSolverByConfig(configId2,true);

			request.setAttribute("solver",solver);
			request.setAttribute("solver2",solver2);

			request.setAttribute("jobId", jobId);
			
		} else {
				if (Jobs.isJobDeleted(jobId)) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
				} else {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
				}
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="Comparison of ${solver.name} and ${solver2.name} in ${space.name} hierarchy" js="lib/jquery.dataTables.min, util/jobDetailsUtilityFunctions, details/shared, details/solverComparison, lib/jquery.ba-throttle-debounce.min" css="common/table, details/shared, details/pairsInSpace">			
	<span style="display:none" id="jobId" value="${jobId}" > </span>
	<span style="display:none" id="spaceId" value="${space.id}" > </span>
	<span style="display:none" id="configId1" value="${configId}" > </span>
	<span style="display:none" id="configId2" value="${configId2}" > </span>
	
	<fieldset id="#comparisonTblField">	
	<legend>comparison</legend>	
	<fieldset id="actions" class="tableActions">
		<button class="changeTime">Use CPU Time</button>
	
	</fieldset>
		<table id="comparisonTable" class="shaded">
			<thead>
				<tr>
					<th id="benchHead">benchmark</th>
					<th>solver 1 time</th>
					<th>solver 2 time</th>	
					<th>time difference (1-2)</th>
					<th>solver 1 result</th>
					<th>solver 2 result</th>
					<th>same result</th>
				</tr>		
			</thead>	
			<tbody>
				<!-- This will be populated by the pagination feature -->
			</tbody>
		</table>
	</fieldset>	
</star:template>