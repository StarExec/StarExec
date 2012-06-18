<%@page contentType="text/html" pageEncoding="UTF-8"%>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Quick Reference Guide" css="explore/quickRef">	
	<p> This a quick reference to help you accomplish basic tasks in StarExec.  For a more extensive guide to Starexec, see the 
	<a href="http://wiki.uiowa.edu/display/stardev/User+Guide"><u>user guide</u>.</a>  The guide is organized by the method one uses to accomplish tasks.</p>
	<br/>
	<p>The main page on starexec is the space explorer.  The space explorer is accessed by selecting the 
	spaces tab in the upper right and selecting explore in the dropdown.
	The left hand side of the space explorer is a representation of the space hierarchy that you have access to.
	When you click on a space on the space tree, you can see information about it on the right hand side<p>	
		
	<br/>	
	
	<p><b>Space Information</b> - all of the following are represented in tables with rows that can be clicked on for
	additional information</p>
	<ul id="qrTable">
		<li>jobs</li>
		<li>solvers</li>
		<li>benchmarks</li>
		<li>users</li>
		<li>subspaces</li>
	</ul>	
	<p><b>Dragging and Dropping</b> - all drags are from the right hand side of the space explorer page to the left</p>
	<p>Drag and Drop uses</p>
	<ul id="qrTable">
		<li>add benchmarks, users, or solvers to a space by dragging them to the space in the space explorer tree</li>
		<li>deleting a space, benchmark or user by dragging to a trashcan icon that appears near the top of the screen</li>
	</ul>
	<p><b>Action buttons</b> at the bottom of the right hand side</p>
	<ul id="qrTable">
		<li>create job - this will take you to a sequence of pages where you enter job parameters, select benchmarks and solvers, and submit the job</li>
		<li>add subspace to the currently selected space</li>
		<li>upload benchmarks - typically you will be creating a space hierarchy corresponding to the directory structure of a zipped directory of benchmarks</li>
		<li>upload and download xml representations of space hierarchies - an upload will create the space hierarchy
		using already existing benchmarks and solvers.</li>
		<li>edit space - edit basic information about the space
		<li>upload solver</li>
			<ul id="qrTable">
				<li>upload a compressed directory that contains at least one configuration(run script) that will tell starexec how to run your
				solver on the execution nodes</li>
		    	<li>configuration file name must have the prefix "run_".  Everything after the prefix will be the name of the configuration
		    	within starexec</li>
		    	<li>configurations must be placed in a /bin folder in the top level directory of the uploaded archive</li>
		    	<li>Your script has access to a few different arguments and environmental variables, the most important being $1, the
		    	absolute path the the benchmark input file</li>
		    </ul>
	</ul>
	
	
</star:template>