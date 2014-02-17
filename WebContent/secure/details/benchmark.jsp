<%@page import="java.util.ArrayList, java.util.List"%>
<%@page import="java.util.TreeMap" %>
<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.util.Util"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int benchId = Integer.parseInt(request.getParameter("id"));
		
		Benchmark b = null;
		TreeMap<String,String> attrs = new TreeMap<String,String>();
		List<BenchmarkDependency> deps = new ArrayList<BenchmarkDependency>();
		if(Permissions.canUserSeeBench(benchId, userId)) {
			b = Benchmarks.get(benchId, true);
			attrs = Benchmarks.getSortedAttributes(benchId);
			deps = Benchmarks.getBenchDependencies(benchId);
		}		
		
		if(b != null) {
			request.setAttribute("usr", Users.get(b.getUserId()));
			request.setAttribute("bench", b);
			request.setAttribute("diskSize", Util.byteCountToDisplaySize(b.getDiskSize()));		
			//save the integer codes for benchmark-related cache items. This way, 
			//if the admin decides to clear the cache for the item, we can query the server with the right code
			request.setAttribute("cacheType",CacheType.CACHE_BENCHMARK.getVal());
			request.setAttribute("isAdmin",Users.isAdmin(userId));
			Space s = Communities.getDetails(b.getType().getCommunityId());
			if (s==null) {
				s=new Space();
				s.setName("none");
			}
			request.setAttribute("com", s);
			request.setAttribute("depends", deps);
			request.setAttribute("attributes",attrs);
			boolean down=b.isDownloadable();
			if (b.getUserId()==userId) {
				down=true;
			}
			request.setAttribute("downloadable",down);
		} else {
			if (Benchmarks.isBenchmarkDeleted(benchId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This benchmark has been deleted. You likely want to remove it from your spaces");
			} else if (Benchmarks.isBenchmarkRecycled(benchId))  {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This benchmark has been moved to the recycle bin by its owner.");
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");	
			}
					
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given benchmark id was in an invalid format");		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${bench.name}" js="lib/jquery.cookie, common/delaySpinner, details/shared, details/benchmark, lib/jquery.dataTables.min" css="common/delaySpinner, details/shared, common/table">				
	<span style="display:none;" id="benchId" value="${bench.id}"></span>
	<fieldset>
		<legend>details</legend>
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
	
	<c:if test="${not empty attributes}">
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
				<c:forEach var="entry" items="${attributes}">
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

	<c:if test="${downloadable}">
		<fieldset id="fieldContents">
			<legend><img alt="loading" src="/${starexecRoot}/images/loader.gif"> contents</legend>
			<textarea class="contentTextarea" id="benchContent" readonly="readonly" ></textarea>	
			<a href="/${starexecRoot}/services/benchmarks/${bench.id}/contents?limit=-1" target="_blank" class="popoutLink">popout</a>
			<p class="caption">contents may be truncated. 'popout' for the full content.</p>
		</fieldset>			
	</c:if> 

	<fieldset id="actions">
		<legend>actions</legend>
	
		<c:if test="${usr.id == user.id}">
			<a id="editLink" href="/${starexecRoot}/secure/edit/benchmark.jsp?id=${bench.id}">edit</a>
		</c:if>
	 	
		<c:if test="${downloadable}">
			<a id="downLink" href="/${starexecRoot}/secure/download?type=bench&id=${bench.id}">download benchmark</a>
		</c:if>
		
		<c:if test="${isAdmin}">
			<span id="cacheType" value="${cacheType}"></span>
			<button type="button" id="clearCache">clear cache</button>
		</c:if>
		
	</fieldset>
	<div id="dialog-warning" title="warning">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>		
</star:template>