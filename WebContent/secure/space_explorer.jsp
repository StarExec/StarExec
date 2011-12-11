<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="space explorer" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, space_explorer" css="table, space_explorer">			
	<div id="explorer">
		<h3>spaces</h3>
		<ul id="spaceList">
		</ul>
	</div>
	
	<div id="detailPanel">		
		
		<fieldset>
			<legend>space</legend>			
			<h3 id="spaceName"></h3>
			<p id="spaceDesc"></p>			
		</fieldset>
			
		<fieldset id="jobField">
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> jobs <span>(+)</span></legend>
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
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> solvers <span>(+)</span></legend>
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
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> benchmarks <span>(+)</span></legend>
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
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> users <span>(+)</span></legend>
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
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> subspaces <span>(+)</span></legend>
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
				<li><span class="round button"><a id="addSpace" href="/starexec/secure/add/space.jsp">add subspace</a></span></li>
				<li><span class="round button"><a id="uploadBench" href="/starexec/secure/add/benchmarks.jsp">upload benchmarks</a></span></li>
				<li><span class="round button"><a id="uploadSolver" href="/starexec/secure/add/solver.jsp">upload solver</a></span></li>				
				<li><span class="round button"><a id="removeSubspace">remove subspace</a></span></li>
				<li><span class="round button"><a id="removeUser">remove user</a></span></li>
				<li><span class="round button"><a id="removeBench">remove benchmark</a></span></li>
				<li><span class="round button"><a id="removeSolver">remove solver</a></span></li>
				<li><span class="round button"><a id="removeJob">remove job</a></span></li>
			</ul>
		</fieldset>				
	</div>	
</star:template>