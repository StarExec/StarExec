<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		// Get parent space info for display
		long spaceId = Long.parseLong(request.getParameter("sid"));
		long userId = SessionUtil.getUserId(request);
		request.setAttribute("space", Spaces.get(spaceId));
		request.setAttribute("types", BenchTypes.getAll());
		
		// Verify this user can add spaces to this space
		Permission p = SessionUtil.getPermission(request, spaceId);
		if(!p.canAddBenchmark()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to add benchmarks here");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The parent space id was not in the correct format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to upload benchmarks to this space or the space does not exist");		
	}
%>

<star:template title="upload benchmarks to ${space.name}" css="add/benchmark" js="lib/jquery.validate.min, add/benchmarks">
	<form id="uploadForm" enctype="multipart/form-data" method="POST" action="/starexec/secure/upload/solvers">
		<input type="hidden" name="space" value="${space.id}"/>
		<fieldset>
			<legend>upload benchmarks</legend>
			<table id="tblUploadBench">
				<tr>
					<td><label for="typeFile">benchmarks</label></td>
					<td><input name="benchFile" type="file" id="benchFile"/></td>
				</tr>
				<tr>
					<td class="label"><p>upload method</p></td>
					<td>
						<input id="radioConvert" type="radio" name="upMethod" value="convert" checked="checked"/> <label for="radioConvert">convert file structure to space structure</label>
						<br/><input  id="radioDump" type="radio" name="upMethod" value="dump" /> <label for="radioDump">place all benchmarks in ${space.name}</label>
					</td>
				</tr>
				<tr id="permRow">
					<td class="label"><p>default</p></td>
					<td>			
						<table id="tblDefaultPerm">
							<tr>
								<th></th>
								<th>solver</th>
								<th>bench</th>
								<th>users</th>
								<th>space</th>
							</tr>
							<tr>
								<td>add</td>
								<td><input type="checkbox" name="addSolver"/></td>
								<td><input type="checkbox" name="addBench"/></td>
								<td><input type="checkbox" name="addUser"/></td>
								<td><input type="checkbox" name="addSpace"/></td>
							</tr>
							<tr>
								<td>remove</td>
								<td><input type="checkbox" name="removeSolver"/></td>
								<td><input type="checkbox" name="removeBench"/></td>
								<td><input type="checkbox" name="removeUser"/></td>
								<td><input type="checkbox" name="removeSpace"/></td>
							</tr>
						</table>
					</td>
				</tr>			
				<tr>
					<td class="label"><p>benchmark type</p></td>
					<td>			
						<select id="benchType" name="benchType">
							<c:forEach var="type" items="${types}">
								<option value="${type.id}">${type.name}</option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td class="label"><p>downloadable</p></td>
					<td>			
						<input id="radioDownload" type="radio" name="download" value="true" checked="checked"/> <label for="radioDownload">yes</label>
						<input id="radioNoDownload" type="radio" name="download" value="false" /> <label for="radioNoDownload">no</label>
					</td>
				</tr>				
				<tr>
					<td colspan="2"><button type="submit" class="round">upload</button></td>
				</tr>
			</table>		
		</fieldset>
	</form>		
</star:template>