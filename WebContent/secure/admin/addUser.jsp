<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.constants.*, java.util.UUID"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	String password = "Password0410!!";

	request.setAttribute("coms", Communities.getAll());
	request.setAttribute("firstNameLen", R.USER_FIRST_LEN);
	request.setAttribute("lastNameLen", R.USER_LAST_LEN);
	request.setAttribute("institutionLen", R.INSTITUTION_LEN);
	request.setAttribute("emailLen",R.EMAIL_LEN);
	request.setAttribute("passwordLen",R.PASSWORD_LEN);
	request.setAttribute("password", password);
	request.setAttribute("msgLen", R.MSG_LEN);
%>

<star:template title="user registration" css="common/table, explore/common, admin/admin, jqueryui/jquery-ui-1.8.16.starexec" js="lib/jquery.validate.min, lib/jquery-ui-1.8.16.custom.min.js, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min, add/user">	
	<p class="registration">create a new user account</p>
	<form method="POST" action="${starexecRoot}/public/registration/manager" id="regForm" class="add">
	<fieldset>			
		<legend>user information</legend>
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
					<td><input id="firstname" type="text" name="fn" maxlength="${firstNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">last name</td>
					<td><input id="lastname" type="text" name="ln" maxlength="${lastNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">email</td>
					<td><input id="email" type="text" name="em" maxlength="${emailLen}"/></td>
				</tr>
				<tr>
					<td class="label">institution</td>
					<td><input id="institution" type="text" name="inst" maxlength="${institutionLen}"/></td>
				</tr>
				<tr>
					<td class="label">password</td>
					<td>
						<input id="password" type="password" name="pwd" length="${passwordLen}"/>
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
						<td colspan="3"><button type="submit" id="submit" value="Submit">register</button></td>
					</tr>
				</tbody>
			</table>
	</fieldset>	
	</form>
	<c:if test="${not empty param.result and param.result == 'regSuccess'}">			
		<div class='success message'>User was created successfully</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'regFail'}">			
		<div class='error message'>User creation was unsuccessful -- please try again</div>
	</c:if>	
</star:template>