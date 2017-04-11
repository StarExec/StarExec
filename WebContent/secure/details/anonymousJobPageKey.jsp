<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.HashMap, java.util.Map, java.util.ArrayList, java.util.List, org.apache.commons.lang3.StringUtils, org.starexec.app.RESTHelpers, org.starexec.constants.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.data.to.JobStatus.JobStatusCode, org.starexec.util.*, org.starexec.data.to.enums.ProcessorType, org.starexec.util.dataStructures.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		JspHelpers.handleAnonymousJobKeyPage(request, response);
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="${job.name}" js="util/sortButtons, util/jobDetailsUtilityFunctions, common/delaySpinner, lib/jquery.jstree, lib/jquery.dataTables.min, details/shared, details/anonymousJobPageKey, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, common/delaySpinner, explore/common, details/shared, details/anonymousJobPageKey">		

<p class="noteText">Note: Only the owner of the job can see this page.</p>
<fieldset id="solverNameKeyFieldset">
	<table id="solverNameKeyTable">
		<thead>
			<tr>
				<th>Solver Name</th>
				<th>Anonymized Name</th>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="solverTriple" items="${solverTripleList}">
				<tr>
					<td>
						<a href="${starexecRoot}/secure/details/solver.jsp?id=${solverTriple.getRight()}">${solverTriple.getLeft()}</a>
						<img class="extLink" src="${starexecRoot}/images/external.png">
					</td>
					<td>${solverTriple.getMiddle()}</td>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</fieldset>

</star:template>
