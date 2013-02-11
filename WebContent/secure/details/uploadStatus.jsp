<%@page import="java.util.ArrayList, java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8" import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%		
	try {
		int userId = SessionUtil.getUserId(request);
		int statusId = Integer.parseInt(request.getParameter("id"));
		
		UploadStatus s = null;
		
		if(Permissions.canUserSeeStatus(statusId, userId)) {
			s = Uploads.get(statusId);
		}		
		
		if(s != null) {
			request.setAttribute("status", s);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Upload Status does not exist or is restricted");			
		}
		response.setIntHeader("Refresh", 10);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given upload status id was in an invalid format");		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
	}
%>

<star:template title="upload status" js="details/shared, lib/jquery.dataTables.min" css="details/shared, common/table">				
	<fieldset>
		<legend>details</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>upload date</td>			
					<td><fmt:formatDate pattern="MMM dd yyyy" value="${status.uploadDate}" /></td>
				</tr>
				<tr>
					<td>file upload complete</td>			
					<td>${status.fileUploadComplete}</td>
				</tr>							
				<tr>
					<td>file extraction complete</td>			
					<td>${status.fileExtractionComplete}</td>
				</tr>
				<tr>
					<td>begun validating</td>			
					<td>${status.processingBegun}</td>
				</tr>	
				<tr>
					<td>total spaces</td>			
					<td>${status.totalSpaces}</td>
				</tr>	
				<tr>
					<td>total benchmarks</td>			
					<td>${status.totalBenchmarks}</td>
				</tr>	
				<tr>
					<td>completed spaces</td>			
					<td>${status.totalSpaces}</td>
				</tr>	
				<tr>
					<td>completed benchmarks</td>			
					<td>${status.completedBenchmarks}</td>
				</tr>		
			</tbody>
		</table>	
	</fieldset>

		<a id="returnLink" href="/starexec/secure/explore/spaces.jsp">back</a>
	
</star:template>