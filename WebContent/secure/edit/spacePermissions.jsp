<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*, org.starexec.data.security.*, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int spaceId=Integer.parseInt(request.getParameter("id"));
		int userId = SessionUtil.getUserId(request);

		request.setAttribute("userId",userId);
		request.setAttribute("isAdmin",Users.isAdmin(userId));

		List<Space> communities = Communities.getAll();
		StringBuilder communityIdList = new StringBuilder();


		if(communities.size() > 0){
		        for(Space c : communities){
			        communityIdList.append(c.getId());
				communityIdList.append(",");
			}
			communityIdList.delete(communityIdList.length() -1,communityIdList.length());
			request.setAttribute("communityIdList",communityIdList.toString());

		}
		else{
			request.setAttribute("communityIdList","1");
			}

		if (SpaceSecurity.canUserSeeSpace(spaceId,userId).isSuccess() && spaceId > 0) {
			List<Integer> idChain=Spaces.getChainToRoot(spaceId);
			StringBuilder stringChain=new StringBuilder();
			for (Integer id : idChain) {
				stringChain.append(id);
				stringChain.append(",");
			}
			stringChain.delete(stringChain.length()-1,stringChain.length());
			request.setAttribute("spaceChain",stringChain.toString());
		} else {
			request.setAttribute("spaceChain","1");

		}

	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>
<star:template title="edit permissions" js="common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, edit/spacePermissions, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, explore/common, explore/spaces">			
	<span id="userId" value="${userId}" ></span>
	<span id="isAdmin" value="${isAdmin}"></span>
	<span id="spaceChain" value="${spaceChain}"></span>
	<span id="communityIdList" value="${communityIdList}"></span>
	<div id="explorer">
		<h3>spaces</h3>
		 
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">				
		<h3 id="spaceName"></h3>
		<p id="spaceLeader" class="accent"></p>
		<p id="spaceDesc" class="accent"></p>
		<p id="spaceID" class="accent"></p>																			
		<fieldset  id="userField">
			<legend id="userExpd"><span>0</span> users</legend>
			<table id="users">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
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
						<td><span id= "uaddJob" class="ui-icon ui-icon-help"></span></td>
						<td><span id= "uremoveJob" class="ui-icon ui-icon-help"></span></td>
					</tr>
					
					<tr>
						<td>solver</td>
						<td><span id= "uaddSolver" class="ui-icon ui-icon-help"></span></td>
						<td><span id= "uremoveSolver" class="ui-icon ui-icon-help"></span></td>
					</tr>

					<tr>
						<td>benchmark</td>
						<td><span id= "uaddBench" class="ui-icon ui-icon-help"></span></td>
						<td><span id= "uremoveBench" class="ui-icon ui-icon-help"></span></td>
					</tr>

					<tr>
						<td>user</td>
						<td><span id= "uaddUser" class="ui-icon ui-icon-help"></span></td>
						<td><span id= "uremoveUser" class="ui-icon ui-icon-help"></span></td>
					</tr>

					<tr>
						<td>space</td>
						<td><span id= "uaddSpace" class="ui-icon ui-icon-help"></span></td>
						<td><span id= "uremoveSpace" class="ui-icon ui-icon-help"></span></td>
					</tr>
				</tbody>			
			</table>
			
			<hr>

			<table>
				<tr>
					<td><h2>leader</h2></td>
					<td><span id= "uleaderStatus" class="ui-icon ui-icon-help"></span></td>
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
					<td><input type="button" id="leaderStatus" value="promote"></input></td>
				</tr>
				
				<tr id="communityLeaderStatusRow">
					<td><h2>leader</h2></td>
					<td><span id= "communityLeaderStatus" class="ui-icon ui-icon-check"></span></td>
				</tr>
			</table>
						

			<hr>

			<table id="permChangesButtons">
				<tr>
					<td><input class="btnUp" type="button" id="savePermChanges" value="save"></input></td>
					<td><input class="resetButton" type="button" id="resetPermChanges" value="reset"></input></td>

				</tr>
			</table>
			<div id="dialog-confirm-update" title="confirm update">
				<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-update-txt"></span></p>
			</div>
		</fieldset>
		
		<fieldset id="permissionActions">
			<a id="exploreSpaces" href="/${starexecRoot}/secure/explore/spaces.jsp">return to space explorer</a>
		</fieldset>

	</div>	
	
	
	
</star:template>


