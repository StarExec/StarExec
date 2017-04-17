<%@page import="java.util.ArrayList, java.util.List"%>
<%@page import="java.util.TreeMap" %>
<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.R, org.starexec.data.security.BenchmarkSecurity,org.starexec.data.security.GeneralSecurity,org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.util.Util, org.starexec.data.to.enums.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		String uniqueId = request.getParameter("anonId");
		boolean isAnonymousPage = uniqueId != null;
		request.setAttribute( "isAnonymousPage", isAnonymousPage );
		request.setAttribute("benchType", R.BENCHMARK);
		if ( isAnonymousPage ) {
			JspHelpers.handleAnonymousBenchPage( uniqueId, request, response );
		} else {
			JspHelpers.handleNonAnonymousBenchPage( request, response );
		}
		request.setAttribute("primitiveType", Primitive.BENCHMARK);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given benchmark id was in an invalid format");		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="${benchPageTitle}" js="common/delaySpinner, shared/copyToStardev, details/shared, details/benchmark, lib/jquery.dataTables.min" css="common/delaySpinner, details/shared, common/table, details/benchmark, shared/copyToStardev">
	<star:primitiveTypes/>
	<span style="display:none;" id="isAnonymousPage" value="${isAnonymousPage}"></span>
	<c:if test="${!isAnonymousPage}">
		<span style="display:none;" id="benchId" value="${bench.id}"></span>
		<star:primitiveIdentifier primId="${bench.id}" primType="${primitiveType.toString()}"/>
		<span style="hidden" class="benchProcessorId" value="${bench.type.id}"></span>
	</c:if>
	<c:if test="${!isAnonymousPage}">
        <c:if test="${not empty brokenBenchDeps}">
            <fieldset>
                <legend>Warning</legend>
                <span> Missing bench dependency:</span>
                <c:forEach var="brokeBench" items="${brokenBenchDeps}">
                    <br>${brokeBench.getName()}<span>
                </c:forEach>
            </fieldset>
        </c:if>
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
	</c:if>
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
					<td>processor id</td>			
					<td>${bench.type.id}</td>
				</tr>
				<tr>
					<td>description</td>			
					<td>${bench.type.description}</td>
				</tr>
				<c:if test="${!isAnonymousPage}">
					<tr>
						<td>owning community</td>			
						<td><star:community value="${com}" /></td>
					</tr>		
				</c:if>	
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
			<legend><img alt="loading" src="${starexecRoot}/images/loader.gif"> contents</legend>
			<textarea class="contentTextarea" id="benchContent" readonly="readonly" >${content}</textarea>	
			<a href="${starexecRoot}/services/benchmarks/${bench.id}/contents?limit=-1" target="_blank" class="popoutLink">popout</a>
			<p class="caption">content may be truncated. 'popout' for larger text window.</p>
		</fieldset>			
	</c:if> 

	<fieldset id="actions">
		<legend>actions</legend>

		<c:if test="${!isAnonymousPage}">
			<a id="anonymousLink">get anonymous link</a>
		
			<c:if test="${usr.id == user.id || hasAdminReadPrivileges}">
				<a id="editLink" href="${starexecRoot}/secure/edit/benchmark.jsp?id=${bench.id}">edit</a>
			</c:if>
		</c:if>
		
		<c:if test="${downloadable || hasAdminReadPrivileges}">
			<a id="downLink" href="${starexecRoot}/secure/download?type=bench&id=${bench.id}">download benchmark</a>
		</c:if>
		<c:if test="${hasAdminReadPrivileges && !isAnonymousPage}">
			<star:copyToStardevButton/>
		</c:if>
		
	</fieldset>
	<div id="dialog-warning" title="warning" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>		
	<div id="dialog-show-anonymous-link" title="anonymous link" class="hiddenDialog">
		<p>
			<span class="ui-icon ui-icon-info"></span>
			<span id="dialog-show-anonymous-link-txt"></span>
		</p>
	</div>
	<div id="dialog-confirm-anonymous-link" title="confirm anonymous link" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-anonymous-link-txt"></span></p>
	</div>
	<c:if test="${hasAdminReadPrivileges}">
		<star:copyToStardevDialog/>
	</c:if>
</star:template>
