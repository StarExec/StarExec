<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		long id = Long.parseLong(request.getParameter("id"));			
		User user = Users.get(id);
		
		if(user != null) {
			request.setAttribute("usr", user);
			request.setAttribute("sites", Websites.getAll(id, Websites.WebsiteType.USER));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "User does not exist");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${usr.fullName}" js="details/shared" css="details/shared">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/account.jsp">edit</a>)</c:if></legend>
		<table>
			<tr>
				<td>e-mail address</td>			
				<td><a href="mailto:${usr.email}">${usr.email}<img class="extLink" src="/starexec/images/external.png"/></a></td>
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
							<li>${site}<img class="extLink" src="/starexec/images/external.png"/></li>
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