<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Jobs" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		request.setAttribute("isSystemPaused", Jobs.isSystemPaused());
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="Jobs Admin"
               js="lib/jquery.heatcolor.0.0.1.min, common/format, admin/job, lib/jquery.dataTables.min"
               css="common/table, explore/common">
	<star:panel title="jobs" withCount="true" expandable="false">
		<ul class="actionList">
			<c:if test="${isSystemPaused}">
				<li>
					<button type="button" id="resumeAll">resume all</button>
				</li>
			</c:if><c:if test="${not isSystemPaused}">
			<li>
				<button type="button" id="pauseAll">pause all</button>
			</li>
		</c:if>
		</ul>
		<table id="jobs"></table>
	</star:panel>
</star:template>
