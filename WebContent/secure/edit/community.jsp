<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		boolean admin = Users.isAdmin(userId);
		
		request.setAttribute("isAdmin", admin);
		request.setAttribute("communityNameLen", R.SPACE_NAME_LEN);
		request.setAttribute("communityDescLen", R.SPACE_DESC_LEN);
		request.setAttribute("processorNameLen", R.PROCESSOR_NAME_LEN);
		request.setAttribute("processorDescLen", R.PROCESSOR_DESC_LEN);
		request.setAttribute("benchNameLen", R.BENCH_NAME_LEN);
		request.setAttribute("benchDescLen", R.BENCH_DESC_LEN);

		int id = Integer.parseInt((String)request.getParameter("cid"));
		request.setAttribute("sites", Websites.getAllForHTML(id, Websites.WebsiteType.SPACE));
		
		Space com = Communities.getDetails(id);
		Permission perm = SessionUtil.getPermission(request, id);
		
		if(com == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		} else if (perm == null || !perm.isLeader()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only community leaders can edit their communities");		
		} else {
			DefaultSettings settings = Communities.getDefaultSettings(id);
			String url = Util.docRoot("secure/edit/defaultPrimitive.jsp?id="+settings.getId());
			
			request.setAttribute("defaultBenchLink", url+"&type=benchmark");
			request.setAttribute("defaultSolverLink", url+"&type=solver");
			request.setAttribute("com", com);	
			request.setAttribute("bench_proc", Processors.getByCommunity(id, ProcessorType.BENCH));
			request.setAttribute("pre_proc", Processors.getByCommunity(id, ProcessorType.PRE));
			request.setAttribute("post_proc", Processors.getByCommunity(id, ProcessorType.POST));
                        request.setAttribute("update_proc", Processors.getByCommunity(id, ProcessorType.UPDATE));
			request.setAttribute("defaultCpuTimeout", settings.getCpuTimeout());
			request.setAttribute("defaultClockTimeout", settings.getWallclockTimeout());
			request.setAttribute("defaultPPId", settings.getPostProcessorId());
			request.setAttribute("defaultBPId", settings.getBenchProcessorId());
	
			request.setAttribute("dependenciesEnabled",settings.isDependenciesEnabled());
			request.setAttribute("defaultPreProcId", settings.getPreProcessorId());
			request.setAttribute("defaultMaxMem",Util.bytesToGigabytes(settings.getMaxMemory()));
			request.setAttribute("settingId",settings.getId());
			try {
				Benchmark bench=Benchmarks.get(settings.getBenchId());
				if (bench!=null) {
					request.setAttribute("defaultBenchmark", bench.getName());
				} else {
					request.setAttribute("defaultBenchmark", "none specified");
				}
			} catch (Exception e) {
				request.setAttribute("defaultBenchmark", "none specified");
			}
			try {
				Solver solver = Solvers.get(settings.getSolverId());
				if (solver!=null) {
					request.setAttribute("defaultSolver",solver.getName());
				} else {
					request.setAttribute("defaultSolver","none specified");
				}
			} catch (Exception e) {
				request.setAttribute("defaultSolver","none specified");
			}
		
		
		

	}
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${com.name}" js="lib/jquery.dataTables.min, lib/jquery.validate.min, edit/community" css="common/table, edit/community">
	
	<input type="hidden" value="${com.id}" id="comId"/>
	<input type="hidden" value="${settingId}" id="settingId"/>
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
		<table id="websiteTable" class="shaded">
		
		<tbody>
		
			<c:forEach var="site" items="${sites}">
				<tr>
					<td><a href="${site.url}" target="_blank">${site.name} <img class="extLink" src="/${starexecRoot}/images/external.png"/></a></td>
					<td><a class="delWebsite" id="${site.id}">delete</a></td>
				</tr>
			
			</c:forEach>	
		</tbody>
		
		</table>
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
				<td>pre processor </td>
				<td>					
					<select id="editPreProcess" name="editPreProcess" default="${defaultPreProcId}">
					<option value=-1>none</option>
					<c:forEach var="proc" items="${pre_proc}">
							<option value="${proc.id}">${proc.name}</option>
					</c:forEach>
					</select>
				</td>
			</tr>
			
			<tr>
				<td>bench processor </td>
				<td>					
					<select id="editBenchProcess" name="editBenchProcess" default="${defaultBPId}">
					<option value=-1>none</option>
					<c:forEach var="proc" items="${bench_proc}">
							<option value="${proc.id}">${proc.name}</option>
					</c:forEach>
					</select>
				</td>
			</tr>
			
			<tr>
				<td>post processor </td>
				<td>					
					<select id="editPostProcess" name="editPostProcess" default="${defaultPPId}">
					<option value=-1>none</option>
					<c:forEach var="proc" items="${post_proc}">
							<option value="${proc.id}">${proc.name}</option>
					</c:forEach>
					</select>
				</td>
			</tr>
			<tr>
				<td>update processor </td>
				<td>					
					<select id="editUpdateProcess" name="editUpdateProcess" default="${defaultPPId}">
					<option value=-1>none</option>
					<c:forEach var="proc" items="${update_proc}">
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
				<td>maximum memory</td>
				<td id="editMaxMem">${defaultMaxMem}</td>
			</tr>
			<tr>
				<td>dependencies enabled</td>
				<td>
					<select id="editDependenciesEnabled" name="editDependenciesEnabled" default="${dependenciesEnabled}">
						<option value="1">True</option>
						<option value="0">False</option>
					</select>
				</td>
			</tr>
			<tr id="defaultBenchRow">
				<td>default benchmark</td>
				<td>${defaultBenchmark} <a href="${defaultBenchLink}"><span class="selectPrim">select benchmark</span></a></td>
			</tr>
			<tr id="defaultSolverRow">
				<td>default solver</td>
				<td>${defaultSolver} <a href="${defaultSolverLink}"><span class="selectPrim">select solver</span></a></td>
			</tr>
		</tbody>
	</table>
	</fieldset>	
	<fieldset id="preProcessorField">
		<legend>pre processors</legend>
		
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
						<td><a href="/${starexecRoot}/secure/edit/processor.jsp?type=pre&id=${proc.id}">${proc.name} <img class="extLink" src="/${starexecRoot}/images/external.png"/> </a></td>
						<td>${proc.description}</td>
						<td>${proc.fileName}</td>
					</tr>
				</c:forEach>
			</tbody>																				
		</table>
		
	</fieldset>
	<fieldset id="postProcessorField">
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
			<input type="hidden" name="type" value="update"/>
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
	<fieldset id="updateProcessorField">
		<legend>update processors</legend>
			<table id="updateProcessTbl" class="shaded">
				<thead>
					<tr class="headerRow">
						<th id="procName" length="${processorNameLen}">name</th>				
						<th id="procDesc" length="${processorDescLen}">description</th>
						<th>file name</th>
					</tr>								
				</thead>
				<tbody>
					<c:forEach var="proc" items="${update_proc}">
						<tr id="proc_${proc.id}">
							<td><a href="/${starexecRoot}/secure/edit/processor.jsp?type=post&id=${proc.id}">${proc.name}<img class="extLink" src="/${starexecRoot}/images/external.png"/>  </a></td>
							<td>${proc.description}</td>
							<td>${proc.fileName}</td>
						</tr>
					</c:forEach>					
				</tbody>
			</table>
		<span id="toggleUpdateProcessor" class="caption">+ add new</span>
		<form id="addUpdateProcessorForm" class="newForm" enctype="multipart/form-data" method="POST" action="/${starexecRoot}/secure/processors/manager">			
			<input type="hidden" name="com" value="${com.id}"/>
			<input type="hidden" name="action" value="add"/>
			<input type="hidden" name="type" value="update"/>
			<table id="newUpdateProcessTbl">			
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
					<td colspan="2"><button id="addUpdateProcessor" type="submit">add</button></td>				
				</tr>			 			 			
			</table>
		</form>
	</fieldset>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
</star:template>
