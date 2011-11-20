<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.Database" session="false"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("coms", Database.getRootSpaces());
%>

<star:template title="user registration" css="registration" js="lib/jquery.validate.min, lib/jquery.validate.password, registration">	
	<p>create a new user account</p>
	<form method="POST" action="Registration" id="regForm">	

	<fieldset>			
		<legend>user information</legend>
		<table>								
			<tr>
				<td class="label">first name: </td>
				<td><input id="firstname" type="text" name="fn" maxlength="32"/></td>
			</tr>
			<tr>
				<td class="label">last name: </td>
				<td><input id="lastname" type="text" name="ln" maxlength="32"/></td>
			</tr>
			<tr>
				<td class="label">email: </td>
				<td><input id="email" type="text" name="em" maxlength="64"/></td>
			</tr>
			<tr>
				<td class="label">institution: </td>
				<td><input id="institution" type="text" name="inst" maxlength="64"/></td>
			</tr>
			<tr>
				<td class="label">password: </td>
				<td>
					<input id="password" type="password" name="pwd" maxlength="20"/>
					<div class="password-meter">
						<div class="password-meter-message"> </div>
						<div class="password-meter-bg">
							<div class="password-meter-bar"></div>
						</div>
					</div>
				</td>				
			</tr>
			<tr>
				<td class="label">confirm password: </td>
				<td><input id="confirm_password" type="password" name="confirm_password" maxlength="16"/></td>
			</tr>

		</table>		
	</fieldset>	
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
					<td><textarea name="msg" id="reason" maxlength="300">describe your motivation for joining this community</textarea></td>
				</tr>		
				<tr>
					<td colspan="3"><button type="submit" id="submit" value="Submit" class="round">create user</button></td>
				</tr>
			</table>
	</fieldset>	
	</form>
	<c:if test="${not empty param.result and param.result == 'regSuccess'}">			
		<div class='success message'>registration successful - an email was sent to you to activate your account</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'regFail'}">			
		<div class='error message'>registration unsuccessful - a user already exists under this email address</div>
	</c:if>	
</star:template>