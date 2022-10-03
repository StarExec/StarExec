<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("userId", userId);
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given user id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>

<star:template title="Personal Trash Bin"
               js="common/delaySpinner, details/recycleBin, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min"
               css="common/delaySpinner, common/table, explore/common, explore/spaces, details/shared, details/recycleBin">
	<fieldset id="recycledSolverField">
		<legend class="expd" id="recycledSolverExpd"><span class="list-count"></span>
			solvers
		</legend>
		<ul class="actionList">
			<li>
				<button type="button" id="clearSolvers">permanently delete all solvers in trash
				</button>
			</li>
			<li>
				<button type="button" id="restoreSolvers">restore all solvers
				</button>
			</li>
			<li>
				<button type="button" id="deleteSelectedSolvers">Delete</button>
			</li>
			<li>
				<button type="button" id="restoreSelectedSolvers">Restore
				</button>
			</li>
		</ul>
		<table id="rsolvers" uid="${userId}">
			<thead>
			<tr>
				<th> name</th>
				<th> description</th>
				<th> type</th>
			</tr>
			</thead>
		</table>
	</fieldset>

	<fieldset id="recycledBenchField">
		<legend class="expd" id="recycledBenchExpd"><span>0</span>
			benchmarks
		</legend>
		<ul class="actionList">
			<li>
				<button type="button" id="clearBenchmarks">permanently delete all benchmarks in trash
				</button>
			</li>
			<li>
				<button type="button" id="restoreBenchmarks">restore all
					benchmarks
				</button>
			</li>
			<li>
				<button type="button" id="deleteSelectedBenchmarks">Delete
				</button>
			</li>
			<li>
				<button type="button" id="restoreSelectedBenchmarks">Restore
				</button>
			</li>
		</ul>
		<table id="rbenchmarks" uid="${userId}">
			<thead>
			<tr>
				<th> name</th>
				<th> type</th>
			</tr>
			</thead>
		</table>
	</fieldset>

	<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span
				id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-confirm-restore" title="confirm restore"
	     class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span
				id="dialog-confirm-restore-txt"></span></p>
	</div>
</star:template>
