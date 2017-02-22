<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int configId=Integer.parseInt(request.getParameter("configid"));
		String stageCheck=request.getParameter("stagenum");
		int stageId=0;
		// this validates that the stage is an integer
		if (stageCheck!=null) {
			stageId=Integer.parseInt(stageCheck);
		}
		String type=request.getParameter("type");
		int jobSpaceId=Integer.parseInt(request.getParameter("sid"));
		JobSpace space = Spaces.getJobSpace(jobSpaceId);
		if( space==null || !JobSecurity.canUserSeeJob(space.getJobId(), userId).isSuccess()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "This job has been deleted or is restricted");
			return;
		}
		
		Job j= Jobs.get(space.getJobId());

		
		if(j != null && JobSecurity.isValidGetPairType(type)) {	
			JobSpace s=Spaces.getJobSpace(jobSpaceId);
			request.setAttribute("jobspace",s);
			Solver solver =Solvers.getSolverByConfig(configId,true);
			request.setAttribute("solver",solver);
			request.setAttribute("pairType",type);
			
		} else {
				if (Jobs.isJobDeleted(space.getJobId())) {
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

<star:template title="Job Pairs for ${solver.name} in ${jobspace.name} hierarchy" js="util/sortButtons, util/jobDetailsUtilityFunctions, util/datatablesUtility, lib/jquery.dataTables.min, details/shared, details/pairsInSpace, lib/jquery.ba-throttle-debounce.min" css="common/table, details/shared, details/pairsInSpace">			
	<fieldset id="#pairTblField">	
	<legend>job pairs</legend>	
	<fieldset id="pairActions" class="tableActions">
		<button class="changeTime">Use CPU Time</button>
		<button asc="true" class="sortButton" id="idSort" value="6">sort by id</button>
		<button asc="true" class="sortButton" id="collapsePanels" value="7">sort by completion order</button>
		<label class="stageSelectorLabel" for="subspaceSummaryStageSelector">Stage: </label>
			<select id="subspaceSummaryStageSelector" class="stageSelector">
				<option value="0">Primary</option>
				<c:forEach var="i" begin="1" end="${jobspace.maxStages}">
					<option value="${i}">${i}</option>
				</c:forEach>
							
			</select> 
		<label for="pairFilter">filter pairs by:</label>
		<select id="pairFilter">
			<option value="all">all</option>
			<option value="solved">solved</option>
			<option value="wrong">wrong</option>
			<option value="resource">resource out</option>	
			<option value="failed">failed</option>	
			<option value="unknown">unknown</option>		
			<option value="incomplete">incomplete</option>
		</select>

	</fieldset>
		<table id="pairTbl" class="shaded">
			<thead>
				<tr>
					<th id="benchHead">benchmark</th>
					<th id="solverHead">solver</th>
					<th id="configHead">config</th>
					<th>status</th>
					<th>time</th>
					<th>result</th>	
				</tr>		
			</thead>	
			<tbody>
				<!-- This will be populated by the job pair pagination feature -->
			</tbody>
		</table>
	</fieldset>	
</star:template>