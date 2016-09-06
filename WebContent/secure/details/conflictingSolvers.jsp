<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@ page import="org.starexec.data.to.pipelines.JoblineStage" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    try {


        int jobId = Integer.parseInt(request.getParameter("jobId"));
        int benchId = Integer.parseInt(request.getParameter("benchId"));

    } catch (Exception e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
%>
<star:template title="" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared">
    <table class="conflictingSolversTable">
        <thead>
        </thead>
        <tbody>
        <%--
        <c:forEach var="bench" items="${conflictingBenchmarks}">
            <tr>
                <td>${bench.name}</td>
            </tr>
        </c:forEach>
        --%>
        </tbody>
    </table>
</star:template>
