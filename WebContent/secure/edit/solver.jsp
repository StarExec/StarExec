<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*, org.starexec.data.database.*,org.starexec.data.to.Website.WebsiteType, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int solverId = Integer.parseInt(request.getParameter("id"));
		request.setAttribute("solverNameLen", R.SOLVER_NAME_LEN);
		request.setAttribute("solverDescLen", R.SOLVER_DESC_LEN);
		Solver s = null;
		if(Permissions.canUserSeeSolver(solverId, userId)) {
			s = Solvers.get(solverId);
		}

		if(s != null) {
			// Ensure the user visiting this page is the owner of the solver or has admin read privileges
			if (userId == s.getUserId() || Users.hasAdminReadPrivileges(userId)) {
				request.setAttribute("solver", s);
				request.setAttribute("sites", Websites.getAllForHTML(solverId, WebsiteType.SOLVER));
				if(s.isDownloadable()){
					request.setAttribute("isDownloadable", "checked");
					request.setAttribute("isNotDownloadable", "");
				} else {
					request.setAttribute("isDownloadable", "");
					request.setAttribute("isNotDownloadable", "checked");
				}
			} else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the owner of this solver can edit details about it.");
			}
		} else {
			if (Solvers.isSolverDeleted(solverId)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "This solver has been deleted. You likely want to remove it from your spaces.");
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

<star:template title="edit ${solver.name}" js="lib/jquery.validate.min, edit/solver" css="edit/shared, edit/solver">				
	<form id="editSolverForm">

		<fieldset>
			<legend>solver details</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label">solver name</td>
						<td>
							<input id="name" type="text" name="name" value="${solver.name}">
							
						</td>
					</tr>
					<tr>
						<td class="label">description</td>			
						<td>
							<textarea id="description" name="description" length="${solverDescLen}">${solver.description}</textarea>
						</td>
					</tr>
					<tr>
						<td>downloadable</td>
						<td>
						<input id="downloadable" type="radio" name="downloadable" value="true"  ${isDownloadable}>yes
						<input id="downloadable" type="radio" name="downloadable" value="false" ${isNotDownloadable}>no
						</td>
					</tr>																			
				</tbody>
			</table>	
			<button type="button" id="delete">recycle</button>
			<button type="button" id="update">update</button>
		</fieldset>		
	</form>
	<fieldset>
		<legend>associated websites</legend>
		<table id="websites" class="shaded">
			<thead>
				<tr>
					<th>link</th>
					<th>action</th>					
				</tr>
			</thead>
			<tbody>
			<c:forEach items="${sites}" var="s">
				<tr>
					<td><a href="${s.url}">${s.name}<img class="extLink" src="${starexecRoot}/images/external.png"/></a></td>
					<td><a class="delWebsite" id="${s.id}">delete</a></td>
				</tr>
			</c:forEach>
			</tbody>
		</table>			
		<span id="toggleWebsite" class="caption"><span>+</span> add new</span>
		<div id="new_website">
			name: <input type="text" id="website_name" /> 
			url: <input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
		<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
		</div>
</star:template>
