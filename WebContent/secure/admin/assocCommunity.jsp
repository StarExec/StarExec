<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

try {
	int userId = SessionUtil.getUserId(request);
	int id = Integer.parseInt(request.getParameter("id"));	
	Queue q = Queues.get(id);
		
	List<Space> communities = Spaces.getNonAttachedCommunities(id);
	request.setAttribute("queueNameLen", R.QUEUE_NAME_LEN);
	request.setAttribute("queueName", q.getName());
	request.setAttribute("communities", communities);
			
} catch (NumberFormatException nfe) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given user id was in an invalid format");
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
}

%>

<star:template title="give communities acces to permanent queue" js="admin/assocCommunity, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
	<form id="addForm" method="POST" action="/${starexecRoot}/secure/assoc/communities" class="queue">
		<fieldset id="fieldStep1">
			<legend>Give Community Leaders Access to Queue</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr id="queueName" class="noHover">
						<td class="label"><p>queue name</p></td>
						<td>
							<input type="hidden" name="name" value="${queueName}"/>
							<p>${queueName}</p></td>
					</tr>						
				</tbody>
			</table>
	</fieldset>
	<fieldset id="fieldSelectLeaderSpace"> 
			<legend>community selection</legend>
			<table id="tblCommunities" class="contentTbl">
				<thead>
					<tr>
						<th>communitiy</th>
					</tr>
				</thead>	
				<tbody>
				<c:forEach var="c" items="${communities}">
					<tr id="community_${u.id}">
						<td>
							<input type="hidden" name="community" value="${c.id}"/>
							<p>${c.name}</p>							
							
						</td>																		
					</tr>
				</c:forEach>
				</tbody>					
			</table>		
		</fieldset>
	<div id="actionBar">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><button type="submit" id="btnDone">Give Access</button></li>
			</ul>
		</fieldset>		
	</div>
		
</form>
</star:template>