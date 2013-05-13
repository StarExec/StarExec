<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.constants.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%	
	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("space", Spaces.get(spaceId));
		request.setAttribute("nameLength", R.SPACE_NAME_LEN);
		request.setAttribute("descLength", R.SPACE_DESC_LEN);
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if(!p.canAddSpace()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add a space here");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<star:template title="add subspace to ${space.name}" css="add/space" js="lib/jquery.validate.min, add/space">
	<form id="addForm" method="post" action="/${starexecRoot}/secure/add/space">	
		<input type="hidden" name="parent" value="${space.id}"/>
		<fieldset>
			<legend>new space</legend>
			<table id="tblSpace" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label"><p>name</p></td>
						<td><input id="txtName" name="name" type="text" length=${nameLength}></input></td>
					</tr>
					<tr>
						<td class="label"><p>description</p></td>
						<td><textarea id="txtDesc" name="desc" rows="6" draggable="false" length=${descLength}></textarea></td>
					</tr>
					<tr>
						<td class="label"><p>default</p></td>
						<td>			
							<table id="tblDefaultPerm">
								<tr>
									<th></th>
									<th>solver</th>
									<th>bench</th>
									<th>users</th>
									<th>space</th>
									<th>jobs</th>
								</tr>
								<tr>
									<td>add</td>
									<td><input type="checkbox" name="addSolver"/></td>
									<td><input type="checkbox" name="addBench"/></td>
									<td><input type="checkbox" name="addUser"/></td>
									<td><input type="checkbox" name="addSpace"/></td>
									<td><input type="checkbox" name="addJob"/></td>
								</tr>
								<tr>
									<td>remove</td>
									<td><input type="checkbox" name="removeSolver"/></td>
									<td><input type="checkbox" name="removeBench"/></td>
									<td><input type="checkbox" name="removeUser"/></td>
									<td><input type="checkbox" name="removeSpace"/></td>
									<td><input type="checkbox" name="removeJob"/></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td class="label"><p>locked</p></td>
						<td>			
							<input type="radio" name="locked" value="true" /> yes
							<input type="radio" name="locked" value="false" checked="checked"/> no
						</td>
					</tr>
					<tr>
						<td colspan="1"><button id="btnPrev">back</button></td>						
						<td colspan="1"><button id="btnCreate">create</button></td>
					</tr>
				</tbody>
			</table>		
		</fieldset>
	</form>		
</star:template>