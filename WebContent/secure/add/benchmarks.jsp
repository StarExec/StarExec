<%@page contentType="text/html" pageEncoding="UTF-8"
	import="org.starexec.data.database.*,org.starexec.data.to.*,org.starexec.util.*,org.starexec.data.to.Processor.ProcessorType"%>
<%@page import="java.util.ArrayList, java.util.List"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	try {
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		DefaultSettings settings = Communities.getDefaultSettings(spaceId);
		
		int userId = SessionUtil.getUserId(request);
		List<Space> userSpaces = new ArrayList<Space>();
		List<Processor> postProcs = Processors.getByCommunity(Spaces.getCommunityOfSpace(spaceId), ProcessorType.BENCH);
		userSpaces = Spaces.getSpacesByUser(userId);

		postProcs.add(Processors.getNoTypeProcessor());

		request.setAttribute("space", Spaces.get(spaceId));
		request.setAttribute("types", postProcs);
		request.setAttribute("userSpaces",userSpaces);
		request.setAttribute("dependenciesEnabled",settings.isDependenciesEnabled());
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if (!p.canAddBenchmark()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,
					"You do not have permission to add benchmarks here");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"The parent space id was not in the correct format");
	} catch (Exception e) {
		e.printStackTrace();
		response.sendError(
				HttpServletResponse.SC_NOT_FOUND,
				"You do not have permission to upload benchmarks to this space or the space does not exist");
	}
%>

<star:template title="upload benchmarks to ${space.name}" css="common/delaySpinner, add/benchmark" js="common/delaySpinner, lib/jquery.validate.min, add/benchmarks, lib/jquery.qtip.min">
	<form id="uploadForm" enctype="multipart/form-data" method="POST"
		action="/${starexecRoot}/secure/upload/benchmarks">
		<input type="hidden" name="space" value="${space.id}" />
		<fieldset>
			<legend>upload benchmarks</legend>
			<table id="tblUploadBench" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>upload method</td>
						<td>local file<input type="radio" id="radioLocal" name="localOrURL" value="local"/> URL <input type=radio id="radioURL" name="localOrURL" value="URL"/></td>
					</tr>
					<tr title="select the archive file containing your benchmarks">
						<td><label for="typeFile">benchmarks</label></td>
						<td><input name="benchFile" type="file" id="benchFile" /><input name="url" type="text" id="fileURL"/></td>
					</tr>
					<tr>
						<td class="label"><p>upload method</p></td>
						<td><input id="radioConvert" type="radio" name="upMethod"
							value="convert" checked="checked" /> <label for="radioConvert"
							title="this method will keep your archive's directory structure and convert each directory into a new space. benchmarks in that directory will be placed within the new space.">convert
								file structure to space structure</label> <br /> <input id="radioDump"
							type="radio" name="upMethod" value="dump" /> <label
							for="radioDump"
							title="this method discards your archive's directory structure and simply extracts all files and adds them to the space you're uploading to.">place
								all benchmarks in ${space.name}</label></td>
					</tr>
					<tr id="permRow"
						title="if StarExec expands your archive into spaces, the new spaces will have these default permissions">
						<td class="label"><p>default</p></td>
						<td>
							<table id="tblDefaultPerm">
								<tr>
									<th></th>
									<th>solver</th>
									<th>bench</th>
									<th>users</th>
									<th>space</th>
									<th>job</th>
								</tr>
								<tr>
									<td>add</td>
									<td><input type="checkbox" name="addSolver" /></td>
									<td><input type="checkbox" name="addBench" /></td>
									<td><input type="checkbox" name="addUser" /></td>
									<td><input type="checkbox" name="addSpace" /></td>
									<td><input type="checkbox" name="addJob" /></td>
								</tr>
								<tr>
									<td>remove</td>
									<td><input type="checkbox" name="removeSolver" /></td>
									<td><input type="checkbox" name="removeBench" /></td>
									<td><input type="checkbox" name="removeUser" /></td>
									<td><input type="checkbox" name="removeSpace" /></td>
									<td><input type="checkbox" name="removeJob" /></td>
								</tr>
							</table>
						</td>
					</tr>
					<tr
						title="which pre-processor should process your benchmark and endorse it with a type?">
						<td class="label"><p>benchmark type</p></td>
						<td><select id="benchType" name="benchType">
								<c:forEach var="type" items="${types}">
									<option value="${type.id}">${type.name}</option>
								</c:forEach>
						</select></td>
					</tr>
					<tr
						title="can other members who can see these benchmarks download them?">
						<td class="label"><p>downloadable</p></td>
						<td><input id="radioDownload" type="radio" name="download"
							value="true" checked="checked" /> <label for="radioDownload">yes</label>
							<input id="radioNoDownload" type="radio" name="download"
							value="false" /> <label for="radioNoDownload">no</label></td>
					</tr>
					<tr
						title="are some of these benchmarks dependent on previously uploaded benchmarks?">
						<td class="label"><p>dependencies</p></td>
						<td id="selectDep" default="${dependenciesEnabled}"><input id="radioDependency" type="radio"
							name="dependency" value="true" /> <label
							for="radioDependency">yes</label> <input id="radioNoDependency"
							type="radio" name="dependency" value="false"/> <label
							for="radioNoDependency">no</label></td>
					</tr>
					<tr id="depSpaces">
						<td class="label"><p>dependency root space</p></td>
						<td><select id="depRoot" name="depRoot">
								<c:forEach var="uSp" items="${userSpaces}">
									<option value="${uSp.id}" title="${uSp.description}">${uSp.name} (${uSp.id})</option>
								</c:forEach>
						</select></td>
					</tr>
					<tr id="depLinked">
						<td>first directory in path corresponds to dependent bench
							space</td>
						<td><input id="linked" type="radio" name="linked"
							value="true" checked="checked" /><label>yes</label> <input
							id="notLinked" type="radio" name="linked" value="false" /><label>no</label></td>
					</tr>
					<tr>
						<td colspan="1"><button id="btnPrev">Cancel</button></td>						
						<td colspan="1"><button id="btnUpload" type="submit">upload</button></td>
					</tr>
				</tbody>
			</table>			
		</fieldset>
	</form>

</star:template>