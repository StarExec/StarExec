<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.test.*,java.util.List,org.starexec.data.security.*, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<%
	try {
		String sequenceName=request.getParameter("sequenceName");
		request.setAttribute("sequenceName",GeneralSecurity.getHTMLAttributeSafeString(sequenceName));

	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="${sequenceName}" js="admin/testResults, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin, admin/testResults">
		<span id="sequenceName" value="${sequenceName}"></span>
		<fieldset id="fieldTable">
			<legend>Existing Tests</legend>
			<table id="tableTests" class="shaded contentTbl">
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th>message</th>
						<th id="errorHead">error trace</th>
						<th>time(ms)</th>
					</tr>
				</thead>
				<tbody>

				</tbody>
			</table>
	</fieldset>
</star:template>
