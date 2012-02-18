<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int benchId = Integer.parseInt(request.getParameter("id"));
		
		Benchmark b = null;
		if(Permissions.canUserSeeBench(benchId, userId)) {
			b = Benchmarks.get(benchId);
		}		
		
		if(b != null) {
			request.setAttribute("usr", Users.get(b.getUserId()));
			request.setAttribute("bench", b);
			
			Space s = Communities.getDetails(b.getType().getCommunityId());
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

<star:template title="${bench.name}" js="details/shared" css="details/shared">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/benchmark.jsp?id=${bench.id}">edit</a>)</c:if></legend>
		<table class="shaded">
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
		<table class="shaded">
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
					<li><a id="downloadLink" href="/starexec/secure/download?type=bench&id=${benchmark.id}">download</a></li>
				</ul>					
		</fieldset>
	</c:if>			
</star:template>