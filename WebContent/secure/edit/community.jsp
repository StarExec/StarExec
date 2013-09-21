<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		boolean admin = false;
		User u = Users.get(userId);
		if (u.getRole().equals("admin")) {
			admin = true;
		}
		request.setAttribute("isAdmin", admin);
		request.setAttribute("communityNameLen", R.COMMUNITY_NAME_LEN);
		request.setAttribute("communityDescLen", R.COMMUNITY_DESC_LEN);
		request.setAttribute("processorNameLen", R.PROCESSOR_NAME_LEN);
		request.setAttribute("processorDescLen", R.PROCESSOR_DESC_LEN);
		request.setAttribute("benchNameLen", R.BENCH_NAME_LEN);
		request.setAttribute("benchDescLen", R.BENCH_DESC_LEN);
		int id = Integer.parseInt((String)request.getParameter("cid"));
		request.setAttribute("defaultBenchLink", Util.docRoot("secure/edit/defaultBenchmark.jsp?id="+((Integer)id).toString()));
		Space com = Communities.getDetails(id);
		Permission perm = SessionUtil.getPermission(request, id);
		
		if(com == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else if (perm == null || !perm.isLeader()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit their communities");		
		} else {
			List<String> listOfDefaultSettings = Communities.getDefaultSettings(id);

		request.setAttribute("com", com);	
		request.setAttribute("bench_proc", Processors.getByCommunity(id, ProcessorType.BENCH));
		request.setAttribute("pre_proc", Processors.getByCommunity(id, ProcessorType.PRE));
		request.setAttribute("post_proc", Processors.getByCommunity(id, ProcessorType.POST));
		request.setAttribute("defaultPPName", listOfDefaultSettings.get(1));
		request.setAttribute("defaultCpuTimeout", listOfDefaultSettings.get(2));
		request.setAttribute("defaultClockTimeout", listOfDefaultSettings.get(3));
		request.setAttribute("defaultPPId", listOfDefaultSettings.get(4));
		request.setAttribute("dependenciesEnabled",listOfDefaultSettings.get(5));
		try {
			Benchmark bench=Benchmarks.get(Integer.parseInt(listOfDefaultSettings.get(6)));
			if (bench!=null) {
				request.setAttribute("defaultBenchmark", bench.getName());
			} else {
				request.setAttribute("defaultBenchmark", "none specified");
			}
		} catch (Exception e) {
			request.setAttribute("defaultBenchmark", "none specified");
		}
		
		
		

	}
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${com.name}" js="lib/jquery.dataTables.min, lib/jquery.validate.min, edit/community" css="common/table, edit/community">
	
	<input type="hidden" value="${com.id}" id="comId"/>
	<fieldset>
		<legend>community details</legend>
		<table id="detailsTbl" class="shaded">
			<thead>
				<tr class="headerRow">
					<th class="label">attribute</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
				<tr id="nameRow" length="${communityNameLen}">
					<td>community name </td>
					<td id="editname">${com.name}</td>
				</tr>
				<tr id="descRow" length="${communityDescLen}">
					<td>description</td>
					<td id="editdesc">${com.description}</td>
				</tr>		
			</tbody>
		</table>
		<span class="caption">(click the current value of an attribute to edit it)</span>
	</fieldset>
	

	
	<c:if test="${isAdmin}">
		<fieldset id="leaderField">
			<legend class="expd"><span>0</span> leaders</legend>
			
			<table id="leaders">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
						<th>remove</th>
						<th>demote</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
		<fieldset id="memberField">
			<legend class="expd"><span>0</span> members</legend>
			
			<table id="Members">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
						<th>remove</th>
						<th>promote</th>
					</tr>
				</thead>			
			</table>

		</fieldset>
	</c:if>
		
	<fieldset id= "websiteField">
		<legend>associated websites</legend>
		<table id="websiteTable" class="shaded"></table>
		<span id="toggleWebsite" class="caption">+ add new</span>
		<div id="newWebsite">
			<label for="website_name">name </label><input type="text" id="website_name" /> 
			<label for="website_url">url </label><input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
	<fieldset id= "benchmarkField">
		<legend>benchmark types</legend>
			<table id="benchTypeTbl" class="shaded">
				<thead>
					<tr class="headerRow">
						<th id="benchName" length="${benchNameLen}">name</th>				
						<th id="benchDesc" length="${benchDescLen}">description</th>
						<th id="benchFile">file name</th>
					</tr>
				</thead>				
				<tbody>
				<c:forEach var="proc" items="${bench_proc}">
					<tr id="proc_${proc.id}">
						<td><a href="/${starexecRoot}/secure/edit/processor.jsp?type=bench&id=${proc.id}">${proc.name} <img class="extLink" src="/${starexecRoot}/images/external.png"/> </a></td>
						<td>${proc.description}</td>
						<td>${proc.fileName}</td>
					</tr>
				</c:forEach>
				</tbody>									
			</table>
		<span id="toggleBenchType" class="caption">+ add new</span>
		<form id=newTypeForm class="newForm" enctype="multipart/form-data" method="POST" action="/${starexecRoot}/secure/processors/manager">			
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
	<fieldset id="settingsField">
	<legend class="expd"><span></span>default settings</legend>
	<table id="settings" class ="shaded">
		<thead>
			<tr class="headerRow">
				<th class="label">name</th>
				<th>values</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td>post processor </td>
				<td>					
					<select id="editPostProcess" name="editPostProcess" default=${defaultPPId}>
					<option value=-1>none</option>
					<c:forEach var="proc" items="${post_proc}">
							<option value="${proc.id}">${proc.name}</option>
					</c:forEach>
					</select>
				</td>
			</tr>
			<tr>
				<td>wallclock timeout</td>
				<td id="editClockTimeout">${defaultClockTimeout}</td>
			</tr>	
			<tr>
				<td>cpu timeout</td>
				<td id="editCpuTimeout">${defaultCpuTimeout}</td>
			</tr>
			<tr>
				<td>dependencies enabled</td>
				<td>
					<select id="editDependenciesEnabled" name="editDependenciesEnabled" default=${dependenciesEnabled}>
						<option value="1">True</option>
						<option value="0">False</option>
					</select>
				</td>
			</tr>
			<tr id="defaultBenchRow">
				<td>default benchmark</td>
				<td>${defaultBenchmark} <a href="${defaultBenchLink}"><span id="selectBenchmark">select benchmark</span></a></td>
			</tr>
		</tbody>
	</table>
	</fieldset>	
	<!-- <fieldset>
		<legend>pre processors</legend>
		<form id="updatePrePrcssForm" class="updateForm" enctype="multipart/form-data" method="post" action="/${starexecRoot}/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="update"/>
			<input type="hidden" name="type" value="pre"/>
			<table id="preProcessorTbl" class="shaded">
				<thead>
					<tr class="headerRow">
						<th>name</th>				
						<th>description</th>
						<th>file name</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="proc" items="${pre_proc}">
						<tr id="proc_${proc.id}">
							<td><a href="/${starexecRoot}/secure/edit/processor.jsp?type=pre&id=${proc.id}">${proc.name}</a></td>
							<td>${proc.description}</td>
							<td>${proc.fileName}</td>
						</tr>
					</c:forEach>
				</tbody>																				
			</table>
		</form>
		<span id="togglePreProcessor" class="caption">+ add new</span>
		<form id="addPreProcessorForm" class="newForm" enctype="multipart/form-data" method="POST" action="/${starexecRoot}/secure/processors/manager">			
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
	<fieldset id="processorField">
		<legend>post processors</legend>
			<table id="postProcessorTbl" class="shaded">
				<thead>
					<tr class="headerRow">
						<th id="procName" length="${processorNameLen}">name</th>				
						<th id="procDesc" length="${processorDescLen}">description</th>
						<th>file name</th>
					</tr>								
				</thead>
				<tbody>
					<c:forEach var="proc" items="${post_proc}">
						<tr id="proc_${proc.id}">
							<td><a href="/${starexecRoot}/secure/edit/processor.jsp?type=post&id=${proc.id}">${proc.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/>  </a></td>
							<td>${proc.description}</td>
							<td>${proc.fileName}</td>
						</tr>
					</c:forEach>					
				</tbody>
			</table>
		<span id="togglePostProcessor" class="caption">+ add new</span>
		<form id="addPostProcessorForm" class="newForm" enctype="multipart/form-data" method="POST" action="/${starexecRoot}/secure/processors/manager">			
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
