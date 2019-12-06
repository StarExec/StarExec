<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB, org.starexec.data.database.Communities"
        session="false" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	request.setAttribute("coms", Communities.getAll());
	request.setAttribute("firstNameLen", DB.USER_FIRST_LEN);
	request.setAttribute("lastNameLen", DB.USER_LAST_LEN);
	request.setAttribute("institutionLen", DB.INSTITUTION_LEN);
	request.setAttribute("emailLen", DB.EMAIL_LEN);
	request.setAttribute("passwordLen", DB.PASSWORD_LEN);
	request.setAttribute("msgLen", DB.MSG_LEN);
%>

<star:template title="User registration"
               css="common/pass_strength_meter, accounts/registration"
               js="lib/jquery.validate.min, lib/jquery.validate.password, accounts/registration">
	<p class="registration">Create a new user account</p>
	<div id="javascriptDisabled">Javascript is required for most features in
		StarExec, please enable it and reload this page
	</div>
	<form method="POST" action="${starexecRoot}/public/registration/manager"
	      id="regForm" class="registration">
		<fieldset>
			<legend class="registration">User information</legend>
			<table class="shaded">
				<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td class="label">First name</td>
					<td><input id="firstname" type="text" name="fn"
					           maxlength="${firstNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">Last name</td>
					<td><input id="lastname" type="text" name="ln"
					           maxlength="${lastNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">Email</td>
					<td><input id="email" type="text" name="em"
					           maxlength="${emailLen}"/></td>
				</tr>
				<tr>
					<td class="label">Institution</td>
					<td><input id="institution" type="text" name="inst"
					           maxlength="${institutionLen}"/></td>
				</tr>
				<tr>
					<td class="label">Password</td>
					<td>
						<input id="password" type="password" name="pwd"
						       length="${passwordLen}"/>
						<div class="password-meter" id="pwd-meter">
							<div class="password-meter-message"></div>
							<div class="password-meter-bg">
								<div class="password-meter-bar"></div>
							</div>
						</div>
					</td>
				</tr>
				<tr>
					<td class="label">Confirm password</td>
					<td><input id="confirm_password" type="password"
					           name="confirm_password"/></td>
				</tr>
				</tbody>
			</table>
		</fieldset>
		<fieldset>
			<legend>Community information</legend>
			<table class="shaded">
				<thead>
				<tr>
					<th>attribute</th>
					<th id="value_header">value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td class="label">Community</td>
					<td>
						<select id="community" name="cm" class="styled">
							<option></option>
							<c:forEach var="com" items="${coms}">
								<option value="${com.id}">${com.name}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td class="label">Reason for joining</td>
					<td><textarea name="msg" id="reason"
					              length="${msgLen}"></textarea></td>
				</tr>
				<tr>
					<td class="label">New user?</td>
					<td><input type="checkbox" name="uc" value="uc"></td>
				</tr>
				<tr>
					<td class="label">
						Confirm that you have read and agree with our 
						<a href="/starexec/public/TermsOfService2019.pdf" target="_blank" style="text-decoration:underline">Terms of Service</a>
						and
						<a href="https://welcome.miami.edu/privacy-and-legal/index.html" target="_blank" style="text-decoration:underline">Legal Notice</a>.
					</td>
					<td>
						<input type="checkbox" 
							name="termsOfService" 
							value="termsOfService" 
							oninput="$('#submit').button({disabled: !this.checked});">
					</td>
				</tr>
				<tr>
					<td colspan="3">
						<button type="submit" id="submit" value="Submit" disabled>
							Register
						</button>
					</td>
				</tr>
				</tbody>
			</table>
		</fieldset>
	</form>
	<c:if test="${not empty param.result and param.result == 'regSuccess'}">
		<div class='success message'>Registration successful - an email was sent
			to you to activate your account
		</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'regFail'}">
		<div class='error message'>Registration unsuccessful - a user already
			exists under this email address
		</div>
	</c:if>
</star:template>
