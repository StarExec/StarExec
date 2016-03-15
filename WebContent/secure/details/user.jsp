<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*,org.starexec.data.to.Website.WebsiteType, org.starexec.data.to.*,org.starexec.data.security.*, org.starexec.util.*, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int id=-1;
		try {
			id = Integer.parseInt(request.getParameter("id"));	
		} catch (Exception e) {
			id = SessionUtil.getUserId(request);
		}
		User t_user = Users.get(id);
		int visiting_userId = SessionUtil.getUserId(request);
		
		

		if(t_user != null) {
			request.setAttribute("t_user", t_user);
			request.setAttribute("userId", id);
			request.setAttribute("communitiesUserIsIn", Communities.getAllCommunitiesUserIsIn(id));
			boolean owner = true;
			String userFullName = t_user.getFullName();
			request.setAttribute("sites", Websites.getAllForHTML(id, WebsiteType.USER));
			// Ensure the user visiting this page is the owner of the solver
			if( (visiting_userId != id) && (!GeneralSecurity.hasAdminReadPrivileges(visiting_userId))  ){
				owner = false;
			} else {
				List<Job> jList = Jobs.getByUserId(t_user.getId());
				long disk_usage = Users.getDiskUsage(t_user.getId());
				request.setAttribute("diskQuota", Util.byteCountToDisplaySize(t_user.getDiskQuota()));
				request.setAttribute("diskUsage", Util.byteCountToDisplaySize(disk_usage));
				
				if(jList != null) {			
					request.setAttribute("jobList", jList);
					request.setAttribute("userFullName", userFullName);
				} else {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
				}
			}
			request.setAttribute("owner", owner);	
			request.setAttribute("sites", Websites.getAllForHTML(id,WebsiteType.USER));

		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "User does not exist");
		}
		
		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="util/draggable, util/spaceTree, common/delaySpinner, details/user, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="explore/common, details/user, common/delaySpinner, common/table, explore/spaces, details/shared">
	<span id="userId" value="${userId}"></span>
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>	
	<div id="explorer">
		<h3>Spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	
	<div id="detailPanel" class="userDetailPanel">
	<fieldset>
		<legend>details</legend>
		<table id="infoTable">
		<tr>
			<td id="picSection">
				<img id="showPicture" src="${starexecRoot}/secure/get/pictures?Id=${t_user.id}&type=uthn" enlarge="${starexecRoot}/secure/get/pictures?Id=${t_user.id}&type=uorg"><br>
			</td>
			<td id="userDetail" class="detail">
			<table id="personal" class="shaded">
				<tr>
					<td>e-mail address</td>			
					<td><a href="mailto:${t_user.email}">${t_user.email}<img class="extLink" src="${starexecRoot}/images/external.png"/></a></td>
				</tr>				
				<tr>
					<td>institution</td>			
					<td>${t_user.institution}</td>
				</tr>
				<tr>
					<td>member since</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy" value="${t_user.createDate}" /></td>
				</tr>
				<tr>
					<td>member type</td>			
					<td>${t_user.role}</td>
				</tr>
				<c:if test="${not empty sites}">			
				<tr>
					<td>websites</td>	
					<td>		
						<ul>
							<c:forEach var="site" items="${sites}">
								<li>${site}<img class="extLink" src="${starexecRoot}/images/external.png"/></li>
							</c:forEach>	
						</ul>
					</td>
				</tr>
				</c:if>			
			</table>
			</td>
			</tr>
		</table>
	</fieldset>	
	<fieldset>
		<legend>communities</legend>
		<table id="member of communities" class="shaded">
			<thead>
				<tr>
					<th>communites</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="community" items="${communitiesUserIsIn}">
				<tr>
					<td class="community">${community.getName()}</td>
				</tr>
				</c:forEach>
			</tbody>			
		</table>
	</fieldset>
	<c:if test="${owner}">
		<fieldset>
			<legend>user disk quota</legend>
			<table id="diskUsageTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>disk quota</td>
						<td>${diskQuota}</td>
					</tr>
					<tr>
						<td>current disk usage</td>
						<td>${diskUsage}</td>
					</tr>
				</tbody>			
			</table>
		</fieldset>
		<fieldset id="solverField">
			<legend class="expd" id="solverExpd"><span>0</span> solvers</legend>
			<table id="solvers" uid="${t_user.id}" class="selectableTable">
				<thead>
					<tr>
						<th>name</th>
						<th>description</th>
						<th>type</th>
					</tr>
				</thead>
			</table>
			<fieldset id="solverActions" class="actionField">
				<legend>actions</legend>
				<button prim="solver" id="recycleSolver" class="recycleButton recycleSelected">recycle selected</button>
				<button title="This will move all of the solvers you own that are not in any spaces to the recycle bin."
				 prim="solver" id="recycleOrphanedSolvers" class="recycleButton recycleOrphaned">recycle orphaned</button>
				
			</fieldset>
		</fieldset>
		<fieldset id="benchField">
			<legend class="expd" id="benchExpd"><span>0</span> benchmarks</legend>
			<table id="benchmarks" uid="${t_user.id}" class="selectableTable">
				<thead>
					<tr>
						<th> name</th>
						<th> type</th>											
					</tr>
				</thead>		
			</table>
			<fieldset id="benchActions" class="actionField">
				<legend>actions</legend>
				<button prim="benchmark" id="recycleBench" class="recycleButton recycleSelected">recycle selected</button>
				<button title="This will move all of the benchmarkss you own that are not in any spaces to the recycle bin" prim="benchmark" id="recycleOrphanedBench" class="recycleButton recycleOrphaned">recycle orphaned</button>
				
			</fieldset>
		</fieldset>			
		<fieldset id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<table id="jobs" uid="${t_user.id}" class="selectableTable">
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th>completed</th>
						<th>total</th>
						<th>failed</th>
						<th>time</th>
					</tr>
				</thead>			
			</table>
			<fieldset id="jobActions" class="actionField">
				<legend>actions</legend>
				<button id="deleteJob" class="deleteButton deleteSelected">delete selected</button>
				<button title="This will permanently delete all of the jobs you created that are no longer in any spaces"
				 id="deleteOrphanedJob" class="deleteButton deleteOrphaned">delete orphaned</button>
				
			</fieldset>
		</fieldset>
		
			
		<fieldset id="actionField">
		<legend>actions</legend>
			<button id="showSpaceExplorer">show space explorer</button>
			<button title="This will add all of your 'orphaned' solvers, benchmarks, and jobs to the space selected in the space explorer
			on the left. An item is 'orphaned' if it is not linked to any spaces" id="linkOrphanedButton">associate orphaned primitives with space</button>
			<a id="editButton" href="${starexecRoot}/secure/edit/account.jsp?id=${t_user.id}">edit</a>
			<a id="recycleBinButton" href="${starexecRoot}/secure/details/recycleBin.jsp">manage recycle bin</a>
		</fieldset>
		
		<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
		</div>
		<div id="dialog-confirm-recycle" title="confirm recycle" class="hiddenDialog">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-recycle-txt"></span></p>
		</div>
			<div id="dialog-confirm-copy" title="confirm copy" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	</c:if>
	</div>
</star:template>
