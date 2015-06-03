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
		request.setAttribute("job", job);
		Matrix matrix = Matrix.buildMatrixFromJob(job);

		request.setAttribute("matrix", matrix);

	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}

%>
<star:template title="${job.name}" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/jobMatrixView, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/dataTables.fixedColumns.min" css="details/jobMatrixView, common/delaySpinner, details/shared">			
<div id="matrixPanel">
	<p class="matrixLegend">
		Legend: 
		<span class="runtime">runtime</span> 
		<span class="runtimeMemUsageDivider"> / </span> 
		<span class="runtimeWallclockDivider" hidden> / </span>
		<span class="memUsage">memory usage</span> 
		<span class="memUsageWallclockDivider"> / </span>
		<span class="wallclock">wallclock time</span>
	</p>
	<form>
		<input class="runtimeCheckbox" type="checkbox" checked> runtime
		<input class="memUsageCheckbox" type="checkbox" checked> memory usage
		<input class="wallclockCheckbox" type="checkbox" checked> wallclock time
	</form>
	<table class="jobMatrix">
		<thead>
			<tr>
				<th width="120px"></th>
				<c:forEach var="solver" items="${matrix.getColumnHeaders()}">
					<th class="solverHeader" width="120px">${solver}</th>
				</c:forEach>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="matrixRow" varStatus="i" items="${matrix.getInternalMatrixRepresentation()}">
				<tr>
					<td class="benchmarkHeader row${i.getIndex()}">Benchmark${i.getIndex()}</td>
					<c:forEach var="matrixElement" items="${matrixRow}">
						<td class="jobMatrixCell ${matrixElement.getStatus()}" width="120px">
							<span class="runtime"><c:out value="${matrixElement.getRuntime()}" /></span>
							<span class="runtimeMemUsageDivider"> / </span>
							<span class="runtimeWallclockDivider" hidden> / </span>
							<span class="memUsage"><c:out value="${matrixElement.getMemUsage()}" /></span>
							<span class="memUsageWallclockDivider"> / </span>
							<span class="wallclock"><c:out value="${matrixElement.getWallclock()}" /></span>
						</td>	
					</c:forEach>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</div>
</star:template>
