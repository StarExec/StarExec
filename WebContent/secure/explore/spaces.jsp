<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	
	//System.out.println("start");
	int uid=SessionUtil.getUserId(request);
	//System.out.println(uid);
	request.setAttribute("userId",uid);
	
%>
<star:template title="space explorer" js="common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/spaces, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, explore/common, explore/spaces">			
	<span id="userId" value="${userId}" ></span>
	<div id="explorer">
		<h3>spaces</h3>
		 
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
						<th>name</th>
						<th>status</th>
						<th><span title="Job pairs that ran successfully">completed</span></th>
						<th><span title="The total number of job pairs in this job">total</span></th>
						<th><span title="Job pairs for which there was a timeout, mem-out, or internal error">failed</span></th>
						<th>time</th>
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
						<th style="width:150px;">name</th>
						<th>description</th>						
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
			<table id="benchmarks">
				<thead>
					<tr>
						<th style="width:75%;">name</th>
						<th>type</th>											
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
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
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
						<th style="width:150px;">name</th>
						<th>description</th>
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
				<li><a class="btnDown" id="downloadXML" href="/${starexecRoot}/secure/download">download space xml</a></li>				
				<li><a class="btnUp" id="uploadXML" href="/${starexecRoot}/secure/add/batchSpace.jsp">upload space xml</a></li>
				<li><a class="btnEdit" id="editSpace" href="/${starexecRoot}/secure/edit/space.jsp">edit space</a></li>
				<li><a class="btnRun" id="makePublic">make public</a></li>
				<li><a class="btnRun" id="makePrivate">make private</a></li>
				<li><a class="btnDown" id="downloadSpace">download space</a></li>
				<li><a class="btnAdd" id="reserveQueue" href="/${starexecRoot}/secure/reserve/queue.jsp">Reserve Queue</a></li>
				<li><a class="btnRun" id="processBenchmarks" href="/${starexecRoot}/edit/processBenchmarks.jsp">process benchmarks</a></li>
			</ul>
		</fieldset>	

	</div>	
	
	<div id="dialog-confirm-copy" title="confirm copy">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-download-space" title="download space">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-download-space-txt"></span></p><br>
		<p><input type="radio" name="downloadOption" id="downloadSolvers"/> solvers only<br>
		<input type="radio" name="downloadOption" id="downloadBenchmarks"/> benchmarks only<br>
		<input type="radio" name="downloadOption" id="downloadBoth" checked="checked"/> solvers + benchmarks<br></p>
	</div>
	
	
	
</star:template>