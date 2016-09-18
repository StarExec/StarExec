<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%      
    try {
        int userId = SessionUtil.getUserId(request);
        int jobSpaceId=Integer.parseInt(request.getParameter("id"));
        JobSpace space = Spaces.getJobSpace(jobSpaceId);
        if(space==null || !JobSecurity.canUserSeeJob(space.getJobId(), userId).isSuccess()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This job does not exist or is restricted");
            return;
        }
        Job j = Jobs.get(space.getJobId());
        List<String> tableHeaders = Jobs.getJobAttributesTableHeader(jobSpaceId);
        
        if(j != null) { 
            request.setAttribute("jobId", j.getId());
            request.setAttribute("jobSpaceId", jobSpaceId);
            request.setAttribute("attributes", Jobs.getJobAttributesTable(space.getId()));
            request.setAttribute("tableHeaders", tableHeaders);
            
        } else {
                if (Jobs.isJobDeleted(space.getJobId())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "This job has been deleted. You likely want to remove it from your spaces");
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
                }
        }
    } catch (NumberFormatException nfe) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
    } catch (Exception e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
%>

<star:template title="Starexec-Result Attributes Summary" js="util/spaceTree,details/jobAttributes, lib/jquery.dataTables.min, util/jobDetailsUtilityFunctions, util/datatablesUtility, details/shared, lib/jquery.ba-throttle-debounce.min,lib/jquery.jstree" css="common/table, details/shared, details/jobattributes, details/pairsInSpace">         
    <span id="data" data-jobid="${jobId}" data-jobspaceid="${jobSpaceId}" />
        <h1>Results for space <span id="spaceId">${jobSpaceId}</span></h1>
    <div id="explorer">
        <h3>Spaces</h3>
        <ul id="exploreList">
        </ul>
    </div>
	<button class="changeTime">use CPU time</button>
    <fieldset id="attributesTableField">
        <legend>Attributes</legend>
        <table id="attributeTable">
            <thead>
                <tr>
                    <th>solver</th>
                    <th>config</th>
                    <c:forEach items="${tableHeaders}" var="tableHeader">
                    <th><c:out value="${tableHeader}" /></th>
                    </c:forEach>
                </tr>
            </thead>
        </table>
    </fieldset>
    <fieldset id="attributeTotalsTableField">
        <legend>totals</legend>
        <table id="attributeTotalsTable">
            <thead>
                <tr>
                    <th>attribute value</th>
                    <th>total</th>
                </tr>
            </thead>
        </table>
    </fieldset>
</star:template>
