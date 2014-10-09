<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("userId", userId);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="Personal Recycle Bin" js="common/delaySpinner, details/recycleBin, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/delaySpinner, common/table, explore/spaces, details/shared">
		<fieldset id="recycledSolverField">
			<legend class="expd" id="recycledSolverExpd"><span>0</span> recycled solvers</legend>
			<table id="rsolvers" uid="${userId}">
				<thead>
					<tr>
						<th> name </th>
						<th> description </th>
					</tr>
				</thead>
			</table>
			<button type="button" id="deleteSelectedSolvers">Delete</button>
			<button type="button" id="restoreSelectedSolvers">Restore</button>
		</fieldset>
		<fieldset id="recycledBenchField">
			<legend class="expd" id="recycledBenchExpd"><span>0</span> recycled benchmarks</legend>
			<table id="rbenchmarks" uid="${userId}">
				<thead>
					<tr>
						<th> name</th>
						<th> type</th>											
					</tr>
				</thead>		
			</table>
			<button type="button" id="deleteSelectedBenchmarks">Delete</button>
			<button type="button" id="restoreSelectedBenchmarks">Restore</button>
			
		</fieldset>	
			
		<fieldset id="actionField">
		<legend>actions</legend>
			<button type="button" id="clearBenchmarks">empty benchmark bin</button>
			<button type="button" id="clearSolvers">empty solver bin</button>
			<button type="button" id="restoreBenchmarks">restore all benchmarks</button>
			<button type="button" id="restoreSolvers">restore all solvers</button>
		</fieldset>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-confirm-restore" title="confirm restore">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-restore-txt"></span></p>
	</div>
</star:template>