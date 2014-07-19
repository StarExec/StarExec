<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*,org.starexec.data.security.*,org.starexec.util.*, org.starexec.data.to.*, org.starexec.data.database.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	int uid=SessionUtil.getUserId(request);

	try {
		int spaceId=Integer.parseInt(request.getParameter("id"));
		if (SpaceSecurity.canUserSeeSpace(spaceId,uid)==0 && spaceId > 0) {
			List<Integer> idChain=Spaces.getChainToRoot(spaceId);
			StringBuilder stringChain=new StringBuilder();
			for (Integer id : idChain) {
				stringChain.append(id);
				stringChain.append(",");
			}
			stringChain.delete(stringChain.length()-1,stringChain.length());
			request.setAttribute("spaceChain",stringChain.toString());
		} else {
			request.setAttribute("spaceChain","1");

		}

	} catch (Exception e) {
		// we don't need the id, so we can just ignore errors here. It may not exist
	}

	request.setAttribute("userId",uid);
	
	request.setAttribute("cacheType1",CacheType.CACHE_SPACE.getVal());
	request.setAttribute("cacheType2",CacheType.CACHE_SPACE_XML.getVal());
	request.setAttribute("cacheType3",CacheType.CACHE_SPACE_HIERARCHY.getVal());
	request.setAttribute("isAdmin",Users.isAdmin(uid));
	
	
%>
<star:template title="Space Explorer" js="util/sortButtons, common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, explore/spaces,  lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, explore/common, explore/jquery.qtip, explore/spaces">			
	<span id="userId" value="${userId}" ></span>
	<span id="spaceChain" value="${spaceChain}"></span>
	<div id="explorer">
		<h3>Spaces</h3>
		 
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">				
		<h3 id="spaceName"></h3>
		<a id="trashcan" class="active"></a>
		<p id="spaceDesc" class="accent"></p>
		<p id="spaceID" class="accent"></p>
		<fieldset id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<table id="jobs">
				<thead>
					<tr>
						<th id="jobNameHead">name</th>
						<th id="jobStatusHead">status</th>
						<th id="jobCompletedHead"><span title="Job pairs that ran successfully">completed</span></th>
						<th id="jobTotalHead"><span title="The total number of job pairs in this job">total</span></th>
						<th id="jobFailedHead"><span title="Job pairs for which there was a timeout, mem-out, or internal error">failed</span></th>
						<th id="jobTimeHead">time</th>
					</tr>
				</thead>			
			</table>
			<div class="selectWrap">
				<p class="selectAllJobs">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllJobs">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>	
			
		<fieldset id="solverField">
			<legend class="expd" id="solverExpd"><span>0</span> solvers</legend>
			<table id="solvers">
				<thead>
					<tr>
						<th id="solverNameHead">name</th>
						<th id="solverDescHead">description</th>						
					</tr>
				</thead>			
			</table>
			<div class="selectWrap">
				<p class="selectAllSolvers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllSolvers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>						
		
		<fieldset id="benchField">
			<legend class="expd" id="benchExpd"><span>0</span> benchmarks</legend>
			<button title="sorts benchmarks in the order they were added to this space" asc="true" class="sortButton" id="additionSort" value="2">sort by addition order</button>
			<table id="benchmarks">
				<thead>
					<tr>
						<th id="benchNameHead">name</th>
						<th id="benchTypeHead">type</th>											
					</tr>
				</thead>		
			</table>
			<div class="selectWrap">
				<p class="selectAllBenchmarks">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllBenchmarks">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>											
		
		<fieldset  id="userField">
			<legend class="expd" id="userExpd"><span>0</span> users</legend>
			<table id="users">
				<thead>
					<tr>
						<th id="userNameHead">name</th>
						<th id="userInstitutionHead">institution</th>
						<th id="userEmailHead">email</th>
					</tr>
				</thead>			
			</table>
			<div class="selectWrap">
				<p class="selectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>		
		
		<fieldset id="spaceField">
			<legend class="expd" id="spaceExpd"><span>0</span> subspaces</legend>
			<table id="spaces">
				<thead>
					<tr>
						<th id="spaceNameHead">name</th>
						<th id="spaceDescriptionHead">description</th>
					</tr>
				</thead>			
			</table>
		</fieldset>		

		<fieldset id="actions">
			<legend>actions</legend>
			<ul id="actionList">
				<li><a class="btnAdd" id="addSpace" href="/${starexecRoot}/secure/add/space.jsp">add subspace</a></li>
				<li><a class="btnUp" id="uploadBench" href="/${starexecRoot}/secure/add/benchmarks.jsp">upload benchmarks</a></li>
				<li><a class="btnUp" id="uploadSolver" href="/${starexecRoot}/secure/add/solver.jsp">upload solver</a></li>				
				<li><a class="btnRun" id="addJob" href="/${starexecRoot}/secure/add/job.jsp">create job</a></li>
				<li><a class="btnDown" id="downloadXML" >download space xml</a></li>				
				<li><a class="btnUp" id="uploadXML" href="/${starexecRoot}/secure/add/batchSpace.jsp">upload space xml</a></li>
				<li><a class="btnUp" id="uploadJobXML" href="/${starexecRoot}/secure/add/batchJob.jsp">upload job xml</a></li>
				<li><a class="btnEdit" id="editSpace" href="/${starexecRoot}/secure/edit/space.jsp">edit space</a></li>
				<li><a class="btnEdit" id="editSpacePermissions" href="/${starexecRoot}/secure/edit/spacePermissions.jsp">edit space permissions</a></li>
				<li><a class="btnRun" id="makePublic">make public</a></li>
				<li><a class="btnRun" id="makePrivate">make private</a></li>
				<li><a class="btnDown" id="downloadSpace">download space</a></li>
				<!-- <li><a class="btnAdd" id="reserveQueue" href="/${starexecRoot}/secure/reserve/queue.jsp">Reserve Queue</a></li>-->
				<li><a class="btnRun" id="processBenchmarks" href="/${starexecRoot}/edit/processBenchmarks.jsp">process benchmarks</a></li>
			</ul>
			
			<c:if test="${isAdmin}">
				<span id="cacheType1" class="cacheType" value="${cacheType1}"></span>
				<span id="cacheType2" class="cacheType" value="${cacheType2}"></span>
				<span id="cacheType3" class="cacheType" value="${cacheType3}"></span>
				<button type="button" id="clearCache">clear cache</button>
			</c:if>
			
		</fieldset>	

	</div>	
	<div id="dialog-confirm-change" title="confirm change">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-change-txt"></span></p>
	</div>
	<div id="dialog-confirm-copy" title="confirm copy">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-spacexml-attributes" title="include attributes">
  	  <p><span id="dialog-spacexml-attributes-txt"></span></p>
	</div>
	<div id="dialog-download-space" title="download space">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-download-space-txt"></span></p><br>
		<p><input type="radio" name="downloadOption" id="downloadSolvers"/> solvers only<br>
		<input type="radio" name="downloadOption" id="downloadBenchmarks"/> benchmarks only<br>
		<input type="radio" name="downloadOption" id="downloadBoth" checked="checked"/> solvers + benchmarks<br></p>
	</div>
	<div id="dialog-warning" title="warning">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>		
	
	
	
</star:template>