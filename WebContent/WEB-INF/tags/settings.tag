<!-- This tag stores a DefaultSettings profile on the page as a hidden set of spans -->

<%@tag description="A hidden default settings object" import="org.starexec.data.database.Settings, org.starexec.data.to.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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

			<c:forEach items="${Settings.getDefaultBenchmarks(setting.id)}" var="bench">
				<span class="benchId" value="${bench.id}"></span>
				<span class="benchName" value="${bench.name}"></span>
			</c:forEach>
			
			<span class="dependency" value="${setting.isDependenciesEnabled()}"></span>
			
</span>
