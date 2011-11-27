<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		long userId = SessionUtil.getUserId(request);
		long benchId = Long.parseLong(request.getParameter("id"));
		
		Benchmark b = Database.getBenchmark(benchId, userId);		
		
		if(b != null) {
			request.setAttribute("usr", Database.getUser(b.getUserId()));
			request.setAttribute("bench", b);
			
			Space s = Database.getCommunityDetails(b.getType().getCommunityId());
			request.setAttribute("com", s);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");			
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given benchmark id was in an invalid format");		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${bench.name}" js="details" css="details">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="#">edit</a>)</c:if></legend>
		<table>
			<tr>
				<td>description</td>			
				<td>${bench.description}</td>
			</tr>
			<tr>
				<td>owner</td>			
				<td><star:user value="${usr}" /></td>
			</tr>							
			<tr>
				<td>uploaded</td>			
				<td><fmt:formatDate pattern="MMM dd yyyy" value="${bench.uploadDate}" /></td>
			</tr>			
		</table>	
	</fieldset>
		<fieldset>
		<legend>type</legend>
		<table>
			<tr>
				<td>name</td>			
				<td>${bench.type.name}</td>
			</tr>
			<tr>
				<td>description</td>			
				<td>${bench.type.description}</td>
			</tr>
			<tr>
				<td>owning community</td>			
				<td><star:community value="${com}" /></td>
			</tr>		
		</table>	
	</fieldset>			
	<fieldset>
		<legend>related jobs</legend>
		<p>coming soon...</p>
	</fieldset>
	
	<c:if test="${bench.downloadable}">
		<fieldset>
			<legend>actions</legend>		
				<ul>
					<li><a id="downloadLink" href="#">download</a></li>
				</ul>					
		</fieldset>
	</c:if>			
</star:template>