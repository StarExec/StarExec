<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*,org.starexec.util.Util,org.starexec.data.to.Website.WebsiteType, org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.security.*, org.starexec.data.to.*, org.starexec.data.to.enums.*, org.starexec.util.*, org.starexec.util.Util"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		

	try {
		String uniqueId = request.getParameter("anonId");
		boolean isAnonymousPage = uniqueId != null;

		if ( isAnonymousPage ) {
			JspHelpers.handleAnonymousSolverPage( uniqueId, request, response );
		} else {
			JspHelpers.handleNonAnonymousSolverPage( request, response );
		}

		request.setAttribute("primitiveType", Primitive.SOLVER);

	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given solver id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${solverPageTitle}" js="common/delaySpinner, details/shared, details/solver, details/copyToStardev, lib/jquery.dataTables.min" css="common/delaySpinner, common/table, details/shared, details/solver">			
	<star:primitiveTypes/>
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>
	<span style="display:none;" id="isAnonymousPage" value="${ isAnonymousPage }"></span>
	<c:if test="${ !isAnonymousPage }">
		<span style="display:none;" id="solverId" value="${solver.id}"> </span>
		<star:primitiveIdentifier primId="${solver.id}" primType="${primitiveType.toString()}"/>
	</c:if>
	<fieldset>
		<legend>details</legend>
		<table id="infoTable">
			<tr>
			<td id="picSection">
				<img id="showPicture" src="${starexecRoot}/secure/get/pictures?Id=${solver.id}&type=sthn" enlarge="${starexecRoot}/secure/get/pictures?Id=${solver.id}&type=sorg"><br>
				<c:if test="${ !isAnonymousPage && usr.id == user.id }">
					<a id="uploadPicture" href="${starexecRoot}/secure/add/picture.jsp?type=solver&Id=${solver.id}">change</a>
				</c:if>
			</td>
			<td id="solverDetail" class="detail">
				<table id="solverInfo" class="shaded">
					<thead>
						<tr>
							<th>attribute</th>
							<th>value</th>
						</tr>
					</thead>
					<tbody>
						<c:if test="${ !hideSolverName }">
							<tr>
								<td>name</td>			
								<td>${solver.name}</td>
							</tr>
						</c:if>
						<tr>
							<td>description</td>			
							<td>${solver.description}</td>
						</tr>
						<c:if test="${ !isAnonymousPage }">
							<tr>
								<td>owner</td>			
								<td><star:user value="${usr}" /></td>
							</tr>							
						</c:if>
						<tr>
							<td>uploaded</td>			
							<td><fmt:formatDate pattern="MMM dd yyyy" value="${solver.uploadDate}" /></td>
						</tr>			
						<tr>
							<td>disk size</td>
							<td>${diskSize}</td>
						</tr>
						<tr>
							<td>build status</td>
							<td>${buildStatus}</td>
						</tr>
					</tbody>				
				</table>
			</td>
			</tr>
		</table>		
	</fieldset>
	<fieldset>
		<legend>configurations</legend>
		<table id="tblSolverConfig" class="ConfigShaded">	
			<thead>
				<tr>
					<th>name</th>
					<th>description</th>						
				</tr>
			</thead>
			<tbody>
				<c:choose>
					<c:when test="${ isAnonymousPage && hideSolverName }">
						<c:forEach var="c" items="${configs}" varStatus="indexContainer">
							<tr>
								<td id="configItem">
									Configuration${ indexContainer.index + 1 }	
								</td>
								<td>
									${c.description}
								</td>
							</tr>
						</c:forEach>		
					</c:when>
					<c:otherwise>
						<c:forEach var="c" items="${configs}">
							<tr>
								<td id="configItem">
									<a href="${starexecRoot}/secure/details/configuration.jsp?id=${c.id}">
										${c.name}
										<img class="extLink" src="${starexecRoot}/images/external.png"/>
									</a>
								</td>
								<td>
									${c.description}
								</td>
							</tr>
						</c:forEach>		
					</c:otherwise>
				</c:choose>
			</tbody>						
		</table>						
	</fieldset>
	<c:if test="${not empty sites}">		
	<fieldset id="fieldSites">
		<legend>websites</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>link</th>
					<th>url</th>				
				</tr>
			</thead>
			<tbody>
				<c:forEach var="site" items="${sites}">
					<tr>
						<td>${site[0]}</td>
						<td><a href="${site[1]}">${site[2]}</a><img class="extLink" src="${starexecRoot}/images/external.png"/></td>
					
					</tr>
				</c:forEach>			
			</tbody>				
		</table>	
	</fieldset>	
	</c:if>
	


	<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>	
	<div id="dialog-warning" title="warning" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>		
	<div id="dialog-confirm-copy" title="confirm copy" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	<div id="dialog-confirm-anonymous-link" title="confirm anonymous link" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-anonymous-link-txt"></span></p>
	</div>
	<star:copyToStardevDialog/>
	<div id="dialog-show-anonymous-link" title="anonymous link" class="hiddenDialog">
		<p>
			<span class="ui-icon ui-icon-info"></span>
			<span id="dialog-show-anonymous-link-txt"></span>
		</p>
	</div>
    <c:if test="${!sourceDownloadable}">
    <div id="dialog-building-job" title="Building..." class="hiddenDialog">
            <p><span class="ui-icon ui-icon-info" ></span><span id="dialog-building-job-txt"></span></p>
    </div>
    </c:if>

	<!-- Displays 'download' and 'upload configuration' buttons if necessary -->
	<fieldset id="actions">
		<legend>actions</legend>
		<c:if test="${!isAnonymousPage}">
			<a id="anonymousLink">get anonymous link</a>
			<c:if test="${usr.id == user.id || hasAdminReadPrivileges}">
				<a href="${starexecRoot}/secure/add/configuration.jsp?sid=${solver.id}" id="uploadConfig">add configuration</a>
				<a href="${starexecRoot}/secure/edit/solver.jsp?id=${solver.id}" id="editLink">edit</a>
                <a href="${starexecRoot}/services/solvers/${solver.id}/buildoutput" target="_blank" id="downBuildInfo">see build info</a>
			</c:if>
		</c:if>
		<c:if test="${isAnonymousPage || downloadable || hasAdminReadPrivileges}">			
			<a type="button" id="downLink3">download</a>
		</c:if>
        <c:if test="${!isAnonymousPage && sourceDownloadable && downloadable}">
            <a id="srcLink">source</a> 
        </c:if>
		<c:if test="${hasAdminReadPrivileges}">
			<star:copyToStardevButton/>
		</c:if>

	</fieldset>
	
</star:template>
