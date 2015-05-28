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
		Matrix matrix = Matrix.buildMatrixFromJob(job);

		request.setAttribute("matrix", matrix);
		/*
		List<JobPair> jobPairs = null;
		HashSet<Solver> uniqueSolvers = new HashSet<Solver>();
		HashSet<String> uniqueBenchmarks = new HashSet<String>();
		ArrayList<Solver> orderedSolvers = null;	
		ArrayList<Benchmark> orderedBenchmarks = null; 
		HashMap<Pair<Solver, Benchmark>, MatrixElement> jobPairToDataMap = null;

		// Build a hashmap that maps the names of the solver and benchmark in a job pair to the data contained by the job pair.
		//jobPairToDataMap = buildPairToDataMap(jobPairs);

		// convert uniqueSolvers and uniqueBenchmarks to ArrayLists so that they have a consistent order
		//orderedSolvers = new ArrayList<Solver>(Arrays.asList(uniqueSolvers.toArray(new Solver[1])));
		//orderedBenchmarks = new ArrayList<Benchmark>(Arrays.asList(uniqueBenchmarks.toArray(new Benchmark[1])));

		// insert the data stored in the data map at the locations in the matrix where the corresponding solver's row and
		// benchmark's column intersect
		
		matrix = new String[orderedSolvers.size()][orderedBenchmarks.size()];
		for (int i = 0; i < orderedSolvers.size(); i++ ) {
			Solver solver = orderedSolvers.get(i);
			for (int j = 0; j < orderedBenchmarks.size(); j++) {
				Benchmark benchmark = orderedBenchmarks.get(j);
				Pair<Solver, Benchmark> solverBenchmark = new ImmutablePair<Solver, Benchmark>(solver, benchmark);
				if (jobPairToDataMap.containsKey(solverBenchmark)) {
					matrix[i][j] = jobPairToDataMap.get(solverBenchmark);
				} else {
					matrix[i][j] = "";
				}
			}
		}

		int benchmarkNumber = 50;
		int solverNumber = 20;
		matrix = new MatrixElement[benchmarkNumber][solverNumber];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[i].length; j++) {
				String success = "success";
				if ((i+j)%5 == 0) {
					success = "failure";
				}
				matrix[i][j] = new MatrixElement(success, "0.0s", "0KB", "0.0s");
			}
		}
		benchmarkNames = new String[benchmarkNumber];
		solverNames = new String[solverNumber];
		for (int i = 0; i < solverNumber; i++) {
			solverNames[i] = "Solver" + String.valueOf(i);	
		}
		for (int i = 0; i < benchmarkNumber; i++) {
			benchmarkNames[i] = "Benchmark" + String.valueOf(i);
		}

		request.setAttribute("jobPairs", jobPairs);
		request.setAttribute("jobPairMatrix", matrix);
		request.setAttribute("solverNames", solverNames);
		request.setAttribute("benchmarkNames", benchmarkNames);
		*/

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
