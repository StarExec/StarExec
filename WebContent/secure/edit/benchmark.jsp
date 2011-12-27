<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		long userId = SessionUtil.getUserId(request);
		long benchId = Long.parseLong(request.getParameter("id"));
		
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
				request.setAttribute("types", BenchTypes.getAll());
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

<star:template title="edit ${bench.name}" js="lib/jquery.validate.min, edit_benchmark" css="edit/benchmark">				
	<form id="editBenchmarkForm">
		<fieldset>
			<legend>benchmark details</legend>
			<table>
				<tr>
					<td class="label">benchmark name</td>			
					<td><input id="name" type="text" name="name" value="${bench.name}" maxlength="32"/></td>
				</tr>
				<tr>
					<td class="label">description</td>			
					<td><textarea id="description" name="description" >${bench.description}</textarea></td>
				</tr>
				<tr>
					<td class="label">type</td>
					<td>
						<select id="benchType" name="benchType" class="styled">
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
					<input id="downloadable" type="radio" name="downloadable" value="true"  ${isDownloadable}   >yes
					<input id="downloadable" type="radio" name="downloadable" value="false" ${isNotDownloadable}>no
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<button type="button" id="delete" class="round" >delete</button>
						<button type="button" id="update" class="round" >update</button>
					</td>
				</tr>						
			</table>	
		</fieldset>		
	</form>
</star:template>