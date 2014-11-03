<!-- This tag stores a DefaultSettings profile on the page as a hidden set of spans -->

<%@tag description="A hidden default settings object" import="org.starexec.data.to.*"%>
<%@attribute name="setting" required="true" description="The setting object" type="org.starexec.data.to.DefaultSettings" %>

<span class="defaultSettingsProfile" name="${setting.name}" value="${setting.getId()}">
			<span class="cpuTimeout" value="${setting.cpuTimeout}" ></span>
			<span class="clockTimeout" value="${setting.wallclockTimeout}"></span>
			<span class="maxMemory" value="${setting.getRoundedMaxMemoryAsDouble()}"></span>
			<span class="solverId" value="${setting.solverId}"></span>
			<span class="solverName" value="${setting.getSolverName()}"></span>
			
			<span class="preProcessorId" value="${setting.preProcessorId}"></span>
			<span class="postProcessorId" value="${setting.postProcessorId}"></span>
			<span class="benchProcessorId" value="${setting.benchProcessorId}"></span>
			<span class="benchId" value="${setting.getBenchId()}"></span>
			<span class="benchName" value="${setting.getBenchmarkName()}"></span>
			
			<span class="dependency" value="${setting.isDependenciesEnabled()}"></span>
			
</span>