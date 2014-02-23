<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int id = Integer.parseInt(request.getParameter("id"));	
		User t_user = Users.get(id);
		int visiting_userId = SessionUtil.getUserId(request);
		User visiting_user = Users.get(visiting_userId);
		
		

		if(t_user != null) {
			request.setAttribute("t_user", t_user);
			boolean owner = true;
			String userFullName = t_user.getFullName();
			request.setAttribute("sites", Websites.getAll(id, Websites.WebsiteType.USER));
			// Ensure the user visiting this page is the owner of the solver
			if( (visiting_userId != id) && (!visiting_user.getRole().equals("admin"))  ){
				owner = false;
			} else {
				List<Job> jList = Jobs.getByUserId(t_user.getId());
				long disk_usage = Users.getDiskUsage(t_user.getId());
				request.setAttribute("diskQuota", Util.byteCountToDisplaySize(t_user.getDiskQuota()));
				request.setAttribute("diskUsage", Util.byteCountToDisplaySize(disk_usage));
				
				if(jList != null) {			
					request.setAttribute("jobList", jList);
					request.setAttribute("userFullName", userFullName);
				} else {;
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
				}
			}
			request.setAttribute("owner", owner);			
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "User does not exist");
		}
		
		
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${t_user.fullName}" js="details/user, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, explore/spaces, details/shared">
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>				
	<fieldset>
		<legend>details</legend>
		<table id="infoTable">
		<tr>
			<td id="picSection">
				<img id="showPicture" src="/${starexecRoot}/secure/get/pictures?Id=${t_user.id}&type=uthn" enlarge="/${starexecRoot}/secure/get/pictures?Id=${t_user.id}&type=uorg"><br>
			</td>
			<td id="userDetail" class="detail">
			<table id="personal" class="shaded">
				<tr>
					<td>e-mail address</td>			
					<td><a href="mailto:${t_user.email}">${t_user.email}<img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
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
								<li>${site}<img class="extLink" src="/${starexecRoot}/images/external.png"/></li>
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
			<table id="solvers" uid=${t_user.id}>
				<thead>
					<tr>
						<th> name </th>
						<th> description </th>
					</tr>
				</thead>
			</table>
		</fieldset>
		<fieldset id="benchField">
			<legend class="expd" id="benchExpd"><span>0</span> benchmarks</legend>
			<table id="benchmarks" uid=${t_user.id}>
				<thead>
					<tr>
						<th> name</th>
						<th> type</th>											
					</tr>
				</thead>		
			</table>
		</fieldset>			
		<fieldset id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<table id="jobs" uid=${t_user.id}>
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th>complete</th>
						<th>pending</th>
						<th>error</th>
						<th>time</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
		
			
		<fieldset id="actionField">
		<legend>actions</legend>
			<a id="editButton" href="/${starexecRoot}/secure/edit/account.jsp?id=${t_user.id}">edit</a>
			<a id="recycleBinButton" href="/${starexecRoot}/secure/details/recycleBin.jsp">manage recycle bin</a>
		</fieldset>
	</c:if>
</star:template>