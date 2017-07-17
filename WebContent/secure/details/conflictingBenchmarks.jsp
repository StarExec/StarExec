<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.util.dataStructures.*, org.starexec.data.security.JobSecurity" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
        int jobId = Integer.parseInt(request.getParameter("jobId"));
        request.setAttribute("jobId", jobId);
        int userId = SessionUtil.getUserId(request);
        if (!JobSecurity.canUserSeeJob(jobId, userId).isSuccess()) {
            if (Jobs.isJobDeleted(jobId)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
            }
            return;
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
