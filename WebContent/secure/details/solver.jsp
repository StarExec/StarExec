<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*,org.starexec.util.Util, org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.security.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.util.Util"%>
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
			List<Website> sites=Websites.getAll(solverId,Websites.WebsiteType.SOLVER);
			//we need two versions of every website URL-- one for insertion into an attribute and
			//one for insertion into the HTML body. This data structure represents every site with 3 strings
			//first the name, then the attribute URL, then the body URL
			List<String[]> formattedSites=new ArrayList<String[]>();
			for (Website site : sites) {
				String[] formattedSite=new String[3];
				formattedSite[0]=GeneralSecurity.getHTMLSafeString(site.getName());
				formattedSite[1]=GeneralSecurity.getHTMLAttributeSafeString(site.getUrl());
				formattedSite[2]=GeneralSecurity.getHTMLSafeString(site.getUrl());
				formattedSites.add(formattedSite);
			}
			request.setAttribute("sites", formattedSites);
			request.setAttribute("diskSize", Util.byteCountToDisplaySize(s.getDiskSize()));
			request.setAttribute("configs", Solvers.getConfigsForSolver(s.getId()));
			

			request.setAttribute("isAdmin",Users.isAdmin(userId));
			boolean downloadable=SolverSecurity.canUserDownloadSolver(solverId,userId).isSuccess();
			
		
			request.setAttribute("downloadable",downloadable);
		} else {
			if (Solvers.isSolverDeleted(solverId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been deleted. You likely want to remove it from your spaces.");
			}
			else if (Solvers.isSolverRecycled(solverId))  {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been moved to the recycle bin by its owner.");
			}
			else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
			}
			
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given solver id was in an invalid format");
	} catch (Exception e) {
		
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${solver.name}" js="common/delaySpinner, details/shared, details/solver, lib/jquery.dataTables.min" css="common/delaySpinner, common/table, details/shared">				
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>
	<span style="display:none;" id="solverId" value="${solver.id}"> </span>
		<fieldset>
		<legend>details</legend>
		<table id="infoTable">
			<tr>
			<td id="picSection">
				<img id="showPicture" src="/${starexecRoot}/secure/get/pictures?Id=${solver.id}&type=sthn" enlarge="/${starexecRoot}/secure/get/pictures?Id=${solver.id}&type=sorg"><br>
					<c:choose>
					<c:when test="${usr.id == user.id}">
						<a id="uploadPicture" href="/${starexecRoot}/secure/add/picture.jsp?type=solver&Id=${solver.id}">change</a>
					</c:when>
					</c:choose>
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
						<tr>
							<td>disk size</td>
							<td>${diskSize}</td>
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
					<c:forEach var="c" items="${configs}">
					<tr>
						<td id="configItem">
							<a href="/${starexecRoot}/secure/details/configuration.jsp?id=${c.id}">${c.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a>
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
					<th>link</th>
					<th>url</th>				
				</tr>
			</thead>
			<tbody>
				<c:forEach var="site" items="${sites}">
					<tr>
						<td>${site[0]}</td>
						<td><a href="${site[1]}">${site[2]}</a><img class="extLink" src="/${starexecRoot}/images/external.png"/></td>
					
					</tr>
				</c:forEach>			
			</tbody>				
		</table>	
	</fieldset>	
	</c:if>
	


	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>	
	<div id="dialog-warning" title="warning">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>		
	<div id="dialog-confirm-copy" title="confirm copy">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	
	<!-- Displays 'download' and 'upload configuration' buttons if necessary -->
	<fieldset id="actions">
		<legend>actions</legend>
		<c:if test="${downloadable}">			
			<button type="button" id="downLink3">download</button>
		</c:if>
		<c:if test="${usr.id == user.id}">
			
			<a href="/${starexecRoot}/secure/add/configuration.jsp?sid=${solver.id}" id="uploadConfig">add configuration</a>
			<a href="/${starexecRoot}/secure/edit/solver.jsp?id=${solver.id}" id="editLink">edit</a>
			<a href="/${starexecRoot}/services/solvers/${solver.id}/buildoutput" target="_blank" class="popoutLink">see build info</a>
			
		</c:if>

	</fieldset>
	
</star:template>