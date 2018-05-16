<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.R, org.starexec.data.database.Jobs, org.starexec.data.database.Permissions, org.starexec.data.database.Statistics, org.starexec.data.database.Users, org.starexec.data.to.Job, org.starexec.data.to.Processor" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		int userId = R.PUBLIC_USER_ID;
		int jobId = Integer.parseInt(request.getParameter("id"));

		Job j = null;
		if (Permissions.canUserSeeJob(jobId, userId).isSuccess()) {
			j = Jobs.get(jobId);
		}

		if (j != null) {
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);
			request.setAttribute("jobId", jobId);
			request.setAttribute(
					"pairStats", Statistics.getJobPairOverview(j.getId()));

			Processor stage1PostProc =
					j.getStageAttributesByStageNumber(1).getPostProcessor();
			request.setAttribute("firstPostProc", stage1PostProc);
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
               js="lib/jquery.dataTables.min, details/shared, details/job, lib/jquery.ba-throttle-debounce.min"
               css="common/table, details/shared, details/job">
	<fieldset>
		<legend>details</legend>
		<table id="detailTbl" class="shaded">
			<thead>
			<tr>
				<th>property</th>
				<th>value</th>
			</tr>
			</thead>
			<tbody>
			<tr title="${pairStats.pendingPairs == 0 ? 'this job has no pending pairs for execution' : 'this job has 1 or more pairs pending execution'}">
				<td>status</td>
				<td>${pairStats.pendingPairs == 0 ? 'complete' : 'incomplete'}</td>
			</tr>
			<tr title="the job creator's description for this job">
				<td>description</td>
				<td>${job.description}</td>
			</tr>
			<tr title="the user who submitted this job">
				<td>owner</td>
				<td><star:user value="${usr}"/></td>
			</tr>
			<tr title="the date/time the job was created on StarExec">
				<td>created</td>
				<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss a"
				                    value="${job.createTime}"/></td>
			</tr>
			<tr title="the total wallclock elapsed time of the job. calculated by taking the difference between the start time of earliest completed pair and the end time of the latest compelted pair">
				<td>elapsed time</td>
				<td>${pairStats.runtime / 1000} ms</td>
			</tr>
			<tr title="the postprocessor that was used to process output for this job">
				<td>postprocessor</td>
				<c:if test="${not empty firstPostProc}">
					<td title="${firstPostProc.description}">${firstPostProc.name}</td>
				</c:if>
				<c:if test="${empty firstPostProc}">
					<td>none</td>
				</c:if>
			</tr>
			<tr title="the execution queue this job was submitted to">
				<td>queue</td>
				<c:if test="${not empty job.queue}">
					<td>
						<a href="${starexecRoot}/secure/explore/cluster.jsp">${job.queue.name}
							<img class="extLink"
							     src="${starexecRoot}/images/external.png"/></a>
					</td>
				</c:if>
				<c:if test="${empty job.queue}">
					<td>unknown</td>
				</c:if>
			</tr>
			</tbody>
		</table>
	</fieldset>
	<fieldset>
		<legend>job pairs</legend>
		<table id="publicPairTbl" class="shaded">
			<thead>
			<tr>
				<th>benchmark</th>
				<th>solver</th>
				<th>config</th>
				<th>status</th>
				<th>time</th>
				<th>result</th>
				<th>space</th>
			</tr>
			</thead>
			<tbody>
			<!-- This will be populated by the job pair pagination feature -->
			</tbody>
		</table>
	</fieldset>


</star:template>
