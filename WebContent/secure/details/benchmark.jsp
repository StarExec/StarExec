<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int benchId = Integer.parseInt(request.getParameter("id"));
		
		Benchmark b = null;
		if(Permissions.canUserSeeBench(benchId, userId)) {
			b = Benchmarks.get(benchId, true);
		}		
		
		if(b != null) {
			request.setAttribute("usr", Users.get(b.getUserId()));
			request.setAttribute("bench", b);
			request.setAttribute("diskSize", FileUtils.byteCountToDisplaySize(b.getDiskSize()));
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

<star:template title="${bench.name}" js="details/shared, details/benchmark" css="details/shared">				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/benchmark.jsp?id=${bench.id}">edit</a>)</c:if></legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
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
				<tr>
					<td>disk size</td>			
					<td>${diskSize}</td>
				</tr>	
				<tr>
					<td>downloadable</td>			
					<td>${bench.downloadable}</td>
				</tr>		
			</tbody>
		</table>	
	</fieldset>
	<fieldset id="fieldType">
		<legend>type</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
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
			</tbody>
		</table>	
	</fieldset>			
	
	<c:if test="${not empty bench.attributes}">
		<fieldset id="fieldAttributes">
			<legend>attributes</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>key</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach var="entry" items="${bench.attributes}">
					<tr>
						<td>${entry.key}</td>
						<td>${entry.value}</td>
					</tr>
				</c:forEach>					
				</tbody>
		</table>		
			
		</fieldset>							
	</c:if>	
	
	<!-- <fieldset>
		<legend>related jobs</legend>
		<p>coming soon...</p>
	</fieldset> -->		
	
	<c:if test="${bench.downloadable}">
		<fieldset id="fieldContents">
			<legend><img alt="loading" src="/starexec/images/loader.gif"> contents</legend>
			<textarea id="benchContent" readonly="readonly"></textarea>	
			<a href="/starexec/services/benchmarks/${bench.id}/contents?limit=-1" target="_blank" class="popoutLink">popout</a>
			<p class="caption">contents may be truncated. 'popout' for the full content.</p>
		</fieldset>			
		
		<a id="downLink" href="/starexec/secure/download?type=bench&id=${bench.id}">download</a>
	</c:if>			
</star:template>