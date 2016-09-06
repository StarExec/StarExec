<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@ page import="org.starexec.data.to.pipelines.JoblineStage" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    class TableData {
        String solver;
        String config;
        String result;

        public String getSolver() {
            return solver;
        }

        public String getConfig() {return config;}
        public String getResult() {return result;}

    }
%>
<%
    try {


        int jobId = Integer.parseInt(request.getParameter("jobId"));
        int benchId = Integer.parseInt(request.getParameter("benchId"));
        List<TableData> table = new ArrayList<TableData>();

        Benchmark benchmark = Benchmarks.get(benchId);
        request.setAttribute("benchmark", benchmark);

        List<JobPair> jobPairsContainingBenchmark = JobPairs.getPairsInJobContainingBenchmark(jobId, benchId);
        for (JobPair pair : jobPairsContainingBenchmark) {
            for (JoblineStage stage: pair.getStages()) {
                TableData row = new TableData();
                row.solver = stage.getSolver().getName();
                row.config = stage.getConfiguration().getName();
                row.result = stage.getStarexecResult();
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
                <td>${row.solver}</td>
                <td>${row.config}</td>
                <td>${row.result}</td>
            </tr>
        </c:forEach>

        </tbody>
    </table>
</star:template>
