<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database, org.starexec.data.to.*" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
try {
	long id = Long.parseLong((String)request.getParameter("cid"));
	Space com = Database.getCommunityDetails(id);
	
	if(com == null) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	} else {
		request.setAttribute("com", com);	
	}	
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="join ${com.name}" css="join_community" js="lib/jquery.validate.min, join_community">		
	<form method="POST" action="request" id="inviteForm">
	<fieldset>
		<legend>community information</legend>
			<table>
				<tr>
					<td class="label">community </td>
					<td>
						<p>${com.name}</p>
					</td>
				</tr>
				<tr>
					<td class="label">reason for joining </td>
					<td><textarea name="msg" id="reason" maxlength="300">describe your motivation for joining this community</textarea></td>
				</tr>		
				<tr>
					<td class="label">notice </td>
					<td><p>all community leaders of ${com.name} will be e-mailed your request to join their community</p></td>
				</tr>		
				<tr>					
					<td colspan="3">
						<input type="hidden" name="cm" value="${com.id}"/>
						<button type="submit" id="submit" value="Submit" class="round">send request</button>
					</td>
				</tr>
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