<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
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


<star:template title="set logging level" js="admin/logging, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
		<fieldset id="fieldTable">
			<legend>Logging Levels</legend>
			<table id="tableLevels" class="shaded contentTbl">
				<thead>
					<tr>
						<th>level</th>
					</tr>
				</thead>
				<tbody>
					<tr id="clearRow" value="clear"><td>clear</td></tr>
					<tr id="offRow" value="off"><td>off</td></tr>
					<tr id="traceRow" value="trace"><td>trace</td></tr>
					<tr id="debugRow" value="debug"><td>debug</td></tr>
					<tr id="infoRow" value="info"><td>info</td></tr>
					<tr id="warnRow" value="warn"><td>warn</td></tr>
					<tr id="errorRow" value="error"><td>error</td></tr>
					<tr id="fatalRow" value="fatal"><td>fatal</td></tr>
					
				</tbody>
			</table>
	</fieldset>
		<fieldset id="actionField">
			<legend>actions</legend>
			<input id="className" type="text"/>
			<button id="applyAll">Apply Level</button>	
			<button id="applyToClass">Apply Level to Class</button>
			<button id="applyToClassAllOthersOff">Apply Level to Class And Turn Off All Other Classes</button>
		</fieldset>		
	
</star:template>
