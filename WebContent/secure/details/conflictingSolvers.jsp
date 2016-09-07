<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.Benchmarks, org.starexec.data.database.JobPairs, org.starexec.data.to.Benchmark, org.starexec.data.to.JobPair, org.starexec.data.to.pipelines.JoblineStage, java.util.ArrayList, java.util.List"%>
<%@ page import="org.apache.commons.lang3.tuple.Triple" %>
<%@ page import="org.apache.commons.lang3.tuple.ImmutableTriple" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    try {
        int jobId = Integer.parseInt(request.getParameter("jobId"));
        int benchId = Integer.parseInt(request.getParameter("benchId"));
        List<Triple<String, String, String>> table = new ArrayList<Triple<String, String, String>>();

        Benchmark benchmark = Benchmarks.get(benchId);
        request.setAttribute("benchmark", benchmark);

        List<JobPair> jobPairsContainingBenchmark = JobPairs.getPairsInJobContainingBenchmark(jobId, benchId);
        for (JobPair pair : jobPairsContainingBenchmark) {
            for (JoblineStage stage: pair.getStages()) {
                String solverName = stage.getSolver().getName();
                String configName = stage.getConfiguration().getName();
                String result = stage.getStarexecResult();
                Triple<String, String, String> row = new ImmutableTriple<String, String, String>(solverName, configName, result);
                table.add(row);
            }
        }

        request.setAttribute("tableData", table);

    } catch (Exception e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
%>
<star:template title="conflicting solvers for bench ${benchmark.name}" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared">
    <table class="conflictingSolversTable">
        <thead>
            <tr>
                <th>solver</th>
                <th>config</th>
                <th>result</th>
            </tr>
        </thead>
        <tbody>

        <c:forEach var="row" items="${tableData}">
            <tr>
                <td>${row.left}</td>
                <td>${row.middle}</td>
                <td>${row.right}</td>
            </tr>
        </c:forEach>

        </tbody>
    </table>
</star:template>
