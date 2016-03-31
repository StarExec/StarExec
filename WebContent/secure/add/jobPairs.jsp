<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.Map, java.util.ArrayList, java.util.List, java.util.Set, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.security.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int jobId = Integer.parseInt( request.getParameter("jobId") ); 
		final int userId = SessionUtil.getUserId( request );
		ValidatorStatusCode securityStatus = JobSecurity.canUserAddJobPairs( jobId, userId ); 
		if ( !securityStatus.isSuccess() ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, securityStatus.getMessage());
			return;
		}

		if ( !( Jobs.isJobPaused( jobId )  || Jobs.isJobComplete( jobId ) ) ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Job must be finished or paused to add job pairs.");
			return;
		}

		List<Solver> solvers = Solvers.getByJobSimpleWithConfigs( jobId );
		Set<Integer> configIdSet = Solvers.getConfigIdSetByJob( jobId );	
		Solvers.makeDefaultConfigsFirst(solvers);
		request.setAttribute("solvers", solvers);
		request.setAttribute("configIdSet", configIdSet);
		request.setAttribute("jobId", jobId);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Util.getStackTrace( e ));
	}	
%>
<star:template title="Add Job Pairs" js="util/sortButtons, util/datatablesUtility, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, add/sharedSolverConfigTableFunctions, add/jobPairs" css="common/table, common/delaySpinner, explore/common, add/job">		
	<form id="addJobPairsForm" method="post" action="${starexecRoot}/secure/add/jobPairs">
		<input id="jobId" style="display:none" value="${jobId}" name="jobId"></input>
		<fieldset id="fieldSolverSelection">
			<legend>add/delete by config</legend>
			<table id="tblSolverConfig" class="contentTbl">	
				<thead>
					<tr>
						<th>solver</th>
						<th>configuration</th>						
					</tr>
				</thead>
				<tbody>
				<c:forEach var="s" items="${solvers}">
					<tr id="solver_${s.id}" class="solverRow">
						<td>
							<input type="hidden" name="solver" value="${s.id}"/>
							<star:solver value='${s}'/>
						</td>
						<td>
							 <div class="selectConfigs">
								<div class="selectWrap configSelectWrap">
									<p class="selectAll selectAllConfigs"><span class="ui-icon ui-icon-circlesmall-plus"></span>all</p> | 
									<p class="selectNone selectNoneConfigs"><span class="ui-icon ui-icon-circlesmall-minus"></span>none</p>
								</div><br />
								<c:forEach var="c" items="${s.configurations}">
								<c:if test="${configIdSet.contains(c.id)}">
									<input class="config ${c.name}" type="checkbox" name="configs" value="${c.id}" title="${c.description}" checked="checked">${c.name} </input><br />
								</c:if>
								<c:if test="${!configIdSet.contains(c.id)}">
									<input class="config ${c.name}" type="checkbox" name="configs" value="${c.id}" title="${c.description}">${c.name} </input><br />
								</c:if>
								</c:forEach> 
							</div> 
						</td>
					</tr>
				</c:forEach>			
				</tbody>						
			</table>				
			<div class="selectWrap solverSelectWrap">
				<p class="selectAll selectAllSolvers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>all
				</p> 
				| 
				<p class="selectNone selectNoneSolvers">
					<span class="ui-icon ui-icon-circlesmall-minus"></span>none
				</p>
			</div> 
			<h6>please ensure the solver(s) you have selected are highlighted (yellow) before proceeding</h6>
		</fieldset>
		<button type="submit" id="btnDone">submit</button>			
	</form>
	<div id="dialog-confirm-add-delete" title="confirm add/delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-add-delete-txt"></span></p>
	</div>	
</star:template>
