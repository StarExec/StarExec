<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.test.*,java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%		
	try {
		int userId = SessionUtil.getUserId(request);
		User user = Users.get(userId);
		if (!Users.isAdmin(userId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		}		
		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="run diagnostic tests" js="admin/testing, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin, admin/testing">	
		<fieldset id="fieldTable">
			<legend>Existing Tests</legend>
			<table id="tableTests" class="shaded contentTbl">
				<thead>
					<tr>
						<th>name</th>
						<th id="totalHead">total tests</th>
						<th id="passedHead">tests passed</th>
						<th id="failedHead">tests failed</th>
						<th>status</th>
						<th id="errorHead">error trace</th>
					</tr>
				</thead>
				<tbody>
								
				</tbody>
			</table>
	</fieldset>
		<fieldset id="actionField">
			<legend>actions</legend>
			
			<button id="runAll">Run All Tests</button>	
			<button id="runSelected">Run Selected Tests</button>
			<button id="runStress">Create Stress Test</button>
		</fieldset>		
	
</star:template>