<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

	try {
		request.setAttribute("solverType1",CacheType.CACHE_SOLVER.getVal());
		request.setAttribute("solverType2",CacheType.CACHE_SOLVER_REUPLOAD.getVal());
		
		request.setAttribute("benchmarkType1",CacheType.CACHE_BENCHMARK.getVal());
		
		request.setAttribute("jobType1",CacheType.CACHE_JOB_OUTPUT.getVal());
		request.setAttribute("jobType2",CacheType.CACHE_JOB_CSV.getVal());
		request.setAttribute("jobType3",CacheType.CACHE_JOB_CSV_NO_IDS.getVal());
		request.setAttribute("jobType4",CacheType.CACHE_JOB_PAIR.getVal());
		
		request.setAttribute("spaceType1",CacheType.CACHE_SPACE.getVal());
		request.setAttribute("spaceType2",CacheType.CACHE_SPACE_XML.getVal());
		request.setAttribute("spaceType3",CacheType.CACHE_SPACE_HIERARCHY.getVal());

	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "There was an error displaying the page");
	}

%>
<star:template title="manage cache" js="admin/cache, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin">	
		<fieldset id="fieldTable">
			<legend>Cache Types</legend>
			<table id="tableTypes" class="shaded contentTbl">
				<thead>
					<tr>
						<th>Type</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td id="benchmarkType"><span class="cacheType" value="${benchmarkType1}"></span> Benchmarks</td>
					</tr>
					<tr>
						
						<td id="jobType"><span class="cacheType" value="${jobType1}"></span>
						<span class="cacheType" value="${jobType2}"></span>
						<span class="cacheType" value="${jobType3}"></span>
						<span class="cacheType" value="${jobType4}"></span>Jobs</td>	
					</tr>
					<tr>				
						
						<td id="solverType"><span class="cacheType" value="${solverType1}"></span>
						<span class="cacheType" value="${solverType2}"></span>Solvers</td>
					</tr>
					<tr>
					
						<td id="spaceType">	<span class="cacheType" value="${spaceType1}"></span>
						<span class="cacheType" value="${spaceType2}"></span>
						<span class="cacheType" value="${spaceType3}"></span>spaces</td>
					</tr>
					
				</tbody>
			</table>
	</fieldset>
		<fieldset id="actionField">
			<legend>actions</legend>
			<button id="clearSelected">Clear Selected Types</button>	
			<button id="clearAll">Clear Entire Cache</button>	
		</fieldset>		
	
</star:template>