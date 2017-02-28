<%@tag description="Benchmarking framework row used for job creation."%>
<%@tag trimDirectiveWhitespaces="true" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<tr class="noHover advancedOptions" id="benchmarkingFrameworkRow">
	<td>benchmarking framework</td>
	<td>					
		<select id="editBenchmarkingFramework" name="benchmarkingFramework">
			<option class="runsolverOption" value="RUNSOLVER" selected="selected">runsolver</option>
			<option class="benchexecOption" value="BENCHEXEC">BenchExec</option>
		</select>
	</td>
</tr>
