<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int solverId = Integer.parseInt(request.getParameter("id"));
		
		Solver s = null;
		if(Permissions.canUserSeeSolver(solverId, userId)) {
			s = Solvers.get(solverId);
		}
		
		if(s != null) {
			request.setAttribute("usr", Users.get(s.getUserId()));
			request.setAttribute("solver", s);
			request.setAttribute("sites", Websites.getAll(solverId, Websites.WebsiteType.SOLVER));
			request.setAttribute("diskSize", FileUtils.byteCountToDisplaySize(s.getDiskSize()));
			request.setAttribute("configs", Solvers.getConfigsForSolver(s.getId()));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given solver id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${solver.name}" js="details/shared, details/solver, lib/jquery.dataTables.min" css="details/shared, common/table">				
	<div id="popDialog" style="display: none;">
  		<img id="popImage" src=""/>
  		<img class="close-image" src="http://residentialsearch.savills.co.uk/Content/Images/icon_close.png" />
	</div>
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/solver.jsp?id=${solver.id}">edit</a>)</c:if></legend>
			<table>
				<tr>
				<td style="vertical-align: center">
					<img src= "/starexec/secure/get/pictures?Id=${solver.id}&type=sthn" height = 160" enlarge="/starexec/secure/get/pictures?Id=${solver.id}&type=sorg"><br>
					<a class="uploadConfig" id="uploadPicture" href="/starexec/secure/add/picture.jsp?type=solver&Id=${userId}">Change</a>
				</td>
				<td style="vertical-align: top">
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
								<td>${solver.name}</td>
							</tr>
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
						</tbody>				
					</table>
				</td>
			</tr>
		</table>
	</fieldset>
	<fieldset>
		<legend>configurations</legend>
		<table id="tblSolverConfig" class="shaded">	
			<thead>
				<tr>
					<th>name</th>
					<th>description</th>						
				</tr>
			</thead>
			<tbody>
				<c:forEach var="c" items="${configs}">
				<tr>
					<td style="width:150px;">
						<a href="/starexec/secure/details/configuration.jsp?id=${c.id}">${c.name}<img class="extLink" src="/starexec/images/external.png"/></a>
					</td>
					<td>
						${c.description}
					</td>
				</tr>
				</c:forEach>			
			</tbody>						
		</table>				
	</fieldset>
	<c:if test="${not empty sites}">		
	<fieldset id="fieldSites">
		<legend>websites</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>link(s)</th>					
				</tr>
			</thead>
			<tbody>
				<c:forEach var="site" items="${sites}">
					<tr><td><a href="${site.url}" target="_blank">${site.name} <img class="extLink" src="/starexec/images/external.png"/></a></td></tr>
				</c:forEach>			
			</tbody>				
		</table>	
	</fieldset>	
	</c:if>
	
	<!-- <fieldset>
		<legend>related jobs</legend>
		<p>coming soon...</p>
	</fieldset>  -->
	
	<!-- Displays 'download' and 'upload configuration' buttons if necessary -->
	<c:choose>
		<c:when test="${usr.id == user.id && solver.downloadable}">
			<a href="/starexec/secure/download?type=solver&id=${solver.id}" id="downLink">download</a>
			<a href="/starexec/secure/add/configuration.jsp?sid=${solver.id}" id="uploadConfig" class="uploadConfig">upload configuration</a>
		</c:when>
		<c:when test="${usr.id != user.id && solver.downloadable}">
			<a href="/starexec/secure/download?type=solver&id=${solver.id}" id="downLink">download</a>
		</c:when>
		<c:when test="${usr.id == user.id && !solver.downloadable}">
			<a href="/starexec/secure/add/configuration.jsp?sid=${solver.id}" id="uploadConfigMargin" class="uploadConfig">upload configuration</a>
		</c:when>
	</c:choose>
</star:template>