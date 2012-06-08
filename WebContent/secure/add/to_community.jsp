<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.SessionUtil" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
try {
	int comId = Integer.parseInt((String)request.getParameter("cid"));
	Space com = Communities.getDetails(comId);
	
	/*
	*  If the user is attempting to join a community they are apart of, redirect them
	*  back to the community explorer
	*/
	if(Users.isMemberOfSpace(SessionUtil.getUserId(request), comId)){
		response.sendRedirect("/starexec/secure/explore/communities.jsp?result=alreadyMember");
	}
	
	if(com == null) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	} else {
		request.setAttribute("com", com);	
	}	
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="join ${com.name}" css="add/to_community" js="lib/jquery.validate.min, lib/jquery.qtip.min, add/to_community">		
	<form method="POST" action="to_community/request" id="inviteForm">
	<fieldset>
		<legend>community information</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label">community </td>
						<td>
							<p>${com.name}</p>
						</td>
					</tr>
					<tr>
						<td class="label">reason for joining </td>
						<td><textarea name="msg" id="reason"></textarea></td>
					</tr>		
					<tr>
						<td class="label">notice </td>
						<td><p>all community leaders of ${com.name} will be e-mailed your request to join their community</p></td>
					</tr>		
					<tr>					
						<td colspan="3">
							<input type="hidden" name="cm" value="${com.id}"/>
							<button type="submit" id="btnSubmit" value="Submit">send request</button>
						</td>
					</tr>
				</tbody>
			</table>
	</fieldset>	
	</form>
	<c:if test="${not empty param.result and param.result == 'requestSent'}">			
		<div class='success message'>request sent successfully - you will receive an email when a leader of that community approves/declines your request</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'requestNotSent'}">			
		<div class='error message'>you are already a member of that community, or have already requested to be and are awaiting approval</div>
	</c:if>
</star:template>