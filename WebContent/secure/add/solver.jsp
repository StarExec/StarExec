<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*,org.starexec.constants.*,org.starexec.data.security.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		request.setAttribute("space", Spaces.get(spaceId));
		
		request.setAttribute("solverNameLen", R.SOLVER_NAME_LEN);
		request.setAttribute("solverDescLen", R.SOLVER_DESC_LEN);
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if( !GeneralSecurity.hasAdminReadPrivileges(userId) && (p==null || !p.canAddSolver()) ) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add solvers here");
		}
		List<DefaultSettings> listOfDefaultSettings=Settings.getDefaultSettingsVisibleByUser(userId);
		request.setAttribute("defaultSettings",listOfDefaultSettings);	
		Integer defaultId=Settings.getDefaultProfileForUser(userId);
		if (defaultId!=null && defaultId>0) {
			request.setAttribute("defaultProfile",defaultId);
		} else {
			request.setAttribute("defaultProfile",-1);
		}

	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to upload solvers to this space or the space does not exist");		
	}
%>
<star:template title="upload solver to ${space.name}" css="common/delaySpinner, add/solver" js="common/defaultSettings, common/delaySpinner ,lib/jquery.validate.min, add/solver">
	<span id="defaultProfile" style="display:none" value="${defaultProfile}"></span>
	
	<form method="POST" enctype="multipart/form-data" action="${starexecRoot}/secure/upload/solvers" id="upForm" flag= "false">
		<input type="hidden" name="space" value="${space.id}"/>
		<fieldset>
			<legend>solver information</legend>		
			<table id="tblSolver" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>upload method</td>
						<td>
							local file<input type="radio" id="radioLocal" name="upMethod" value="local"/> 
							URL <input type=radio id="radioURL" name="upMethod" value="URL"/>
						</td>
					</tr>
					
					<tr id="localRow">
						<td>solver location</td>
						<td>
							<input type="file" name="f" id="fileLoc"/>
							<input name="url" type="text" id="fileURL"/>
						</td>	
					</tr>
					
					<tr>
						<td>solver name</td>
						<td><input id="name" name="sn" type="text" size="42" length="${solverNameLen}" /></td>
					</tr>
					
					<tr>
						<td>description method</td>
						<td>
							Archive<input type="radio" id="radioUpload" name="descMethod" value="upload"/> 
							Text<input type="radio" id="radioText" name="descMethod" value="text"/> 
							Local File<input type="radio" id="radioFile" name="descMethod" value="file"/>
						</td>
					</tr>
					
					<tr id="textBoxRow">
						<td>solver description</td>
						<td>
						    <textarea id="description" rows="6" cols="40" name="desc" length="${solverDescLen}"></textarea>
						    <input name="d" type="file" id="fileLoc2"/>
						    <p id = "default">Will search the archive upload for starexec_description.txt and extract the description</p>
						</td>
					</tr>
										
					<tr>
						<td>downloadable</td>
						<td>
							<input name="dlable" type="radio" value="true" checked="checked" /><label>yes</label>
							<input name="dlable" type="radio" value="false" /><label>no</label>
						</td>
					</tr>
					<tr>
						<td>Executable Type</td>
						<td>
							<select name="type">
								<option value="1">solver</option>
								<option value="2">preprocessor</option>
								<option value="3">result checker</option>
								<option value="4">other</option>
							</select>
						</td>
					</tr>
					<tr>
						<td title="After uploading this solver, a job will immediately be created in which this solver
						is run against the default benchmark for this community, using community default settings.">run test job</td>
						<td>
							<input id="useTestJob" class="testJobRadio" name="runTestJob" type="radio" value="true" /><label>yes</label>
							<input id="noTestJob" class="testJobRadio" name="runTestJob" name="runTestJob" type="radio" value="false" checked="checked" /><label>no</label>		
						</td>
					</tr>
					<tr id="settingRow" class="noHover" title="what default settings profile would you like to use?">
						<td>setting profile</td>
						<td>
							<select name="setting" id="settingProfile">	
									<c:forEach var="setting" items="${defaultSettings}">
		                                <option value="${setting.getId()}">${setting.name}</option>
									</c:forEach>
							</select>
						</td>
					</tr>
					<tr>
						<td colspan="1"><button id="btnPrev" type="button">Cancel</button></td>						
						<td colspan="1"><button id="btnUpload" type="submit">upload</button></td>
					</tr>
					
				</tbody>
			</table>																	
		</fieldset>
	</form>
</star:template>
