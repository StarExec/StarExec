<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="space explorer" js="lib/jquery.cookie, lib/jquery.jstree, space_explorer" css="space_explorer">			
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
			
		<fieldset>
			<legend>jobs</legend>
			<div id="jobs"></div>
		</fieldset>	
			
		<fieldset>
			<legend>solvers</legend>
			<div id="solvers"></div>
		</fieldset>						
		
		<fieldset>
			<legend>benchmarks</legend>
			<div id="benchmarks"></div>
		</fieldset>											
		
		<fieldset>
			<legend>users</legend>
			<div id="users"></div>
		</fieldset>		
		
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><span class="round button"><a id="addSpace" href="/starexec/secure/add/space.jsp">add space</a></span></li>
				<li><span class="round button"><a id="removeSpace" href="/starexec/secure/remove/space.jsp">delete space</a></span></li>
				<li><span class="round button"><a id="uploadBench" href="/starexec/secure/upload/bench.jsp">upload benchmarks</a></span></li>
				<li><span class="round button"><a id="uploadSolver" href="/starexec/secure/upload/solver.jsp">upload solver</a></span></li>				
			</ul>
		</fieldset>				
	</div>	
</star:template>