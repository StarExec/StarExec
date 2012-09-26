<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		/*
		// Gather solver and user information
		
		int solverId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		Solver solver = Solvers.get(solverId);
		
		// Verify this user is the owner of the solver they are trying to upload configurations to
		if(solver.getUserId() == userId) {
			request.setAttribute("solver", solver);
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add configurations here.");
		}
		*/
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad request.");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "not found");		
	}
%>
<star:template title="Create your own job" css="add/singleJobPair" js="lib/jquery.validate.min, add/singleJobPair">

	<form method="POST" action="/starexec/public/misc/SingleJobPair" id="singleJobPairForm">
		<input type="hidden" name="solverId" value="${solver.id}"/>
		<fieldset id="save">
			<legend>write a benchmark</legend>		
			<table id="saveBenchmarkTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>

					<tr>
						<td>benchmark contents</td>
						<td><textarea id="benchmarkContents" name="benchmarkContents" rows="60" cols="50"/></textarea></td>
					</tr>
				</tbody>
			</table>	
			<button class="cancelBtn" type="button">cancel</button>																
			<button class="saveBtn" type="submit">save</button>
		</fieldset>
	</form>
</star:template>