<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="edit account" css="edit/account" js="lib/jquery.validate.min, lib/jquery.validate.password, edit/account">
	<p>review and edit your account details here.</p>
	<fieldset>
		<legend>personal information</legend>
		<table id="personal" class="shaded">
			<tr>
				<th class="label">attribute</th>
				<th>current value</th>
			</tr>
			<tr>
				<td>first name </td>
				<td id="editfirstname">${user.firstName}</td>
			</tr>
			<tr>
				<td>last name</td>
				<td id="editlastname">${user.lastName}</td>
			</tr>
			<tr>
				<td>institution </td>
				<td id="editinstitution">${user.institution}</td>
			</tr>
			<tr>
				<td>email </td>
				<td>${user.email}</td>
			</tr>
		</table>
		<h6>(click the current value of an attribute to edit it; email addresses are currently not editable)</h6>
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<ul id="websites"></ul>
		<span id="toggleWebsite" class="caption">+ add new</span>
		<div id="new_website">
			name: <input type="text" id="website_name" /> 
			url: <input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
	<fieldset>
		<legend>password</legend>
		<form id="changePassForm">
			<table id="passwordTable">
				<tr>
					<td>current password</td>
					<td><input type="password" id="current_pass" name="current_pass"/></td>
				</tr>
				<tr>
					<td>new password</td>
					<td>
						<input type="password" id="password" name="pwd"/>
						<div class="password-meter" id="pwd-meter" style="visibility:visibile">
							<div class="password-meter-message"> </div>
							<div class="password-meter-bg">
								<div class="password-meter-bar"></div>
							</div>
						</div>
					</td>
				</tr>
				<tr>
					<td>re-enter new password</td>
					<td><input type="password" id="confirm_pass" name="confirm_pass"/></td>
				</tr>
				<tr>
					<td colspan="2"><button id="changePass">change</button></td>
				</tr>
			</table>
		</form>
	</fieldset>
</star:template>