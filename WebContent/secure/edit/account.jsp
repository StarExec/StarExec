<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, java.util.List,org.starexec.data.to.Website.WebsiteType, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType" session="true"%> <%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		int userId=-1;
		try {
			userId = Integer.parseInt(request.getParameter("id"));	

		} catch (Exception e) {
			// if we can't get it from the URL, try to just use the current user ID
			userId=SessionUtil.getUserId(request);
		}
		User t_user = Users.get(userId);
		int visiting_userId = SessionUtil.getUserId(request);		
		
		long disk_usage = Users.getDiskUsage(t_user.getId());		
		
		if(t_user != null) {
			
			boolean owner = true;
			boolean hasAdminReadPrivileges = Users.hasAdminReadPrivileges(visiting_userId);
			boolean hasAdminWritePrivileges = Users.hasAdminWritePrivileges(visiting_userId);
			// The user can be deleted if the visting user has admin write privileges and the user being deleted is NOT an admin.
			boolean canDeleteUser =  hasAdminWritePrivileges && !Users.isAdmin(userId);
			if( (visiting_userId != userId) && !hasAdminReadPrivileges){
				owner = false;
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Must be the administrator to access this page");
			} else {
				List<DefaultSettings> listOfDefaultSettings=Settings.getDefaultSettingsVisibleByUser(userId);
				
				request.setAttribute("userId", userId);
				request.setAttribute("diskQuota", Util.byteCountToDisplaySize(t_user.getDiskQuota()));
				request.setAttribute("diskUsage", Util.byteCountToDisplaySize(disk_usage));
				request.setAttribute("sites", Websites.getAllForHTML(userId, WebsiteType.USER));
				request.setAttribute("settings",listOfDefaultSettings);
				
				List<Processor> ListOfPostProcessors = Processors.getByUser(userId,ProcessorType.POST);
				List<Processor> ListOfPreProcessors = Processors.getByUser(userId,ProcessorType.PRE);
				List<Processor> ListOfBenchProcessors = Processors.getByUser(userId,ProcessorType.BENCH);
				request.setAttribute("postProcs", ListOfPostProcessors);
				request.setAttribute("preProcs", ListOfPreProcessors);
				request.setAttribute("benchProcs",ListOfBenchProcessors);
			}
			
			request.setAttribute("owner", owner);
			request.setAttribute("canDeleteUser", canDeleteUser);
			request.setAttribute("hasAdminReadPrivileges", hasAdminReadPrivileges);
			request.setAttribute("hasAdminWritePrivileges", hasAdminWritePrivileges);
			request.setAttribute("user", t_user);
		}
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
	}
