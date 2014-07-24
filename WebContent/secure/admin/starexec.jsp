<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List, org.starexec.constants.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%		
	try {
		int userId = SessionUtil.getUserId(request);
		User u = Users.get(userId);
		if (!Users.isAdmin(userId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
		} else {
			
		}		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="admin/starexec, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared, explore/common, admin/admin">
	<div id="actionPanel">
		<fieldset>
		<legend>actions</legend>
			<ul id="actionList">
				<li><button type="button" id="restartStarExec">restart StarExec</button></li>	
				<li><a href="/${starexecRoot}/secure/admin/cache.jsp"><button type="button" id="manageCache">manage cache</button></a></li>  
				<li><a href="/${starexecRoot}/secure/admin/logging.jsp"><button type="button" id="manageLogging">manage logging</button></a>
			</ul>
			<div id="dialog-confirm-restart" title="confirm restart">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-restart-txt"></span></p>
			</div>	
		</fieldset>	
	</div>	
</star:template>