<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*, org.starexec.constants.*, org.starexec.data.security.GeneralSecurity"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	boolean hideCommunityRequests = false;
	try {
		int userId=SessionUtil.getUserId(request);
		request.setAttribute("userId", userId);
		request.setAttribute("hasAdminReadPrivileges", GeneralSecurity.hasAdminReadPrivileges(userId));
	} catch (Exception e) {
		// If we can't get the userId, assume the isn't leader.
		hideCommunityRequests = true;
	}
	request.setAttribute("hideCommunityRequests", hideCommunityRequests);
	request.setAttribute("leaderResponseParameterName", Mail.LEADER_RESPONSE);
	request.setAttribute("emailCodeParameterName", Mail.EMAIL_CODE);
	request.setAttribute("approveCommunityRequestName", Web.APPROVE_COMMUNITY_REQUEST);
	request.setAttribute("declineCommunityRequestName", Web.DECLINE_COMMUNITY_REQUEST);
	request.setAttribute("sentFromCommunityPage", Web.SENT_FROM_COMMUNITY_PAGE);
%>

<star:template title="Communities" js="shared/sharedFunctions, common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.jstree, explore/communities" css="common/delaySpinner, common/table, explore/common, explore/communities">
	<span id="leaderResponse" value="${leaderResponseParameterName}" hidden></span>
	<span id="hasAdminReadPrivileges" value="${hasAdminReadPrivileges}" hidden></span>
	<span id="emailCode" value="${emailCodeParameterName}" hidden></span>
	<span id="approveRequest" value="${approveCommunityRequestName}" hidden></span>
	<span id="declineRequest" value="${declineCommunityRequestName}" hidden></span>
	<span id="communityPage" value="${sentFromCommunityPage}" hidden></span>
	<span id="userId" value="${userId}" hidden></span>
	<div id="explorer">
		<h3>Official</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">
		<h3 id="commName"></h3>
		<p id="commDesc" class="accent"></p>
						
		<!--<fieldset id="websiteField">
			<legend><span>0</span> website(s)</legend>						
			<div id="webDiv">				
				<ul id="websites" class="horizontal"></ul>
			</div>		
		</fieldset>-->
					
		<fieldset id="leaderField">
			<legend class="expd"><span>0</span> leaders</legend>
			<table id="leaders">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
											
		<fieldset id="memberField">
			<legend class="expd"><span>0</span> members</legend>
			<table id="members">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
					</tr>
				</thead>			
			</table>
		</fieldset>										 	

		<c:if test="${!hideCommunityRequests}">
			<fieldset  id="communityField"> <legend class="expd" id="communityExpd"><span>0</span> pending community requests</legend>
				<table id="commRequests">
					<thead>
						<tr>
							<th>user</th>
							<th>community</th>
							<th>message</th>
							<th>approve</th>
							<th>decline</th>
						</tr>
					</thead>			
				</table>
			</fieldset>
		</c:if>

		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><a id="joinComm" href="#">join</a></li>
				<li><a id="leaveComm">leave</a></li>
				<li><a id="editComm" href="#">edit</a></li>			
				<li><a id="downloadPreProcessors">download preprocessors</a></li>				
				<li><a id="downloadPostProcessors">download postprocessors</a></li>
				<li><a id="downloadBenchProcessors">download benchmark processors</a></li>
				<li><a id="downloadUpdateProcessors">download update processors</a></li>
				
			</ul>
		</fieldset>				
	</div>	
	
	<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-confirm-leave" title="leave community" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-leave-txt"></span></p>
	</div>
	<c:if test="${not empty param.result and param.result == 'alreadyMember'}">			
		<div class='error message'>you are already a member of that community</div>
	</c:if>
</star:template>
