<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.Web, org.starexec.util.Mail" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	// These will be used by the JavaScript.
	request.setAttribute("leaderResponseParameterName", Mail.LEADER_RESPONSE);
	request.setAttribute("emailCodeParameterName", Mail.EMAIL_CODE);
	request.setAttribute(
			"approveCommunityRequestName", Web.APPROVE_COMMUNITY_REQUEST);
	request.setAttribute(
			"declineCommunityRequestName", Web.DECLINE_COMMUNITY_REQUEST);
	request.setAttribute("sentFromCommunityPage", Web.SENT_FROM_COMMUNITY_PAGE);
%>
<star:template title="Communities Admin"
               js="admin/community, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, shared/sharedFunctions"
               css="common/table, details/shared, explore/common, admin/admin">
	<span id="leaderResponse" value="${leaderResponseParameterName}"
	      hidden></span>
	<span id="emailCode" value="${emailCodeParameterName}" hidden></span>
	<span id="approveRequest" value="${approveCommunityRequestName}"
	      hidden></span>
	<span id="declineRequest" value="${declineCommunityRequestName}"
	      hidden></span>
	<span id="communityPage" value="${sentFromCommunityPage}" hidden></span>

	<div id="explorer">
		<h3>official</h3>
		<ul id="exploreList"></ul>
		<div id="explorerAction">
			<ul id="exploreActions">
				<li><a type="btnRun" id="newCommunity"
				       href="${starexecRoot}/secure/add/space.jsp">Add New
					Community</a></li>
			</ul>
		</div>
	</div>

	<div id="detailPanel">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><a type="btnRun" id="removeCommLeader"
				       href="${starexecRoot}/secure/edit/community.jsp">remove
					community leader</a></li>
				<li><a type="btnRun" id="promoteCommLeader"
				       href="${starexecRoot}/secure/edit/community.jsp">promote
					member to leader</a>
			</ul>
		</fieldset>
		<fieldset id="communityField" class="expdContainer">
			<legend class="expd" id="communityExpd"><span class="list-count"></span> pending
				community requests
			</legend>
			<table id="commRequests">
				<thead>
				<tr>
					<th>user</th>
					<th>community</th>
					<th>message</th>
					<th>date created</th>
					<th>approve</th>
					<th>decline</th>
				</tr>
				</thead>
			</table>
		</fieldset>
	</div>
</star:template>
