<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
try {
	long id = Long.parseLong((String)request.getParameter("cid"));
	Space com = Communities.getDetails(id);
	Permission perm = SessionUtil.getPermission(request, id);
	
	if(com == null) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	} else if (perm == null || !perm.isLeader()) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit their communities");		
	} else {
		request.setAttribute("com", com);	
		request.setAttribute("types", BenchTypes.getByCommunity(id));
	}
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${com.name}" css="edit/community" js="edit/community, lib/jquery.validate.min">
	<p>manage the ${com.name} community</p>
	<input type="hidden" value="${com.id}" id="comId"/>
	<fieldset>
		<legend>community details</legend>
		<table id="details" class="shaded">
			<tr>
				<th class="label">attribute</th>
				<th>current value</th>
			</tr>
			<tr>
				<td>community name </td>
				<td id="editname">${com.name}</td>
			</tr>
			<tr>
				<td>description</td>
				<td id="editdesc">${com.description}</td>
			</tr>			
		</table>
		<span class="caption">(click the current value of an attribute to edit it)</span>
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<ul id="websites"></ul>
		<span id="toggleWebsite" class="caption">+ add new</span>
		<div id="new_website">
			<label for="website_name">name </label><input type="text" id="website_name" /> 
			<label for="website_url">url </label><input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
	<fieldset>
		<legend>benchmark types</legend>
		<form id="updateForm" enctype="multipart/form-data" method="post" action="/starexec/secure/benchtype/manager?action=update">			
			<input type="hidden" name="com" value="${com.id}"/>
			<table id="benchTypes">
				<tr>
					<th>name</th>				
					<th>description</th>
					<th>processor</th>
				</tr>
				<c:forEach var="type" items="${types}">
					<tr id="type_${type.id}">
						<td>${type.name}</td>
						<td>${type.description}</td>
						<td>${type.processorName}</td>
					</tr>
				</c:forEach>									
			</table>
		</form>
		<span id="toggleType" class="caption">+ add new</span>
		<form id="typeForm" enctype="multipart/form-data" method="POST" action="/starexec/secure/benchtype/manager?action=add">			
			<input type="hidden" name="com" value="${com.id}"/>
			<table id="newType">			
				<tr>
					<td><label for="typeName">name</label></td>
					<td><input name="typeName" type="text" id="typeName"/></td>
				</tr>
				<tr>
					<td><label for="typeDesc">description</label></td>
					<td><textarea name="typeDesc" id="typeDesc"></textarea></td>
				</tr>
				<tr>
					<td><label for="typeFile">processor</label></td>
					<td><input name="typeFile" type="file" id="typeFile"/></td>
				</tr>
				<tr>
					<td colspan="2"><button id="addType" type="submit">add</button></td>				
				</tr>			 			 			
			</table>
		</form>
	</fieldset>
</star:template>