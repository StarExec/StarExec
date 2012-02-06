<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="space explorer" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/spaces, lib/jquery.qtip.min" css="common/table, explore/common, explore/spaces">			
	<div id="explorer">
		<h3>spaces</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">				
		<h3 id="spaceName"></h3>
		<p id="spaceDesc" class="accent"></p>
			
		<fieldset id="jobField">
			<legend onclick="toggleTable(this)" class="expd" id="jobExpd"><span>0</span> jobs <span>(+)</span></legend>
			<table id="jobs">
				<thead>
					<tr>
						<th>name</th>
						<th>status</th>
						<th style="width:270px;">description</th>
					</tr>
				</thead>			
			</table>
		</fieldset>	
			
		<fieldset id="solverField">
			<legend onclick="toggleTable(this)" class="expd" id="solverExpd"><span>0</span> solvers <span>(+)</span></legend>
			<table id="solvers">
				<thead>
					<tr>
						<th style="width:150px;">name</th>
						<th>description</th>						
					</tr>
				</thead>			
			</table>
		</fieldset>						
		
		<fieldset id="benchField">
			<legend onclick="toggleTable(this)" class="expd" id="benchExpd"><span>0</span> benchmarks <span>(+)</span></legend>
			<table id="benchmarks">
				<thead>
					<tr>
						<th style="width:150px;">name</th>
						<th>type</th>	
						<th>description</th>						
					</tr>
				</thead>		
			</table>
		</fieldset>											
		
		<fieldset  id="userField">
			<legend onclick="toggleTable(this)" class="expd" id="userExpd"><span>0</span> users <span>(+)</span></legend>
			<table id="users">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
					</tr>
				</thead>			
			</table>
		</fieldset>		
		
		<fieldset id="spaceField">
			<legend onclick="toggleTable(this)" class="expd" id="spaceExpd"><span>0</span> subspaces <span>(+)</span></legend>
			<table id="spaces">
				<thead>
					<tr>
						<th style="width:150px;">name</th>
						<th>description</th>
					</tr>
				</thead>			
			</table>
		</fieldset>		
		
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><a class="btnAdd" id="addSpace" href="/starexec/secure/add/space.jsp">add subspace</a></li>
				<li><a class="btnUp" id="uploadBench" href="/starexec/secure/add/benchmarks.jsp">upload benchmarks</a></li>
				<li><a class="btnUp" id="uploadSolver" href="/starexec/secure/add/solver.jsp">upload solver</a></li>				
				<li><a class="btnRun" id="addJob" href="/starexec/secure/add/job.jsp">create job</a></li>				
				<li><a class="btnRemove" id="removeSubspace">remove subspace</a></li>
				<li><a class="btnRemove" id="removeUser">remove user</a></li>
				<li><a class="btnRemove" id="removeBench">remove benchmark</a></li>
				<li><a class="btnRemove" id="removeSolver">remove solver</a></li>
				<li><a class="btnRemove" id="removeJob">remove job</a></li>				
			</ul>
		</fieldset>				
	</div>	
	
	<div id="dialog-confirm-copy" title="confirm copy">
		<p><span class="ui-icon ui-icon-info" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-copy-txt"></span></p>
	</div>
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
</star:template>