<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.User"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	long id = Long.parseLong(request.getParameter("id"));	
	request.setAttribute("usr", Database.getUser(id));
	request.setAttribute("sites", Database.getWebsitesByUserId(id));
%>

<star:template title="${usr.fullName}" js="details" css="details">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/pages/edit_account.jsp">edit</a>)</c:if></legend>
		<table>
			<tr>
				<td>e-mail address</td>			
				<td><a href="mailto:${usr.email}">${usr.email}</a></td>
			</tr>				
			<tr>
				<td>institution</td>			
				<td>${usr.institution}</td>
			</tr>
			<tr>
				<td>member since</td>			
				<td><fmt:formatDate pattern="MMM dd yyyy" value="${usr.createDate}" /></td>
			</tr>
			<tr>
				<td>member type</td>			
				<td>${usr.role}</td>
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
		<legend>solvers</legend>
		<p>coming soon...</p>
	</fieldset>
	<fieldset>
		<legend>benchmarks</legend>
		<p>coming soon...</p>
	</fieldset>
	<fieldset>
		<legend>jobs</legend>
		<p>coming soon...</p>
	</fieldset>
</star:template>