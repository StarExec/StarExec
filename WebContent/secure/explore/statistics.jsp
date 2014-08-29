<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Community Statistics" js="common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree,lib/jquery.qtip.min, explore/statistics" css="common/delaySpinner, common/table, details/shared,explore/jquery.qtip, explore/common">			

	
	<div id="mainPanel">
									
		<fieldset id="statistics">
			  <legend>statistics</legend>
			  <p id="lastUpdate" class="accent"></p>
			  <fieldset id="statsTableField" hidden>
			  	    <table id="statsTable">
				    	   <thead>
						<tr>
							<th>name</th>
							<th>users</th>
							<th>solvers</th>
							<th>bench</th>
							<th>jobs</th>
							<th>job<br>pairs</th>
							<th>disk use</th>
						</tr>
					   </thead>
				    </table>
			  </fieldset>
			  <fieldset id="graph" hidden>
			  	<legend>graphs</legend>
				<p align="center"><img id="communityOverview" src="/${starexecRoot}/images/loadingGraph.png" width="300" height="300" hidden /></p>
			  </fieldset>

			     <fieldset id="options" hidden>
				  <legend>compare by</legend>
				  <ul>
					  <li><button class="compareBtn" id="compareUsers" type="button">users</button></li>
				  	  <li><button class="compareBtn" id="compareSolvers" type="button">solvers</button></li>
				  	  <li><button class="compareBtn" id="compareBenches" type="button">benches</button></li>
				  	  <li><button class="compareBtn" id="compareJobs" type="button">jobs</button></li>
				  	  <li><button class="compareBtn" id="compareJobPairs" type="button">job pairs</button></li>
					  <li><button class="compareBtn" id="compareDiskUse" type="button">disk use</button></li>
				  </ul>
			     </fieldset>

			     <fieldset hidden>
				<button id="refreshStats" type="button">refresh statistics</button>
			     </fieldset>

		</fieldset>										 	
						
	</div>	
	
</star:template>