<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.constants.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int spaceId = Integer.parseInt(request.getParameter("id"));
		request.setAttribute("nameLength", R.SPACE_NAME_LEN);
		request.setAttribute("descLength", R.SPACE_DESC_LEN);
		Space s = null;
		if (Permissions.canUserSeeSpace(spaceId,userId)) {
			s = Spaces.get(spaceId);
		}
		
		if (s != null) {
			if (!Permissions.get(userId, spaceId).isLeader()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the leaders of this space can edit details about it.");
			}
			else {
				request.setAttribute("space", s);
				Permission dflt = Permissions.getSpaceDefault(s.getId());
				if (dflt.canAddBenchmark()) {
					request.setAttribute("addBench", "checked=\"checked\" ");
				}
				if (dflt.canAddJob()) {
					request.setAttribute("addJob", "checked=\"checked\" ");
				}
				if (dflt.canAddSolver()) {
					request.setAttribute("addSolver", "checked=\"checked\" ");
				}
				if (dflt.canAddSpace()) {
					request.setAttribute("addSpace", "checked=\"checked\" ");
				}
				if (dflt.canAddUser()) {
					request.setAttribute("addUser", "checked=\"checked\" ");
				}
				if (dflt.canRemoveBench()) {
					request.setAttribute("removeBench", "checked=\"checked\" ");
				}
				if (dflt.canRemoveJob()) {
					request.setAttribute("removeJob", "checked=\"checked\" ");
				}
				if (dflt.canRemoveSolver()) {
					request.setAttribute("removeSolver", "checked=\"checked\" ");
				}
				if (dflt.canRemoveSpace()) {
					request.setAttribute("removeSpace", "checked=\"checked\" ");
				}
				if (dflt.canRemoveUser()) {
					request.setAttribute("removeUser", "checked=\"checked\" ");
				}
				if(s.isLocked()){
					request.setAttribute("isLocked", "checked=\"checked\" ");
				} else {
					request.setAttribute("isNotLocked", "checked=\"checked\" ");
				}
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Space does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given space id was in an invalid format");
	}  catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="edit ${space.name}" js="lib/jquery.validate.min, edit/space" css="edit/shared, edit/space">				
	<form id="editSpaceForm">
		<input type="hidden" name="spaceId" value="${space.id}" />
		<fieldset>
			<legend>space details</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label">space name</td>			
						<td><input id="name" type="text" name="name" value="${space.name}" length=${nameLength}></td>
					</tr>
					<tr>
						<td class="label">description</td>			
						<td><textarea id="description" name="description" length=${descLength}>${space.description}</textarea></td>
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
									<td><input type="checkbox" id="addSolver" ${addSolver}/></td>
									<td><input type="checkbox" id="addBench" ${addBench}/></td>
									<td><input type="checkbox" id="addUser" ${addUser}/></td>
									<td><input type="checkbox" id="addSpace" ${addSpace}/></td>
									<td><input type="checkbox" id="addJob" ${addJob}/></td>
								</tr>
								<tr>
									<td>remove</td>
									<td><input type="checkbox" id="removeSolver" ${removeSolver}/></td>
									<td><input type="checkbox" id="removeBench" ${removeBench}/></td>
									<td><input type="checkbox" id="removeUser" ${removeUser}/></td>
									<td><input type="checkbox" id="removeSpace" ${removeSpace}/></td>
									<td><input type="checkbox" id="removeJob" ${removeJob}/></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr>
						<td>locked</td>
						<td>
							<input id="locked" type="radio" name="locked" value="true"  ${isLocked} /><label>yes</label>
							<input id="locked" type="radio" name="locked" value="false" ${isNotLocked} /><label>no</label>
						</td>
					</tr>																			
				</tbody>
			</table>
			<button type="button" id="update">update</button>
		</fieldset>
	</form>
</star:template>