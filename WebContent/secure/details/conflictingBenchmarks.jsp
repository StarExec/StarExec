<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.enums.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@ page import="org.starexec.data.to.pipelines.JoblineStage" %>
<%@ page import="org.starexec.data.security.JobSecurity" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
        int jobId = Integer.parseInt(request.getParameter("jobId"));
        request.setAttribute("jobId", jobId);
        int userId = SessionUtil.getUserId(request);
        if (!JobSecurity.canUserSeeJob(jobId, userId).isSuccess()) {
            if (Jobs.isJobDeleted(jobId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
                return;
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
                return;
            }
        }


        int configId = Integer.parseInt(request.getParameter("configId"));
        Configuration configuration = Solvers.getConfiguration(configId);
        request.setAttribute("configuration", configuration);


        int stageNumber = Integer.parseInt(request.getParameter("stageNumber"));
        request.setAttribute("stageNumber", stageNumber);
        List<Benchmark> conflictingBenchmarksForSolverConfig = Solvers.getConflictingBenchmarksInJobForStage(jobId, configId, stageNumber);
        request.setAttribute("conflictingBenchmarks", conflictingBenchmarksForSolverConfig);

        // Get the solver even if it's deleted since this is for job stats.
        boolean getSolverEvenIfDeleted = true;
        Solver solver = Solvers.getSolverByConfig(configId, getSolverEvenIfDeleted);
        request.setAttribute("solver", solver);



        // Loop through all the conflicting benchmarks for the job

        /*
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
        }*/

	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="Conflicting benchmarks for solver/config ${solver.name} / ${configuration.name}" js="details/solverConflicts, util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="details/solverConflicts, common/table, common/delaySpinner, explore/common, details/shared">
	<table class="conflictsTable">
        <thead>
            <tr>
                <th>name</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="bench" items="${conflictingBenchmarks}">
                <tr>
                    <td>
                        <a href="${starexecRoot}/secure/details/conflictingSolvers.jsp?jobId=${jobId}&benchId=${bench.id}&stageNumber=${stageNumber}">
                            ${bench.name}
                            <img class="extLink" src="${starexecRoot}/images/external.png" />
                        </a>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</star:template>
