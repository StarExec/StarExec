<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*" session="false"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("coms", Communities.getAll());
%>

<star:template title="user registration" css="common/pass_strength_meter, accounts/registration" js="lib/jquery.validate.min, lib/jquery.validate.password, accounts/registration">	
	<p class="registration">create a new user account</p>
	<div id="javascriptDisabled">javascript is required for most features in StarExec, please enable it and reload this page</div>
	<form method="POST" action="/starexec/public/registration/manager" id="regForm" class="registration">	
	<fieldset>			
		<legend class="registration">user information</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
			</thead>		
			<tbody>						
				<tr>
					<td class="label">first name</td>
					<td><input id="firstname" type="text" name="fn" maxlength="32"/></td>
				</tr>
				<tr>
					<td class="label">last name</td>
					<td><input id="lastname" type="text" name="ln" maxlength="32"/></td>
				</tr>
				<tr>
					<td class="label">email</td>
					<td><input id="email" type="text" name="em" maxlength="64"/></td>
				</tr>
				<tr>
					<td class="label">institution</td>
					<td><input id="institution" type="text" name="inst" maxlength="64"/></td>
				</tr>
				<tr>
					<td class="label">password</td>
					<td>
						<input id="password" type="password" name="pwd"/>
						<div class="password-meter" id="pwd-meter">
							<div class="password-meter-message"> </div>
							<div class="password-meter-bg">
								<div class="password-meter-bar"></div>
							</div>
						</div>
					</td>				
				</tr>
				<tr>
					<td class="label">confirm password</td>
					<td><input id="confirm_password" type="password" name="confirm_password"/></td>
				</tr>
				<tr>
					<td class="label">preferred archive type</td>
					<td>
						<select id="archiveType" name="pat">
							<option> </option>
							<option value=".tar">.tar</option>
							<option value=".tar.gz">.tar.gz</option>
							<option value=".tgz">.tgz</option>
							<option value=".zip">.zip</option>
						</select>
					</td>
			</tr>
			</tbody>
		</table>		
	</fieldset>	
	<fieldset>
		<legend>community information</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th id="value_header">value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label">community</td>
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
						<td class="label">reason for joining</td>
						<td><textarea name="msg" id="reason"></textarea></td>
					</tr>		
					<tr>
						<td colspan="3"><button type="submit" id="submit" value="Submit">register</button></td>
					</tr>
				</tbody>
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