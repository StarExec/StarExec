<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB,org.starexec.constants.R,org.starexec.data.database.Spaces, org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Permission, org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {

		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("isRoot", spaceId == 1);
		request.setAttribute("space", Spaces.get(spaceId));
		request.setAttribute("namePattern", R.PRIMITIVE_NAME_PATTERN);
		request.setAttribute("nameLength", DB.SPACE_NAME_LEN);
		request.setAttribute("descLength", DB.SPACE_DESC_LEN);
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if ((p == null || !p.canAddSpace()) &&
				!GeneralSecurity.hasAdminReadPrivileges(userId)) {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"You do not have permission to add a space here"
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The parent space id was not in the correct format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"You do not have permission to add to this space or the space does not exist" +
						e.getMessage()
		);
		return;
	}
%>

<star:template title="add subspace to ${space.name}" css="add/space"
               js="lib/jquery.validate.min, add/space">
	<form id="addForm" method="post" action="${starexecRoot}/secure/add/space">
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
					<td><input id="txtName" name="name" type="text" pattern="${namePattern}"
					           length="${nameLength}"></input></td>
				</tr>
				<tr>
					<td class="label"><p>description</p></td>
					<td><textarea id="txtDesc" name="desc" rows="6"
					              draggable="false"
					              length="${descLength}"></textarea></td>
				</tr>
				<tr>
					<td class="label">
						<p>default</p>
						<span class="showMobile">
								<br/>so = solver<br/>b = bench<br/>u = user<br/>sp = space<br/>j = job
							</span>
					</td>
					<td>
						<table id="tblDefaultPerm">
							<tr>
								<th></th>
								<th><span class="hideMobile">solver</span><span
										class="showMobile">so</span></th>
								<th><span class="hideMobile">bench</span><span
										class="showMobile">b</span></th>
								<th><span class="hideMobile">users</span><span
										class="showMobile">u</span></th>
								<th><span class="hideMobile">space</span><span
										class="showMobile">sp</span></th>
								<th><span class="hideMobile">job</span><span
										class="showMobile">j</span></th>
							</tr>
							<tr>
								<td>add</td>
								<td><input type="checkbox" name="addSolver"/>
								</td>
								<td><input type="checkbox" name="addBench"/>
								</td>
								<td><input type="checkbox" name="addUser"/></td>
								<td><input type="checkbox" name="addSpace"/>
								</td>
								<td><input type="checkbox" name="addJob"/></td>
							</tr>
							<tr>
								<td>remove</td>
								<td><input type="checkbox" name="removeSolver"/>
								</td>
								<td><input type="checkbox" name="removeBench"/>
								</td>
								<td><input type="checkbox" name="removeUser"/>
								</td>
								<td><input type="checkbox" name="removeSpace"/>
								</td>
								<td><input type="checkbox" name="removeJob"/>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td class="label"><p>locked</p></td>
					<td>
						<input type="radio" name="locked" value="true"/> yes
						<input type="radio" name="locked" value="false" checked="checked"/> no
					</td>
				</tr>
				<tr>
					<td class="label"><p>Inherit-Users</p></td>
					<td>
						<input type="radio" name="users" value="true"/> yes
						<input type="radio" name="users" value="false" checked="checked"/> no
					</td>
				</tr>
				<tr>
					<td class="label"><p>Inherit-Solvers</p></td>
					<td>
						<input type="radio" name="solvers" value="true"/> yes
						<input type="radio" name="solvers" value="false" checked="checked"/> no
					</td>
				</tr>
				<tr>
					<td class="label"><p>Inherit-Benchmarks</p></td>
					<td>
						<input type="radio" name="benchmarks" value="true"/> yes
						<input type="radio" name="benchmarks" value="false" checked="checked"/> no
					</td>
				</tr>
				<c:if test="${!isRoot}">
					<tr>
						<td class="label"><p>Sticky-Leaders</p></td>
						<td>
							<input type="radio" name="sticky" value="true"/> yes
							<input type="radio" name="sticky" value="false" checked="checked"/> no
						</td>
					</tr>
				</c:if>
				<c:if test="${isRoot}">
					<input style="display:none" type="radio" name="sticky"
					       value="false" checked="checked"/>
				</c:if>
				<tr>
					<td colspan="1">
						<button id="btnPrev" type="button">Cancel</button>
					</td>
					<td colspan="1">
						<button id="btnCreate">create</button>
					</td>
				</tr>
				</tbody>
			</table>
		</fieldset>
	</form>
</star:template>
