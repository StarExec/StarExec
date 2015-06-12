<%@page contentType="text/html" pageEncoding="UTF-8" 
import="java.util.ArrayList,
		java.util.HashMap,
		java.util.HashSet,
		org.starexec.data.database.*,
		org.starexec.data.to.*,
		org.starexec.util.*,
		org.starexec.util.matrixView.*"
		
%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		int stageNumber = Integer.parseInt(request.getParameter("stage"));
		Job job = MatrixViewUtil.getJobIfAvailableToUser(jobId, userId, response);


		Matrix matrix = new Matrix(job, stageNumber);

		request.setAttribute("job", job);
		request.setAttribute("stage", stageNumber);
		request.setAttribute("matrix", matrix);

	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}

%>
<star:template title="${job.name}" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/jobMatrixView, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/dataTables.fixedColumns.min" css="details/jobMatrixView, common/dataTable, common/dataTables.fixedColumns.min">			
<div id="matrixPanel">
	<span id="jobId" style="display: none;">${job.id}</span>
	
	<div class="matrixLegend">
		<p class="matrixTextLegend">
		<span class="bold">Legend:</span><br>
		<span class="wallclock">runtime (wallclock)</span>
		<span class="cpuTimeWallclockDivider"> / </span>
		<span class="memUsageWallclockDivider" hidden> / </span>
		<span class="cpuTime">cpu usage</span> 
		<span class="cpuTimeMemUsageDivider"> / </span> 
		<span class="memUsage">max virtual memory</span> 
		</p>
		<table class="legendColorTable">
			<thead>
			</thead>
			<tbody>
				<tr>
					<td class="legendColor solved">Solved</td>
					<td class="legendColor incomplete">Incomplete</td>
					<td class="legendColor unknown">Unknown</td>
					<td class="legendColor resource">Out Of Resource</td>
					<td class="legendColor failed">Failed</td>
					<td class="legendColor wrong">Wrong</td>
				</tr>
			</tbody>
		</table>
	</div>
	<div class="matrixControls">
		<form class="matrixLegendSelection">
			<input class="wallclockCheckbox" type="checkbox" checked> runtime (wallclock)
			<input class="cpuTimeCheckbox" type="checkbox" checked> cpu usage 
			<input class="memUsageCheckbox" type="checkbox" checked> max virtual memory 
		</form>
		<c:if test="${matrix.hasMultipleStages()}">
			<form class="matrixStageSelection">
				Stage: <input id="selectStageInput" type="text" name="stage" value="${stage}">
				<button id="selectStageButton" type="button">Show Stage</button>
				<span id="selectStageError" style="color: red; display: none;">Stage must be a positive integer.</span> 
			</form>
		</c:if>
	</div>
	
	<table id="jobMatrix">
		<thead>
			<tr class="matrixHeaderRow">
				<th class="solverHeader benchmarksColumnHeader" width="120px">Benchmark</th>
				<c:forEach var="solverConfig" varStatus="i" items="${matrix.getSolverConfigsByColumn()}">
				<th class="solverHeader" width="120px">
					<a href="/${starexecRoot}/secure/details/solver.jsp?id=${solverConfig.getLeft().getId()}" target="_blank">
						${matrix.getTruncatedColumnHeader(i.getIndex())}
					<img class="extLink" src="/${starexecRoot}/images/external.png">
				</th>
				</c:forEach>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="matrixRow" varStatus="i" items="${matrix.getInternalMatrixRepresentation()}">
				<tr class="matrixBodyRow">
					<td class="benchmarkHeader row${i.getIndex()}">
						<a href="/${starexecRoot}/secure/details/benchmark.jsp?id=${matrix.getBenchmarksByRow().get(i.getIndex()).getId()}" target="_blank">
							${matrix.getBenchmarksByRow().get(i.getIndex()).getName()}
							<img class="extLink" src="/${starexecRoot}/images/external.png">
						</a>
					</td>
					<c:forEach var="matrixElement" items="${matrixRow}">
						<c:choose>
							<c:when test="${matrixElement != null}">
								<td class="jobMatrixCell ${matrixElement.getStatus()}" width="120px">
									<a href="/${starexecRoot}/secure/details/pair.jsp?id=${matrixElement.getJobPairId()}">
										<span class="wallclock">${matrixElement.getWallclock()}</span>
										<span class="cpuTimeWallclockDivider"> / </span>
										<span class="memUsageWallclockDivider" hidden> / </span>
										<span class="cpuTime">${matrixElement.getCpuTime()}</span>
										<span class="cpuTimeMemUsageDivider"> / </span>
										<span class="memUsage">${matrixElement.getMemUsage()}</span>
									</a>
								</td>	
							</c:when>
							<c:otherwise>
								<td class="jobMatrixCell" width="120px"></td>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</div>
</star:template>
