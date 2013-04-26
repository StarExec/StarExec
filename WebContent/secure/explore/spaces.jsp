<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="space explorer" js="common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/spaces, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min" css="common/delaySpinner, common/table, explore/common, explore/spaces, common/comments">			
	<div id="explorer">
		<h3>spaces</h3>
		 
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">				
		<h3 id="spaceName"></h3>
		<a id="trashcan"></a>
		<p id="spaceDesc" class="accent"></p>
			
		<fieldset id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<table id="jobs">
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th>complete</th>
						<th>total</th>
						<th>failed</th>
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
<!--  THIS NEEDS TO REMAIN COMMENTED OUT.		
		<fieldset id="resultField">
			<legend class="expd" id="resultExpd"><span>0</span> result</legend>
			<table id="results">
				<thead>
					<tr>
						<th style="width:150px;">name</th>
						<th>score</th>
						<th>runtime</th>
					</tr>
				</thead>			
			</table>			
		</fieldset>		
	
		<fieldset id="resultChart">
		<table id="chartTable">
			<tr>
				<td id="chart">
					<img id="chartPicture"><br>
				</td>
			</tr>
		</table>
		</fieldset>	
	-->	
		<fieldset id="actions">
			<legend>actions</legend>
			<ul id="actionList">
				<li><a class="btnAdd" id="addSpace" href="/starexec/secure/add/space.jsp">add subspace</a></li>
				<li><a class="btnUp" id="uploadBench" href="/starexec/secure/add/benchmarks.jsp">upload benchmarks</a></li>
				<li><a class="btnUp" id="uploadSolver" href="/starexec/secure/add/solver.jsp">upload solver</a></li>				
				<li><a class="btnRun" id="addJob" href="/starexec/secure/add/job.jsp">create job</a></li>
				<li><a class="btnDown" id="downloadXML" href="/starexec/secure/download">download space xml</a></li>				
				<li><a class="btnUp" id="uploadXML" href="/starexec/secure/add/batchSpace.jsp">upload space xml</a></li>
				<li><a class="btnEdit" id="editSpace" href="/starexec/secure/edit/space.jsp">edit space</a></li>
				<!--  <li><a class="btnRun" id="generateResultChart" href="/starexec/secure/generateResultChart">generate chart</a></li>-->
				<li><a class="btnRun" id="makePublic">make public</a></li>
				<li><a class="btnRun" id="makePrivate">make private</a></li>
				<li><a class="btnDown" id="downloadSpace">download space</a></li>
				
			</ul>
		</fieldset>	
		
<!-- 	Comments feature not yet polished, commenting-out for now
	
		<div id="commentDiv">		
		<fieldset id="commentField">
		<legend class="expd" id="commentExpd"><span>0</span> comments </legend>
			<table id="comments">
			<thead>
				<tr>
					<th style="width:20%;">user</th>
					<th style="width:20%;">time</th>
					<th>comment</th>					
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>			
		<span id="toggleComment" class="caption"><span>+</span> add new</span>
		<div id="new_comment">
			<textarea id="comment_text"></textarea>  
			<button id="addComment">add</button>
		</div>
		</fieldset>	
		</div>	
 -->	
	</div>	
	
	<div id="dialog-confirm-copy" title="confirm copy">
		<p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	
	
</star:template>