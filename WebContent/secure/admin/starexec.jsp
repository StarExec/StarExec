<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*,org.starexec.jobs.JobManager, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		request.setAttribute("debugModeActive", R.DEBUG_MODE_ACTIVE);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="Admin" js="admin/starexec, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared, explore/common, admin/admin">
	<div id="actionPanel">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><button type="button" id="restartStarExec">restart StarExec</button></li>
				<li><button type="button" id="toggleDebugMode" value="${debugModeActive}">Enable debug mode</button></li>
				<li><button id="clearStatsCache">Clear Job Stats</button></li>
				<li><a href="logging.jsp"><button type="button" id="manageLogging">manage logging</button></a></li>
				<li><button type="button" id="clearLoadData">clear load balance data</button></li>
				<li><button type="button" id="clearSolverCacheData">clear compute node solver cache</button></li>
			</ul>
			<div id="dialog-confirm-restart" title="confirm restart" class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-restart-txt"></span></p>
			</div>
			<p>Starexec revision ${buildVersion} built ${buildDate}</p>
		</fieldset>
	</div>
</star:template>
