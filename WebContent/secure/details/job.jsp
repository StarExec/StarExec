<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	long id = Long.parseLong(request.getParameter("id"));
	Job j = Database.getJob(id);
	request.setAttribute("usr", Database.getUser(j.getUserId()));
	request.setAttribute("job", j);	
	request.setAttribute("pairs", Database.getPairsForJob(j.getId()));
%>

<star:template title="${job.name}" js="details" css="details, job_details">				
	<fieldset>
		<legend>details</legend>
		<table>
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
		<table>
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