<%@page import="java.util.ArrayList, java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int benchId = Integer.parseInt(request.getParameter("id"));
		
		Benchmark b = null;
		List<BenchmarkDependency> deps = new ArrayList<BenchmarkDependency>();
		if(Permissions.canUserSeeBench(benchId, userId)) {
			b = Benchmarks.get(benchId, true);
			deps = Benchmarks.getBenchDependencies(benchId);
		}		
		
		if(b != null) {
			request.setAttribute("usr", Users.get(b.getUserId()));
			request.setAttribute("bench", b);
			request.setAttribute("diskSize", FileUtils.byteCountToDisplaySize(b.getDiskSize()));
			Space s = Communities.getDetails(b.getType().getCommunityId());
			request.setAttribute("com", s);
			request.setAttribute("depends", deps);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");			
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given benchmark id was in an invalid format");		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${bench.name}" js="details/shared, details/benchmark, lib/jquery.dataTables.min" css="details/shared, common/comments, common/table">				
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
	
	<c:if test="${not empty depends}">
		<fieldset id="fieldDepends">
			<legend>dependencies</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>benchmark</th>
						<th>path</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach var="dependency" items="${depends}">
					<tr>
						<td><star:benchmark value="${dependency.secondaryBench}" /></td>			
						<td>${dependency.dependencyPath}</td>
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
			<textarea class=contentTextarea id="benchContent" readonly="readonly" ></textarea>	
			<a href="/starexec/services/benchmarks/${bench.id}/contents?limit=-1" target="_blank" class="popoutLink">popout</a>
			<p class="caption">contents may be truncated. 'popout' for the full content.</p>
		</fieldset>			
	</c:if>
	
	<fieldset id="commentField">
		<legend class="expd" id="commentExpd"><span>0</span> comments </legend>
			<table id="comments">
			<thead>
				<tr>
					<th style="width:20%;">user</th>
					<th>time</th>
					<th style="width:44%;">comment</th>	
					<th style="width:11%;">action</th>				
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>			
		<span id="toggleComment" class="caption"><span>+</span>add new</span>
		<div id="new_comment">
			<textarea id="comment_text" cols="60"></textarea>  
			<button id="addComment">add</button>
		</div>
	</fieldset>	
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>				
	
	<c:if test="${bench.downloadable}">
		<a id="downLink" href="/starexec/secure/download?type=bench&id=${bench.id}">download benchmark</a>
	</c:if>
</star:template>