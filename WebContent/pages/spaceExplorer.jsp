<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="space explorer" js="jquery.cookie, jquery.jstree, spaceExplorer" css="spaceExplorer">			
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
	</div>	
</star:template>