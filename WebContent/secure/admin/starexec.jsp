<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.R, org.starexec.app.RESTHelpers" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		request.setAttribute("debugModeActive", R.DEBUG_MODE_ACTIVE);
		request.setAttribute("freezePrimitives", RESTHelpers.freezePrimitives());
		request.setAttribute("readOnly", RESTHelpers.getReadOnly());
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
<star:template title="Admin"
               js="admin/starexec, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min"
               css="common/table, details/shared, explore/common, admin/admin">
	<div id="actionPanel">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li>
					<button type="button" id="restartStarExec">restart
						StarExec
					</button>
				</li>
				<li>
					<button type="button" id="toggleDebugMode"
					        value="${debugModeActive}">Enable debug mode
					</button>
				</li>
				<li>
					<button id="clearStatsCache">Clear Job Stats</button>
				</li>
				<li><a href="logging.jsp">
					<button type="button" id="manageLogging">manage logging
					</button>
				</a></li>
				<li>
					<button type="button" id="clearLoadData">clear load balance
						data
					</button>
				</li>
				<li>
					<button type="button" id="clearSolverCacheData">clear
						compute node solver cache
					</button>
				</li>
				<li><a href="status.jsp">
					<button type="button" id="manageStatus">manage status message
					</button>
				</a></li>
			</ul>
			<div id="dialog-confirm-restart" title="confirm restart"
			     class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span
						id="dialog-confirm-restart-txt"></span></p>
			</div>
			<p>Starexec revision ${buildVersion} built ${buildDate}</p>
		</fieldset>
		<fieldset>
			<legend>freeze primitives</legend>
			<p>
				When frozen, no new solvers or benchmarks can be uploaded.
				Solvers and benchmarks cannot be copied between spaces
				(though they can still be <em>linked</em>).
			</p>
			<p id="frozenDesc">
				This mode is designed to be used when migrating primitives to a new drive.
			</p>
			<ul id="actionList">
				<li>
					<button type="button" id="toggleFreezePrimitives">
						${freezePrimitives ? "Unfreeze" : "Freeze"} Primitives
					</button>
				</li>
			</ul>
			<script>
				var star = star || {};
				star.freezePrimitives = ${freezePrimitives}
				star.readOnly = ${readOnly}


			</script>
		</fieldset>
		<fieldset>
			<legend>read only</legend>
			<p id="readOnly">When read-only mode is enabled, no new jobs can be created.</p>
			<ul id="actionList">
				<li>
					<button type="button" id="toggleReadOnly">
						${readOnly ? "Disable" : "Enable"} Read Only
					</button>
				</li>
			</ul>
		</fieldset>
	</div>
</star:template>
