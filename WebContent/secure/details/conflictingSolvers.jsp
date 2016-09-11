<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.Benchmarks, org.starexec.data.database.JobPairs, org.starexec.data.to.Benchmark, org.starexec.data.to.JobPair, org.starexec.data.to.pipelines.JoblineStage, java.util.ArrayList, java.util.List"%>
<%@ page import="org.apache.commons.lang3.tuple.Triple" %>
<%@ page import="org.apache.commons.lang3.tuple.ImmutableTriple" %>
<%@ page import="org.starexec.data.database.Solvers" %>
<%@ page import="org.starexec.util.SessionUtil" %>
<%@ page import="org.starexec.data.security.JobSecurity" %>
<%@ page import="org.starexec.data.database.Jobs" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    try {
        int jobId = Integer.parseInt(request.getParameter("jobId"));
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
        int benchId = Integer.parseInt(request.getParameter("benchId"));
        int stageNum = Integer.parseInt(request.getParameter("stageNumber"));

        request.setAttribute("tableData", Solvers.getSolverConfigResultsForBenchmarkInJob(jobId, benchId, stageNum));

    } catch (Exception e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
%>
<star:template title="conflicting solvers for bench ${benchmark.name}" js="details/solverConflicts, util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="details/solverConflicts, common/table, common/delaySpinner, explore/common, details/shared">
    <table class="conflictsTable">
        <thead>
            <tr>
                <th>solver</th>
                <th>config</th>
                <th>result</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="solverConfigResult" items="${tableData}">
            <tr>
                <td>${solverConfigResult.left.name}</td>
                <td>${solverConfigResult.middle.name}</td>
                <td>${solverConfigResult.right}</td>
            </tr>
            </c:forEach>
        </tbody>
    </table>
</star:template>
