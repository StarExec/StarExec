<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- could do this with JSON since we're in /pages/ --%>
<%
	request.setAttribute("coms", Database.getRootSpaces());
%>

<star:template title="join a community" css="registration" js="lib/jquery.validate.min, make_invite">	
	<p>submit a request to join another community</p>
	<form method="POST" action="Invitation" id="inviteForm">	
	<fieldset>
		<legend>community information</legend>
			<table>
				<tr>
					<td class="label">community: </td>
					<td>
						<select id="community" name="cm" class="styled">
							<option> </option>
							<c:forEach var="com" items="${coms}">
	                                <option value="${com.id}">${com.name}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td class="label">reason for joining: </td>
					<td><textarea name="msg" id="reason" maxlength="300">Describe your motivation for joining this community and/or what you plan on using it for...</textarea></td>
				</tr>		
				<tr>
					<td colspan="3"><button type="submit" id="submit" value="Submit" class="round">send request</button></td>
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