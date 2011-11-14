<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	long id = Long.parseLong(request.getParameter("id"));
	Solver s = Database.getSolver(id);
	request.setAttribute("usr", Database.getUser(s.getUserId()));
	request.setAttribute("solver", s);
	request.setAttribute("sites", Database.getWebsitesForSolver(id));
%>

<star:template title="${solver.name}" js="details" css="details">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="#">edit</a>)</c:if></legend>
		<table>
			<tr>
				<td>description</td>			
				<td>${solver.description}</td>
			</tr>
			<tr>
				<td>owner</td>			
				<td><star:user value="${usr}" /></td>
			</tr>							
			<tr>
				<td>uploaded</td>			
				<td><fmt:formatDate pattern="MMM dd yyyy" value="${solver.uploadDate}" /></td>
			</tr>
			<c:if test="${not empty sites}">			
			<tr>
				<td>websites</td>	
				<td>		
					<ul>
						<c:forEach var="site" items="${sites}">
							<li>${site}</li>
						</c:forEach>	
					</ul>
				</td>
			</tr>
			</c:if>				
		</table>	
	</fieldset>		
	<fieldset>
		<legend>related jobs</legend>
		<p>coming soon...</p>
	</fieldset>
	
	<c:if test="${solver.downloadable}">
	<fieldset>
		<legend>actions</legend>		
			<ul>
				<li><a id="downloadLink" href="#">download</a></li>
			</ul>					
	</fieldset>
	</c:if>	
</star:template>