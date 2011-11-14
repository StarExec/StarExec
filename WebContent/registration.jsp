<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<star:template title="user registration" css="registration" js="lib/jquery.validate.min, lib/jquery.validate.password, registration">	
	<p>create a new user account</p>
	<form method="POST" action="Registration" id="regForm">
	<fieldset>			
		<legend>user information</legend>
		<table>								
			<tr class="shade">
				<td class="label">first name: </td>
				<td><input id="firstname" type="text" name="firstname" maxlength="32"/></td>
			</tr>
			<tr>
				<td class="label">last name: </td>
				<td><input id="lastname" type="text" name="lastname" maxlength="32"/></td>
			</tr>
			<tr class="shade">
				<td class="label">email: </td>
				<td><input id="email" type="text" name="email" maxlength="64"/></td>
			</tr>
			<tr>
				<td class="label">institute: </td>
				<td><input id="institute" type="text" name="institute" maxlength="64"/></td>
			</tr>
			<tr class="shade">
				<td class="label">password: </td>
				<td>
					<input id="password" type="password" name="password" maxlength="16"/>
					<label for="password" class="error" style="display: none; ">&nbsp;</label>
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
			<tr>
				<td colspan="3"><button type="submit" value="Submit" class="round">create user</button></td>
			</tr>
		</table>		
	</fieldset>		
	</form>
	<%-- error message types in master.css --%>
	<c:if test="${not empty param.result and param.result == 'ok'}">			
		<div class='success message'>registration successful</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'fail'}">			
		<div class='error message'>registration unsuccessful</div>
	</c:if>		
</star:template>