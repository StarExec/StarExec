<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Status.StatusCode"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int pairId = Integer.parseInt(request.getParameter("id"));
		
		JobPair jp = Jobs.getPairDetailed(pairId);
		
		if(jp == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist");	
		} else if(Permissions.canUserSeeJob(jp.getJobId(), userId)) {
			Job j = Jobs.getShallow(jp.getJobId());
			User u = Users.get(j.getUserId());
			request.setAttribute("pair", jp);
			request.setAttribute("job", j);
			request.setAttribute("usr", u);		
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to view this job pair");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given pair id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name} pair #${pair.id}" js="lib/jquery.dataTables.min, details/pair" css="common/table, details/shared, details/pair">			
	<fieldset>
		<legend>details</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>property</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>status</td>
					<td title="${pair.status.description}">${pair.status}</td>
				</tr>
				<tr>
					<td>execution host</td>
					<td><a href="/starexec/secure/explore/cluster.jsp">${pair.node.name}  <img class="extLink" src="/starexec/images/external.png"/></a></td>
				</tr>				
				<tr>
					<td>ran by</td>			
					<td><star:user value="${usr}" /></td>
				</tr>
				<tr>
					<td>submit time</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss.SSS a" value="${pair.queueSubmitTime}" /></td>
				</tr>							
				<tr>
					<td>start time</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss.SSS a" value="${pair.startTime}" /></td>
				</tr>
				<tr>
					<td>end time</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy  hh:mm:ss.SSS a" value="${pair.endTime}" /></td>
				</tr>
				<tr>
					<td>runtime (wallclock)</td>			
					<td>${pair.wallclockTime / 1000} ms</td>
				</tr>											
			</tbody>
		</table>	
	</fieldset>		
	<fieldset>
	<legend>run statistics</legend>	
	<c:choose>
		<c:when test="${pair.status.code == 6}">
			<p>waiting for results. try again in 2-5 minutes.</p>
		</c:when>
		<c:when test="${pair.status.code == 7}">
			<table id="pairStats" class="shaded">
				<thead>
					<tr>
						<th>property</th>
						<th>value</th>				
					</tr>		
				</thead>	
				<tbody>
					<tr>
						<td></td>
					</tr>
				</tbody>
			</table>
		</c:when>				
		<c:otherwise>		
			<p>unavailable</p>
		</c:otherwise>
	</c:choose>		
	</fieldset>		
	
	<c:if test="${pair.status.code > 4 && pair.status.code <= 12}">
		<fieldset>
			<legend>job log</legend>
			<textarea>
			</textarea>
		</fieldset>
	</c:if>
	<a href="#" id="downLink">download output</a>
</star:template>