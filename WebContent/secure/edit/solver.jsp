<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
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
			// Ensure the user visiting this page is the owner of the solver
			if(userId != s.getUserId()){
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the owner of this benchmark can edit details about it.");
			} else {
				request.setAttribute("solver", s);
				if(s.isDownloadable()){
					request.setAttribute("isDownloadable", "checked");
					request.setAttribute("isNotDownloadable", "");
				} else {
					request.setAttribute("isDownloadable", "");
					request.setAttribute("isNotDownloadable", "checked");
				}
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Solver does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given solver id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="edit ${solver.name}" js="lib/jquery.validate.min, edit/solver" css="edit/solver">				
	<form id="editSolverForm">
		<fieldset>
			<legend>solver details</legend>
			<table class="shaded">
				<tr>
					<td class="label">solver name</td>			
					<td><input id="name" type="text" name="name" value="${solver.name}" maxlength="32"/></td>
				</tr>
				<tr>
					<td class="label">description</td>			
					<td><textarea id="description" name="description" >${solver.description}</textarea></td>
				</tr>
				<tr>
					<td>downloadable</td>
					<td>
					<input id="downloadable" type="radio" name="downloadable" value="true"  ${isDownloadable}>yes
					<input id="downloadable" type="radio" name="downloadable" value="false" ${isNotDownloadable}>no
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<button type="button" id="delete">delete</button>
						<button type="button" id="update">update</button>
					</td>
				</tr>						
			</table>	
		</fieldset>		
	</form>
</star:template>