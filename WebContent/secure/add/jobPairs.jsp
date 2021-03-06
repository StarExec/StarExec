<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Jobs, org.starexec.data.database.Solvers, org.starexec.data.database.Spaces, org.starexec.data.security.JobSecurity, org.starexec.data.security.ValidatorStatusCode, org.starexec.data.to.Solver, org.starexec.util.SessionUtil, org.starexec.util.Util, java.util.HashSet, java.util.List, java.util.Set" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int jobId = Integer.parseInt(request.getParameter("jobId"));
		final int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode securityStatus =
				JobSecurity.canUserAddJobPairs(jobId, userId);
		if (!securityStatus.isSuccess()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
			                   securityStatus.getMessage()
			);
			return;
		}

		if (!(Jobs.isJobPaused(jobId) || Jobs.isJobComplete(jobId))) {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"Job must be finished or paused to add job pairs."
			);
			return;
		}

		if (Jobs.isReadOnly(jobId)) {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"Job is Read Only during StarExec storage migration, and pairs cannot be added or removed."
			);
			return;
		}

		Set<Integer> spacesAssociatedWithJob = Spaces.getByJob(jobId);
		List<Solver> solvers = Solvers.getSolversInSpacesAndJob(jobId,
		                                                        spacesAssociatedWithJob
		);
		Set<Integer> configIdSet = Solvers.getConfigIdSetByJob(jobId);
		List<Solver> solversInJob = Solvers.getByJobSimpleWithConfigs(jobId);
		Set<Integer> solverIdsInJob = new HashSet<Integer>();
		for (Solver s : solversInJob) {
			solverIdsInJob.add(s.getId());
		}

		Solvers.sortConfigs(solvers);
		Solvers.makeDefaultConfigsFirst(solvers);

		request.setAttribute("solverIdsInJob", solverIdsInJob);
		request.setAttribute("solvers", solvers);
		request.setAttribute("configIdSet", configIdSet);
		request.setAttribute("jobId", jobId);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		                   Util.getStackTrace(e)
		);
		return;
	}
%>
<star:template title="Add Job Pairs"
               js="util/sortButtons, util/datatablesUtility, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, add/sharedSolverConfigTableFunctions, add/jobPairs"
               css="common/delaySpinner, explore/common, add/jobPairs">
	<form id="addJobPairsForm" method="post"
	      action="${starexecRoot}/secure/add/jobPairs">
		<input id="jobId" style="display:none" value="${jobId}" name="jobId"/>
		<p> The solvers available here are ones that are in the same space as
			your job.</p>
		<br>
		<p> Unchecking a configuration will delete all job pairs containing that
			configuration. </p>
		<br>
		<p> If "all" is selected for a solver then checking a configuration will
			add a job pair for each
			selected configuration for all benchmarks in the job.</p>
		<br>
		<p> If "paired with solver" is selected then checking a configuration
			will add a job pair for every
			job pair in the job that contains the same solver. The new job pair
			will have the same benchmark
			and solver with the selected configuration. </p>
		<fieldset id="fieldSolverSelection">
			<legend>add/delete by config</legend>
			<table id="tblSolverConfig" class="contentTbl">
				<thead>
				<tr>
					<th>solver</th>
					<th>benchmarks</th>
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
							<c:if test="${solverIdsInJob.contains(s.id)}">
								<input class="addToAllCheckbox" type="checkbox"
								       name="addToAll" value="${s.id}"/>all<br>
								<input class="addToPairedCheckbox"
								       type="checkbox" name="addToPaired"
								       value="${s.id}"
								       checked="checked"/>paired with solver
							</c:if>
							<c:if test="${!solverIdsInJob.contains(s.id)}">
								<input class="addToAllCheckbox" type="checkbox"
								       name="addToAll" value="${s.id}"
								       checked="checked"/>all<br>
							</c:if>
						</td>
						<td>
							<div class="selectConfigs">
								<div class="selectWrap configSelectWrap">
									<p class="selectAll selectAllConfigs"><span
											class="ui-icon ui-icon-circlesmall-plus"></span>all
									</p> |
									<p class="selectNone selectNoneConfigs">
										<span class="ui-icon ui-icon-circlesmall-minus"></span>none
									</p>
								</div>
								<br/>
								<c:forEach var="c" items="${s.configurations}">
									<c:if test="${configIdSet.contains(c.id)}">
										<input class="config ${c.name}"
										       type="checkbox" name="configs"
										       value="${c.id}"
										       title="${c.description}"
										       checked="checked">${c.name} </input><br />
									</c:if>
									<c:if test="${!configIdSet.contains(c.id)}">
										<input class="config ${c.name}"
										       type="checkbox" name="configs"
										       value="${c.id}"
										       title="${c.description}">${c.name} </input><br />
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
			<h6>please ensure the solver(s) you have selected are highlighted
				(yellow) before proceeding</h6>
		</fieldset>
		<button type="submit" id="btnDone">submit</button>
	</form>
	<div id="dialog-confirm-add-delete" title="confirm add/delete"
	     class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span
				id="dialog-confirm-add-delete-txt"></span></p>
	</div>
</star:template>
