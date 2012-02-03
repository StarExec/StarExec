<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		long userId = SessionUtil.getUserId(request);
		long solverId = Long.parseLong(request.getParameter("id"));
		
		Solver s = null;
		if(Permissions.canUserSeeSolver(solverId, userId)) {
			s = Solvers.get(solverId);
		}
		
		if(s != null) {
			request.setAttribute("usr", Users.get(s.getUserId()));
			request.setAttribute("solver", s);
			request.setAttribute("sites", Websites.getAll(solverId, Websites.WebsiteType.SOLVER));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given solver id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${solver.name}" js="details/shared" css="details/shared">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/solver.jsp?id=${solver.id}">edit</a>)</c:if></legend>
		<table class="shaded">
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