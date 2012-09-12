<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int id = Integer.parseInt(request.getParameter("id"));			
		User user = Users.get(id);
		String userFullName = user.getFullName();
		List<Job> jList = Jobs.getByUserId(user.getId());
		
		if(user != null) {
			request.setAttribute("usr", user);
			request.setAttribute("sites", Websites.getAll(id, Websites.WebsiteType.USER));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "User does not exist");
		}
		
		if(jList != null) {			
			request.setAttribute("jobList", jList);
			request.setAttribute("userFullName", userFullName);
		} else {;
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Job does not exist or is restricted");
		}
		
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="${usr.fullName}" js="details/user, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared">
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>				
	<fieldset>
		<legend>details<c:if test="${usr.id == user.id}"> (<a href="/starexec/secure/edit/account.jsp">edit</a>)</c:if></legend>
		<table id="infoTablep">
		<tr>
			<td id="picSection">
				<img id="showPicture" src="/starexec/secure/get/pictures?Id=${user.id}&type=uthn" enlarge="/starexec/secure/get/pictures?Id=${user.id}&type=uorg"><br>
			</td>
			<td id="userDetail">
			<table id="personal" class="shaded">
				<tr>
					<td>e-mail address</td>			
					<td><a href="mailto:${usr.email}">${usr.email}<img class="extLink" src="/starexec/images/external.png"/></a></td>
				</tr>				
				<tr>
					<td>institution</td>			
					<td>${usr.institution}</td>
				</tr>
				<tr>
					<td>member since</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy" value="${usr.createDate}" /></td>
				</tr>
				<tr>
					<td>member type</td>			
					<td>${usr.role}</td>
				</tr>
				<c:if test="${not empty sites}">			
				<tr>
					<td>websites</td>	
					<td>		
						<ul>
							<c:forEach var="site" items="${sites}">
								<li>${site}<img class="extLink" src="/starexec/images/external.png"/></li>
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
		<legend>solvers</legend>
		<p>coming soon...</p>
	</fieldset>
	<fieldset>
		<legend>benchmarks</legend>
		<p>coming soon...</p>
	</fieldset>
	<fieldset id="jobField">
		<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
		<table id="usrJobsTable" uid=${usr.id}>
			<thead>
				<tr>
					<th>name</th>
					<th>status</th>
					<th>complete</th>
					<th>pending</th>
					<th>error</th>
				</tr>
			</thead>			
		</table>
	</fieldset>	
</star:template>