%>
<star:template title="edit account" css="common/table, common/pass_strength_meter, edit/account" js="common/defaultSettings,lib/jquery.validate.min, lib/jquery.validate.password, edit/account, lib/jquery.dataTables.min">
	<c:forEach items="${settings}" var="setting">
		<star:settings setting="${setting}" />
	</c:forEach>
	
	
	<div id="popDialog">
  		<img id="popImage" src=""/>
	</div>
	<p>review and edit your account details here.</p>
	<fieldset>
	<legend>personal information</legend>
	<table id="infoTable" uid="${userId}">
		<tr>
			<td id="picSection">
				<img id="showPicture" src="${starexecRoot}/secure/get/pictures?Id=${userId}&type=uthn" enlarge="${starexecRoot}/secure/get/pictures?Id=${userId}&type=uorg">
		    	<ul>
					<li><a class="btnUp" id="uploadPicture" href="${starexecRoot}/secure/add/picture.jsp?type=user&Id=${userId}">change</a></li>
				</ul>
			</td>
		<td id="userDetail">
			<table id="personal" class="shaded">   
			<thead> 	
				<tr>
					<th class="label">attribute</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
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
					<td id="editemail">${user.email}</td>
				</tr>
			</tbody>
		</table>
		</td>
		</tr>
		</table>
		<h6>(click the current value of an attribute to edit it)</h6>
	</fieldset>
	<c:if test="${hasAdminReadPrivileges}">
		<fieldset>
			<legend>user disk quota</legend>
				<table id="diskUsageTable" class="shaded" uid=${userId}>
					<thead>
						<tr>
							<th>attribute</th>
							<th>value</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td>disk quota <h6>Format: [Integer] [Units(B, KB, MB, GB)]</h6></td>
							<td id="editdiskquota">${diskQuota}</td>
						</tr>
						<tr>
							<td>current disk usage</td>
							<td>${diskUsage}</td>
						</tr>
					</tbody>			
				</table>
		</fieldset>
	</c:if>
	<fieldset>
		<legend>site settings</legend>
		<table id="siteSettingTable">
			<thead>
				<tr>
					<th>setting</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>table entries per page</td>
					<td type="int" id="editpagesize">${pagesize}</td>
				</tr>
			
			
			</tbody>
		
		</table>
		
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<table id="websites" class="shaded">
			<thead>
				<tr>
					<th>link</th>
					<th>action</th>					
				</tr>
			</thead>
			<tbody>
			<c:forEach items="${sites}" var="s">
				<tr>
					<td><a href="${s.url}">${s.name}<img class="extLink" src="${starexecRoot}/images/external.png"/></a></td>
					<td><a class="delWebsite" id="${s.id}">delete</a></td>
				</tr>
			</c:forEach>
			</tbody>
		</table>
		
		<span id="toggleWebsite" class="caption"><span>+</span> add new</span>
		<div id="new_website">
			name: <input type="text" id="website_name" /> 
			url: <input type="text" id="website_url" /> 
			<button id="addWebsite">add</button>
		</div>
	</fieldset>
	<fieldset>
		<legend>password</legend>
		<form id="changePassForm" method="post">
			<table id="passwordTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>current password</td>
						<td><input type="password" id="current_pass" name="current_pass"/></td>
					</tr>
					<tr>
						<td>new password</td>
						<td>
							<input type="password" id="password" name="pwd"/>
							<div class="password-meter" id="pwd-meter" style="visibility:visible">
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
					<tr></tr>
					<tr>
						<td class="notShaded" colspan="2"><button id="changePass">change</button></td>
					</tr>
				</tbody>
			</table>
		</form>
	</fieldset>
	<fieldset id="settingsField">
		<legend class="expd"><span></span>default settings</legend>
		<select id="settingProfile">
			<c:if test="${empty settings}">
				<option value="" />
			</c:if>				
		<c:forEach var="setting" items="${settings}">
		    <option class="settingOption" value="${setting.getId()}" type="${setting.getTypeString()}">${setting.name}</option>
		</c:forEach>
		</select>
		<table id="settings" class ="shaded">
			<thead>
				<tr class="headerRow">
					<th class="label">name</th>
					<th>values</th>
				</tr>
			</thead>
			<tbody>	
				<tr>
					<td title="the pre processor that will be selected by default for new jobs">pre processor </td>
					<td>					
						<select class="preProcessSetting" id="editPreProcess" name="editPreProcess" default="${defaultPreProcId}">
						<option value=-1>none</option>
						<c:forEach var="proc" items="${preProcs}">
								<option value="${proc.id}">${proc.name}</option>
						</c:forEach>
						</select>
					</td>
				</tr>
				
				<tr>
					<td title="the benchmark processor that will be selected by default for new jobs">bench processor </td>
					<td>					
						<select class="benchProcessSetting" id="editBenchProcess" name="editBenchProcess" default="${defaultBPId}">
						<option value=-1>none</option>
						<c:forEach var="proc" items="${benchProcs}">
								<option value="${proc.id}">${proc.name}</option>
						</c:forEach>
						</select>
					</td>
				</tr>
				
				<tr>
					<td title="the post processor that will be selected by default for new jobs">post processor </td>
					<td>					
						<select class="postProcessSetting" id="editPostProcess" name="editPostProcess" default="${defaultPPId}">
						<option value=-1>none</option>
						<c:forEach var="proc" items="${postProcs}">
								<option value="${proc.id}">${proc.name}</option>
						</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td title="the wallclock timeout that will be selected by default for new jobs">wallclock timeout</td>
					<td id="editClockTimeout"><input type="text" name="wallclockTimeout" id="wallclockTimeout"/></td>
				</tr>	
				<tr>
					<td title="the cpu timeout that will be selected by default for new jobs">cpu timeout</td>
					<td id="editCpuTimeout"><input type="text" name="cpuTimeout" id="cpuTimeout" /></td>
				</tr>
				<tr>
					<td title="the maximum memory that will be selected by default for new jobs">maximum memory</td>
					<td id="editMaxMem"><input type="text" name="maxMem" id="maxMem"/></td>
				</tr>
				<tr>
					<td>dependencies enabled</td>
					<td>
						<select class="dependencySetting" id="editDependenciesEnabled" name="editDependenciesEnabled">
							<option value="true">True</option>
							<option value="false">False</option>
						</select>
					</td>
				</tr>
				<tr id="defaultBenchRow">
					<td>default benchmark</td>
					<td id="benchmark"><p id="benchmarkNameField"></p> <span class="selectPrim clearBenchmark">clear benchmark</span></td>
				</tr>
				<tr id="defaultSolverRow">
					<td>default solver</td>
					<td id="solver"><p id="solverNameField"></p> <span class="selectPrim clearSolver">clear solver</span></td>
				</tr>
			</tbody>
		</table>
		<fieldset id="settingActions">
			<legend>actions</legend>
			<button id="saveProfile">save profile changes</button>
			
			<button id="createProfile">create new profile</button>
			<button title="Setting a profile as a default means it will be selected 
			automatically when visiting the job creation page" id="setDefaultProfile">set profile as default</button>
			<button id="deleteProfile">delete selected profile</button>
			
		</fieldset>
	</fieldset>	
	
	<fieldset>
		<legend>solvers</legend>
		<table id="solverList">
				<thead>
					<tr>
						<th>name</th>
						<th>description</th>
						<th>type</th>
					</tr>
				</thead>
				<tbody>
					<!-- Will be populated using AJAX -->
			</tbody>
			
		</table>
		<button id="useSolver">use selected solver</button>
		
	</fieldset>
	
	<fieldset>
		<legend>benchmarks</legend>
		<table id="benchmarkList">
				<thead>
					<tr>
						<th>name</th>
						<th>type</th>
					</tr>
				</thead>
				<tbody>
					<!-- Will be populated using AJAX -->
			</tbody>
			
		</table>
		<button id="useBenchmark">use selected benchmark</button>
		
	</fieldset>
	<c:if test="${canDeleteUser}">
		<fieldset>
			<legend>delete user</legend>
			<button id="deleteUser">delete user</button>
		</fieldset>
	</c:if>
	
	<div id="dialog-confirm-delete" title="confirm delete">
			<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-createSettingsProfile" title="create settings profile">
		<p><span id="dialog-createSettingsProfile-txt"></span></p><br/>
		<p><label>name: </label><input id="settingName" type="text"/></p>			
	</div>
</star:template>
