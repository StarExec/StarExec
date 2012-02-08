<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int jobId = Integer.parseInt(request.getParameter("id"));
		
		Job j = null;
		if(Permissions.canUserSeeJob(jobId, userId)) {
			j = Jobs.get(jobId);
		}
		
		if(j != null) {			
			request.setAttribute("usr", Users.get(j.getUserId()));
			request.setAttribute("job", j);	
			request.setAttribute("pairs", Jobs.getPairs(j.getId(), userId));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given job id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${job.name}" js="details/shared" css="details/shared, details/job">			
	<fieldset>
		<legend>details</legend>
		<table class="shaded">
			<tr>
				<td>status</td>			
				<td>${job.status}</td>
			</tr>
			<tr>
				<td>description</td>			
				<td>${job.description}</td>
			</tr>
			<tr>
				<td>owner</td>			
				<td><star:user value="${usr}" /></td>
			</tr>							
			<tr>
				<td>submitted</td>			
				<td><fmt:formatDate pattern="MMM dd yyyy hh:mm:ss.SSS a" value="${job.submitted}" /></td>
			</tr>
			<tr>
				<td>finished</td>			
				<td><fmt:formatDate pattern="MMM dd yyyy hh:mm:ss.SSS a" value="${job.finished}" /></td>
			</tr>
			<tr>
				<td>runtime</td>			
				<td>${job.runTime}</td>
			</tr>			
		</table>	
	</fieldset>		
	<fieldset>
	<legend>job pairs</legend>
	<c:if test="${not empty pairs}">		
		<table class="shaded">
			<tr>
				<th>benchmark</th>
				<th>solver</th>
				<th>config</th>
				<th>status</th>
				<th>result</th>
				<!--<th>start</th>
				<th>end</th> -->
				<th>runtime</th>
			</tr>			
			<c:forEach var="pair" items="${pairs}">
				<tr>
					<td><star:benchmark value="${pair.benchmark}" /></td>
					<td><star:solver value="${pair.solver}" /></td>
					<td><star:config value="${pair.solver.configurations[0]}" /></td>				
					<td>${pair.status}</td>
					<td>${pair.result}</td>
					<!-- <td><fmt:formatDate pattern="MMM dd yyyy hh:mm:ss.SSS a" value="${pair.startDate}" /></td>					
					<td><fmt:formatDate pattern="MMM dd yyyy hh:mm:ss.SSS a" value="${pair.endDate}" /></td> -->					
					<td>${pair.runTime}</td>
				</tr>
			</c:forEach>
		</table>
	</c:if>		
	<c:if test="${empty pairs}">
		<p>none</p>
	</c:if>		
	</fieldset>	
	<fieldset>
		<legend>actions</legend>		
			<ul>
				<li><a id="downloadLink" href="#">download</a></li>
				<li><a href="#">run</a></li>
			</ul>					
	</fieldset>	
</star:template>