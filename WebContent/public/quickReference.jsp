<%@page contentType="text/html" pageEncoding="UTF-8"%>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="Quick Reference Guide" css="explore/quickRef">	

<div id="support">
<p>This a quick reference to help you accomplish basic tasks in StarExec.  
For a more extensive guide, see the <a href="http://wiki.uiowa.edu/display/stardev/User+Guide">User Guide</a>. 
This guide is organized by the method one uses to accomplish tasks.
</p>

<p>The main page on StarExec is the Space Explorer.  This page is accessed 
by selecting the Spaces tab in the upper right and selecting Explore in the 
dropdown menu. The left hand side of the Space Explorer is a representation 
of the space hierarchy that you have access to.
When you click on a space on the space tree, you can see information about it 
on the right hand side.
</p>
	
<p><strong>Space Information</strong>
All of the following are represented in tables with rows that can be clicked 
on for additional information:
</p>
	<ul id="support">
		<li>Jobs</li>
		<li>Solvers</li>
		<li>Benchmarks</li>
		<li>Users</li>
		<li>Subspaces</li>
	</ul>	

<p><strong>Dragging and Dropping</strong>
All drags are from the right hand side of the Space Explorer page to the left
</p>

<p><strong>Drag and Drop uses</strong></p>
	<ul id="support">
		<li>Add benchmarks, users, or solvers to a space by dragging them to the space in the space explorer tree</li>
		<li>Delete a space, benchmark or user by dragging to a trash can icon that appears near the top of the screen</li>
	</ul>

<p><strong>Action buttons</strong> 
At the bottom of the right hand side</p>
	<ul id="support">
		<li>create job - this will take you to a sequence of pages where you enter job parameters, select benchmarks and solvers, and submit the job</li>
		<li>add subspace to the currently selected space</li>
		<li>upload benchmarks - typically you will be creating a space hierarchy corresponding to the directory structure of a zipped directory of benchmarks</li>
		<li>upload and download xml representations of space hierarchies - an upload will create the space hierarchy
		using already existing benchmarks and solvers.</li>
		<li>edit space - edit basic information about the space
		<li>upload solver</li>

			<ul id="support">
				<li>upload a compressed directory that contains at least one configuration(run script) that will tell starexec how to run your
				solver on the execution nodes</li>
		    	<li>configuration file name must have the prefix "starexec_run_".  Everything after the prefix will be the name of the configuration
		    	within starexec</li>
		    	<li>configurations must be placed in a /bin folder in the top level directory of the uploaded archive</li>
		    	<li>Your script has access to a few different arguments and environmental variables, the most important being $1, the
		    	absolute path the the benchmark input file</li>
		    </ul>
	</ul>
</div>
	
</star:template>
