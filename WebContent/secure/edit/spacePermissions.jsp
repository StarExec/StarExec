<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.Communities, org.starexec.data.database.Spaces, org.starexec.data.database.Users, org.starexec.data.security.SpaceSecurity, org.starexec.data.to.Space, org.starexec.util.SessionUtil, java.util.List" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int spaceId = Integer.parseInt(request.getParameter("id"));
		int userId = SessionUtil.getUserId(request);

		request.setAttribute("userId", userId);
		request.setAttribute("isAdmin", Users.isAdmin(userId));

		List<Space> communities = Communities.getAll();
		StringBuilder communityIdList = new StringBuilder();

		if (!communities.isEmpty()) {
			for (Space c : communities) {
				communityIdList.append(c.getId());
				communityIdList.append(",");
			}
			communityIdList.delete(communityIdList.length() - 1,
			                       communityIdList.length()
			);
			request.setAttribute("communityIdList", communityIdList.toString());
		} else {
			request.setAttribute("communityIdList", "1");
		}

		if (SpaceSecurity.canUserSeeSpace(spaceId, userId).isSuccess() &&
				spaceId > 0) {
			List<Integer> idChain = Spaces.getChainToRoot(spaceId);
			StringBuilder stringChain = new StringBuilder();
			for (Integer id : idChain) {
				stringChain.append(id);
				stringChain.append(",");
			}
			stringChain.delete(stringChain.length() - 1, stringChain.length());
			request.setAttribute("spaceChain", stringChain.toString());
		} else {
			request.setAttribute("spaceChain", "1");
		}
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="edit permissions"
               js="util/spaceTree, common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.jstree, edit/spacePermissions, util/datatablesUtility, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min, shared/sharedFunctions"
               css="common/delaySpinner, common/table, explore/common, explore/spaces, edit/spacePermissions">
	<span id="userId" value="${userId}"></span>
	<span id="isAdmin" value="${isAdmin}"></span>
	<span id="spaceChain" value="${spaceChain}"></span>
	<span id="communityIdList" value="${communityIdList}"></span>
	<div id="explorer">
		<h3>spaces</h3>
		<ul id="exploreList"> </ul>
	</div>

	<div id="detailPanel">
		<h3 class="spaceName"></h3>
		<p id="spaceLeader" class="accent"></p>
		<p id="spaceDesc" class="accent"></p>
		<p id="spaceID" class="accent"></p>
		<fieldset id="userField">
			<legend id="usersLegend" class="userExpd"><span class="list-count"></span> users</legend>
			<table id="usersTable">
				<thead>
				<tr>
					<th>name</th>
					<th>institution</th>
					<th>email</th>
				</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>

		<fieldset id="currentPerms" hidden>
			<legend>selected user permissions</legend>
			<table id="userPermissions">
				<thead>
				<tr>
					<th>property</th>
					<th>add</th>
					<th>remove</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>job</td>
					<td><span id="uaddJob" class="ui-icon ui-icon-help"></span>
					</td>
					<td><span id="uremoveJob"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>

				<tr>
					<td>solver</td>
					<td><span id="uaddSolver"
					          class="ui-icon ui-icon-help"></span></td>
					<td><span id="uremoveSolver"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>

				<tr>
					<td>benchmark</td>
					<td><span id="uaddBench"
					          class="ui-icon ui-icon-help"></span></td>
					<td><span id="uremoveBench"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>

				<tr>
					<td>user</td>
					<td><span id="uaddUser" class="ui-icon ui-icon-help"></span>
					</td>
					<td><span id="uremoveUser"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>

				<tr>
					<td>space</td>
					<td><span id="uaddSpace"
					          class="ui-icon ui-icon-help"></span></td>
					<td><span id="uremoveSpace"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>
				</tbody>
			</table>

			<hr>

			<table>
				<tr>
					<td><h2>leader</h2></td>
					<td><span id="uleaderStatus"
					          class="ui-icon ui-icon-help"></span></td>
				</tr>
			</table>
		</fieldset>

		<fieldset id="permCheckboxes">
			<legend>change permissions</legend>
			<table id="permissionChanges">
				<thead>
				<tr>
					<th>property</th>
					<th>add</th>
					<th>remove</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>job</td>
					<td><input type="checkbox" id="addJob"></input></td>
					<td><input type="checkbox" id="removeJob"></input></td>
				</tr>

				<tr>
					<td>solver</td>
					<td><input type="checkbox" id="addSolver"></input></td>
					<td><input type="checkbox" id="removeSolver"></input></td>
				</tr>

				<tr>
					<td>benchmark</td>
					<td><input type="checkbox" id="addBench"></input></td>
					<td><input type="checkbox" id="removeBench"></input></td>
				</tr>

				<tr>
					<td>user</td>
					<td><input type="checkbox" id="addUser"></input></td>
					<td><input type="checkbox" id="removeUser"></input></td>
				</tr>

				<tr>
					<td>space</td>
					<td><input type="checkbox" id="addSpace"></input></td>
					<td><input type="checkbox" id="removeSpace"></input></td>
				</tr>
				</tbody>
			</table>

			<hr>

			<table>
				<tr id="leaderStatusRow">
					<td><h2>leader</h2></td>
					<td><input type="button" id="leaderStatus"
					           value="promote"></input></td>
				</tr>

				<tr id="communityLeaderStatusRow">
					<td><h2>leader</h2></td>
					<td><span id="communityLeaderStatus"
					          class="ui-icon ui-icon-check"></span></td>
				</tr>
			</table>

			<hr>

			<table id="permChangesButtons">
				<tr>
					<td><input class="btnUp" type="button" id="savePermChanges"
					           value="save"></input></td>
					<td><input class="resetButton" type="button"
					           id="resetPermChanges" value="reset"></input></td>
				</tr>
			</table>

			<div id="dialog-confirm-update" title="confirm update"
			     class="hiddenDialog">
				<p><span class="ui-icon ui-icon-alert"></span><span
						id="dialog-confirm-update-txt"></span></p>
			</div>
		</fieldset>

		<h3 class="addUsersTitle">Add users to <span class="spaceName"></span></h3>
		<fieldset id="addUsersField">
			<legend id="addUsersLegend" class="userExpd"><span class="list-count"></span> users
			</legend>
			<table id="addUsers">
				<thead>
				<tr>
					<th>name</th>
					<th>institution</th>
					<th>email</th>
				</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
			<hr>
			<table id="addUsersButtons">
				<tr>
					<td><input class="btnUp" type="button" id="addUsersButton"
					           value="add"></input></td>
				</tr>
			</table>
		</fieldset>

		<fieldset id="permissionActions">
			<a id="exploreSpaces"
			   href="${starexecRoot}/secure/explore/spaces.jsp">return to space
				explorer</a>
			<a class="btnRun" id="makePublic">make public</a>
		</fieldset>

	</div>

	<div id="dialog-confirm-change" title="confirm change" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-info"></span><span
				id="dialog-confirm-change-txt"></span></p>
	</div>

</star:template>
