<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Jobs, org.starexec.data.database.Spaces, org.starexec.data.database.Users, org.starexec.data.security.JobSecurity, org.starexec.data.to.Job, org.starexec.data.to.JobSpace, org.starexec.data.to.User, org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int jobSpaceId = Integer.parseInt(request.getParameter("spaceid"));
		int stageNumber = Integer.parseInt(request.getParameter("stage"));
		JobSpace space = Spaces.getJobSpace(jobSpaceId);
		if (space != null && JobSecurity.canUserSeeJob(space.getJobId(), userId)
		                                .isSuccess()) {
			Job j = Jobs.get(space.getJobId());
			JobSpace s = Spaces.getJobSpace(jobSpaceId);
			User u = Users.get(j.getUserId());
			request.setAttribute("job", j);
			request.setAttribute("jobspace", s);
			request.setAttribute("userId", userId);
			request.setAttribute("stageNumber", stageNumber);
		} else if (space == null || Jobs.isJobDeleted(space.getJobId())) {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"This job has been deleted. You likely want to remove it from your spaces"
			);
			return;
		} else {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"Job does not exist or is restricted"
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given job id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="${job.name}"
               js=" lib/jquery.jstree, util/jobDetailsUtilityFunctions, lib/jquery.dataTables.min, details/shared, details/jobPanelView, lib/jquery.ba-throttle-debounce.min, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min"
               css="common/table, explore/common, details/shared, details/jobPanelView">
	<span style="display:none" id="jobId" value="${job.id}"> </span>
	<span style="display:none" id="spaceId" value="${jobspace.id}"></span>
	<span style="display:none" id="stageNumber" value="${stageNumber}"></span>
	<div id="mainPanel">
		<form class="panelStageSelection">
			Stage: <input id="selectStageInput" type="text" name="stage"
			              value="${stageNumber}">
			<button id="selectStageButton" type="button">Show Stage</button>
			<span id="selectStageError" style="color: red; display: none;">Stage must be a positive integer.</span>
		</form>
		<fieldset id="subspaceSummaryField">
			<legend class="expd" id="subspaceExpd">subspace summaries</legend>
			<fieldset id="panelActions">
				<button id="collapsePanels">Collapse All</button>
				<button id="openPanels">Open All</button>
				<button class="changeTime ui-button-text">Use CPU Time</button>
				<button id="includeUnknown">Include Unknown Status</button>
			</fieldset>
		</fieldset>
	</div>
</star:template>
