<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@ page import="org.starexec.data.to.pipelines.JoblineStage" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {


		int jobId = Integer.parseInt(request.getParameter("jobId"));
        int configId = Integer.parseInt(request.getParameter("configId"));
        int stageNumber = Integer.parseInt(request.getParameter("stageNumber"));
        List<Benchmark> conflictingBenchmarksForSolverConfig = new ArrayList<Benchmark>();

        Set<Integer> benchmarks = Jobs.getConflictingBenchmarksForJob(jobId);
        // Loop through all the conflicting benchmarks for the job

        benchmarkLoop:
        for (Integer conflictingBenchId : benchmarks) {
            List<JobPair> pairsContainingConflictingBench = JobPairs.getPairsInJobContainingBenchmark(jobId, conflictingBenchId);
            for (JobPair pair : pairsContainingConflictingBench) {
                JoblineStage stage = pair.getStageFromNumber(stageNumber);
                if (stage != null && stage.getConfiguration().getId() == configId) {
                    // If a pair containing the conflicting benchmark was found then the solver-config
                    // for this page contributed to a conflict so add it to the benchmarks to display on the page.
                    conflictingBenchmarksForSolverConfig.add(Benchmarks.get(conflictingBenchId));
                    continue benchmarkLoop;
                }
            }
        }

        request.setAttribute("conflictingBenchmarks", conflictingBenchmarksForSolverConfig);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="" js="details/solverConflicts, util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared">
	<table class="solverConflictsTable">
        <thead>
            <tr>
                <th>name</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="bench" items="${conflictingBenchmarks}">
                <tr>
                    <td>${bench.name}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</star:template>
