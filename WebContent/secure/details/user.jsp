<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int id = Integer.parseInt(request.getParameter("id"));	
		User t_user = Users.get(id);
		int userId = SessionUtil.getUserId(request);
		boolean owner = true;
		String userFullName = t_user.getFullName();
		List<Job> jList = Jobs.getByUserId(t_user.getId());
		
		if(t_user != null) {
			// Ensure the user visiting this page is the owner of the solver
			if(userId != id){
				owner = false;
			}
			request.setAttribute("owner", owner);
			request.setAttribute("t_user", t_user);
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

<star:template title="${t_user.fullName}" js="details/user, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min" css="common/table, details/shared">
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>				
	<fieldset>
		<legend>details<c:if test="${owner}"> (<a href="/starexec/secure/edit/account.jsp">edit</a>)</c:if></legend>
		<table id="infoTable">
		<tr>
			<td id="picSection">
				<img id="showPicture" src="/starexec/secure/get/pictures?Id=${t_user.id}&type=uthn" enlarge="/starexec/secure/get/pictures?Id=${t_user.id}&type=uorg"><br>
			</td>
			<td id="userDetail" class="detail">
			<table id="personal" class="shaded">
				<tr>
					<td>e-mail address</td>			
					<td><a href="mailto:${t_user.email}">${t_user.email}<img class="extLink" src="/starexec/images/external.png"/></a></td>
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
	<c:if test="${owner}">
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
	</c:if>
	<c:if test="${owner}">
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
	</c:if>
	<c:if test="${owner}"> 
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
	</c:if>
</star:template>