<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@page import="java.util.ArrayList, java.util.List"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {	
		List<Space> publicCommunities = Communities.getCommsWithPublicSolvers();
		List<Solver> publicSolvers = Solvers.getPublicSolvers();
		request.setAttribute("publicSolvers",publicSolvers);
		request.setAttribute("publicCommunities",publicCommunities);
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bad request.");
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "not found");		
	}
%>
<star:template title="create your own job" css="add/singleJobPair" js="lib/jquery.validate.min, add/singleJobPair,  lib/*, http://code.jquery.com/jquery-latest.js">

	<form method="POST" action="/starexec/public/misc/SingleJobPair" id="singleJobPairForm">
		<input type="hidden" name="solverId" value="${solver.id}"/>
		<fieldset id="save">
			<legend>write a benchmark</legend>		
			<table id="saveBenchmarkTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr id="publicCommunities">
						<td class="label"><p>communities</p></td>
						<td><select id="publicCommunity" name="publicCommunity" onChange="alert.(hello!)">
								<c:forEach var="pCommunity" items="${publicCommunities}">
									<option value="${pCommunity.id}" title="${pCommunity.description}">${pCommunity.name}</option>
								</c:forEach>
						</select></td>
					</tr>
					<tr>
						<td>benchmark contents</td>
						<td><textarea id="benchmarkContents" name="benchmarkContents" rows="60" cols="50"/>
						benchmark contents
						</textarea> </td>
					</tr>
				</tbody>
					<tr id="publicSolvers">
						<td class="label"><p>solvers</p></td>
						<td><select id="publicSolver" >
								
								<option value = "whatever"/>
								
						</select></td>
					</tr>
			</table>																
			<button class="saveBtn" type="submit">start job</button>
		</fieldset>
	</form>
	<div></div>
</star:template>