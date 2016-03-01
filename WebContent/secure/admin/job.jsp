<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<%		
	try {
		request.setAttribute("isSystemPaused", Jobs.isSystemPaused());	
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/job, lib/jquery-ui-1.8.16.custom.min.js, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/table, explore/common, explore/spaces, admin/admin, jqueryui/jquery-ui-1.8.16.starexec">
	
	
	<fieldset  id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<table id="jobs">
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th>completed</th>
						<th>total</th>
						<th>failed</th>
						<th>time</th>
					</tr>
				</thead>			
			</table>
	</fieldset>
	<fieldset>
		<legend>actions</legend>
			<ul id="actionList">
				<c:if test="${isSystemPaused}">
					<li><button type="button" id="resumeAll">resume all</button></li>
				</c:if>
				<c:if test="${not isSystemPaused}">
					<li><button type="button" id="pauseAll">pause all</button></li>
				</c:if>
			</ul>
	</fieldset>	
	<div id="dialog-confirm-pause" title="confirm pause" class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-pause-txt"></span></p>
	</div>
</star:template>
