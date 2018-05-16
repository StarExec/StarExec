<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB,org.starexec.data.database.Communities, org.starexec.data.database.Permissions, org.starexec.data.database.Spaces, org.starexec.data.security.GeneralSecurity, org.starexec.data.to.Permission, org.starexec.data.to.Space, org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int spaceId = Integer.parseInt(request.getParameter("id"));
		request.setAttribute("nameLength", DB.SPACE_NAME_LEN);
		request.setAttribute("descLength", DB.SPACE_DESC_LEN);
		Space s = null;
		if (Permissions.canUserSeeSpace(spaceId, userId)) {
			s = Spaces.get(spaceId);
		}

		if (s != null) {
			Permission userPermission = Permissions.get(userId, spaceId);
			if (!GeneralSecurity.hasAdminReadPrivileges(userId) &&
					(userPermission == null || !userPermission.isLeader())) {
				response.sendError(
						HttpServletResponse.SC_FORBIDDEN,
						"Only the leaders of this space can edit details about it."
				);
				return;
			} else {
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
					request.setAttribute(
							"removeSolver", "checked=\"checked\" ");
				}
				if (dflt.canRemoveSpace()) {
					request.setAttribute("removeSpace", "checked=\"checked\" ");
				}
				if (dflt.canRemoveUser()) {
					request.setAttribute("removeUser", "checked=\"checked\" ");
				}
				if (s.isLocked()) {
					request.setAttribute("isLocked", "checked=\"checked\" ");
				} else {
					request.setAttribute("isNotLocked", "checked=\"checked\" ");
				}
				if (s.isStickyLeaders()) {
					request.setAttribute("isSticky", "checked=\"checked\"");
				} else {
					request.setAttribute("isNotSticky", "checked=\"checked\"");
				}
				request.setAttribute("isCommunity",
				                     (Communities.isCommunity(s.getId()) ||
						                     Spaces.isRoot(s.getId()))
				);
			}
		} else {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"Space does not exist or is restricted"
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given space id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="edit ${space.name}"
               js="lib/jquery.validate.min, edit/space"
               css="edit/shared, edit/space">
	<form id="editSpaceForm">
		<input type="hidden" name="spaceId" value="${space.id}"/>
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
					<td><input id="name" type="text" name="name"
					           value="${space.name}" length="${nameLength}">
					</td>
				</tr>
				<tr>
					<td class="label">description</td>
					<td><textarea id="description" name="description"
					              length=${descLength}>${space.description}</textarea>
					</td>
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
								<td><input type="checkbox"
								           id="addSolver" ${addSolver}/></td>
								<td><input type="checkbox"
								           id="addBench" ${addBench}/></td>
								<td><input type="checkbox"
								           id="addUser" ${addUser}/></td>
								<td><input type="checkbox"
								           id="addSpace" ${addSpace}/></td>
								<td><input type="checkbox"
								           id="addJob" ${addJob}/></td>
							</tr>
							<tr>
								<td>remove</td>
								<td><input type="checkbox"
								           id="removeSolver" ${removeSolver}/>
								</td>
								<td><input type="checkbox"
								           id="removeBench" ${removeBench}/>
								</td>
								<td><input type="checkbox"
								           id="removeUser" ${removeUser}/></td>
								<td><input type="checkbox"
								           id="removeSpace" ${removeSpace}/>
								</td>
								<td><input type="checkbox"
								           id="removeJob" ${removeJob}/></td>
							</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td>locked</td>
					<td>
						<input id="locked" type="radio" name="locked"
						       value="true"  ${isLocked} /><label>yes</label>
						<input id="locked" type="radio" name="locked"
						       value="false" ${isNotLocked} /><label>no</label>
					</td>
				</tr>
				<c:if test="${!isCommunity}">
					<tr>
						<td>sticky leaders</td>
						<td>
							<input id="sticky" type="radio" name="sticky"
							       value="true" ${isSticky} /><label>yes</label>
							<input id="notSticky" type="radio" name="sticky"
							       value="false" ${isNotSticky} /><label>no</label>
						</td>
					</tr>
				</c:if>
				<c:if test="${isCommunity}">
					<!-- Include the sticky input, but don't set it to sticky -->
					<input style="display:none" id="sticky" type="radio"
					       name="sticky" value="true"/><label></label>
				</c:if>
				</tbody>
			</table>
			<button type="button" id="update">update</button>
			<button type="button" id="btnPrev">Cancel</button>
		</fieldset>
	</form>
</star:template>
