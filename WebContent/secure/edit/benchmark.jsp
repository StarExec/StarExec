<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int benchId = Integer.parseInt(request.getParameter("id"));
		
		Benchmark b = null;
		if(Permissions.canUserSeeBench(benchId, userId)) {
			b = Benchmarks.get(benchId);
		}

		if(b != null) {
			// Ensure the user visiting this page is the owner of the benchmark
			if(userId != b.getUserId()){
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the owner of this benchmark can edit details about it.");
			} else {
				request.setAttribute("bench", b);
				if(b.isDownloadable()){
					request.setAttribute("isDownloadable", "checked");
					request.setAttribute("isNotDownloadable", "");
				} else {
					request.setAttribute("isDownloadable", "");
					request.setAttribute("isNotDownloadable", "checked");
				}
				request.setAttribute("types", Processors.getAll(ProcessorType.BENCH));
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Benchmark does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given benchmark id was in an invalid format");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="edit ${bench.name}" js="lib/jquery.validate.min, edit/benchmark" css="edit/shared">				
	<form id="editBenchmarkForm">
		<fieldset>
			<legend>benchmark details</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="label">name</td>			
						<td><input id="name" type="text" name="name" value="${bench.name}" maxlength="32"/></td>
					</tr>
					<tr>
						<td class="label">description</td>			
						<td><textarea id="description" name="description" >${bench.description}</textarea></td>
					</tr>
					<tr>
						<td class="label">type</td>
						<td>
							<select id="benchType" name="benchType">
								<c:forEach var="type" items="${types}">
										<c:choose>
											<c:when test="${type.name == bench.type.name}">
												<option selected value="${type.id}">${type.name}</option>	
											</c:when>
											<c:otherwise>
												<option value="${type.id}">${type.name}</option>
											</c:otherwise>
										</c:choose>
								</c:forEach>
							</select>
						</td>
					</tr>
					<tr>
						<td>downloadable</td>
						<td>
							<input id="downloadable" type="radio" name="downloadable" value="true" ${isDownloadable}> yes
							<input id="downloadable" type="radio" name="downloadable" value="false" ${isNotDownloadable}> no
						</td>
					</tr>						
				</tbody>			
			</table>	
			<button type="button" id="delete">delete</button>
			<button type="button" id="update">update</button>
		</fieldset>		
	</form>
</star:template>