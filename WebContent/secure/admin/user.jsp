<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	request.setAttribute("columnWidth", "100px");
%>
<star:template title="Users Admin"
               js="admin/user, lib/jquery-ui.min, lib/jquery.dataTables.min"
               css="admin/user, common/table, explore/common, admin/admin, jqueryui/jquery-ui">
	<fieldset id="userField" class="expdContainer">
		<legend class="expd" id="userExpd"><span>0</span> users</legend>
		<ul id="actionList">
			<li><a type="btnRun" id="addUser"
			       href="${starexecRoot}/secure/admin/addUser.jsp">Create New
				User</a></li>
		</ul>
		<table id="users">
			<thead>
			<tr>
				<th style="width:${columnWidth};">name</th>
				<th style="width:${columnWidth};">institution</th>
				<th style="width:200px;">email</th>
				<th style="width:${columnWidth};">permissions</th>
				<th style="width:${columnWidth};">suspend</th>
				<th style="width:${columnWidth};">reports</th>
				<th style="width:${columnWidth};">developer</th>
			</tr>
			</thead>
		</table>
	</fieldset>
</star:template>
