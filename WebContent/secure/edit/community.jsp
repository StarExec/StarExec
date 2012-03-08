<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
	int id = Integer.parseInt((String)request.getParameter("cid"));
	Space com = Communities.getDetails(id);
	Permission perm = SessionUtil.getPermission(request, id);
	
	if(com == null) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	} else if (perm == null || !perm.isLeader()) {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit their communities");		
	} else {
		request.setAttribute("com", com);	
		request.setAttribute("bench_proc", Processors.getByCommunity(id, ProcessorType.BENCH));
		request.setAttribute("pre_proc", Processors.getByCommunity(id, ProcessorType.PRE));
		request.setAttribute("post_proc", Processors.getByCommunity(id, ProcessorType.POST));
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
		<table id="detailsTbl" class="shaded">
			<thead>
				<tr>
					<th class="label">attribute</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>community name </td>
					<td id="editname">${com.name}</td>
				</tr>
				<tr>
					<td>description</td>
					<td id="editdesc">${com.description}</td>
				</tr>		
			</tbody>
		</table>
		<span class="caption">(click the current value of an attribute to edit it)</span>
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<table id="websiteTable" class="shaded"></table>
		<span id="toggleWebsite" class="caption">+ add new</span>
		<div id="newWebsite">
			<label for="website_name">name </label><input type="text" id="website_name" /> 
			<label for="website_url">url </label><input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
	<fieldset>
		<legend>benchmark types</legend>
		<form id="updateBenchTypeForm" class="updateForm" enctype="multipart/form-data" method="post" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="update"/>
			<input type="hidden" name="type" value="bench"/>
			<table id="benchTypeTbl" class="shaded">
				<thead>
					<tr>
						<th>name</th>				
						<th>description</th>
						<th>file name</th>
					</tr>
				</thead>				
				<tbody>
				<c:forEach var="proc" items="${bench_proc}">
					<tr id="proc_${proc.id}">
						<td>${proc.name}</td>
						<td>${proc.description}</td>
						<td>${proc.fileName}</td>
					</tr>
				</c:forEach>
				</tbody>									
			</table>
		</form>
		<span id="toggleBenchType" class="caption">+ add new</span>
		<form id=newTypeForm class="newForm" enctype="multipart/form-data" method="POST" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="add"/>
			<input type="hidden" name="type" value="bench"/>
			<table id="newTypeTbl">			
				<tr>
					<td><label for="typeName">name</label></td>
					<td><input name="name" type="text" id="typeName"/></td>
				</tr>
				<tr>
					<td><label for="typeDesc">description</label></td>
					<td><textarea name="desc" id="typeDesc"></textarea></td>
				</tr>
				<tr>
					<td><label for="typeFile">processor</label></td>
					<td><input name="file" type="file" id="typeFile"/></td>
				</tr>
				<tr>
					<td colspan="2"><button id="addType" type="submit">add</button></td>				
				</tr>			 			 			
			</table>
		</form>
	</fieldset>
	<!-- <fieldset>
		<legend>pre processors</legend>
		<form id="updatePrePrcssForm" class="updateForm" enctype="multipart/form-data" method="post" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="update"/>
			<input type="hidden" name="type" value="pre"/>
			<table id="preProcessorTbl" class="shaded">
				<thead>
					<tr>
						<th>name</th>				
						<th>description</th>
						<th>file name</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="proc" items="${pre_proc}">
						<tr id="proc_${proc.id}">
							<td>${proc.name}</td>
							<td>${proc.description}</td>
							<td>${proc.fileName}</td>
						</tr>
					</c:forEach>
				</tbody>																				
			</table>
		</form>
		<span id="togglePreProcessor" class="caption">+ add new</span>
		<form id="addPreProcessorForm" class="newForm" enctype="multipart/form-data" method="POST" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="add"/>
			<input type="hidden" name="type" value="pre"/>
			<table id="newPreProcessTbl">			
				<tr>
					<td><label for="processorName">name</label></td>
					<td><input name="name" type="text" id="processorName"/></td>
				</tr>
				<tr>
					<td><label for="processorDesc">description</label></td>
					<td><textarea name="desc" id="processorDesc"></textarea></td>
				</tr>
				<tr>
					<td><label for="processorFile">processor</label></td>
					<td><input name="file" type="file" id="processorFile"/></td>
				</tr>
				<tr>
					<td colspan="2"><button id="addPreProcessor" type="submit">add</button></td>				
				</tr>			 			 			
			</table>
		</form>
	</fieldset>-->
	<fieldset>
		<legend>post processors</legend>
		<form id="updatePstPrcssForm" class="updateForm" enctype="multipart/form-data" method="post" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="update"/>
			<input type="hidden" name="type" value="post"/>
			<table id="postProcessorTbl" class="shaded">
				<thead>
					<tr>
						<th>name</th>				
						<th>description</th>
						<th>file name</th>
					</tr>								
				</thead>
				<tbody>
					<c:forEach var="proc" items="${post_proc}">
						<tr id="proc_${proc.id}">
							<td>${proc.name}</td>
							<td>${proc.description}</td>
							<td>${proc.fileName}</td>
						</tr>
					</c:forEach>					
				</tbody>
			</table>
		</form>
		<span id="togglePostProcessor" class="caption">+ add new</span>
		<form id="addPostProcessorForm" class="newForm" enctype="multipart/form-data" method="POST" action="/starexec/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="add"/>
			<input type="hidden" name="type" value="post"/>
			<table id="newPostProcessTbl">			
				<tr>
					<td><label for="processorName">name</label></td>
					<td><input name="name" type="text" id="processorName"/></td>
				</tr>
				<tr>
					<td><label for="processorDesc">description</label></td>
					<td><textarea name="desc" id="processorDesc"></textarea></td>
				</tr>
				<tr>
					<td><label for="processorFile">processor</label></td>
					<td><input name="file" type="file" id="processorFile"/></td>
				</tr>
				<tr>
					<td colspan="2"><button id="addPostProcessor" type="submit">add</button></td>				
				</tr>			 			 			
			</table>
		</form>
	</fieldset>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
</star:template>