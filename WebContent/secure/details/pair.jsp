<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.database.JobPairs, org.starexec.data.database.Jobs,org.starexec.data.database.Permissions, org.starexec.data.database.Users,org.starexec.data.security.GeneralSecurity, org.starexec.data.security.JobSecurity, org.starexec.data.to.Job, org.starexec.data.to.JobPair, org.starexec.data.to.User, org.starexec.data.to.enums.BenchmarkingFramework, org.starexec.data.to.pipelines.JoblineStage, org.starexec.logger.StarLogger, org.starexec.util.SessionUtil, java.util.Optional" 
		import="org.starexec.data.to.Benchmark, org.starexec.data.database.Benchmarks, org.starexec.data.to.BenchmarkDependency, java.util.ArrayList, java.util.List"
		%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	StarLogger log = StarLogger.getLogger(JobPair.class);

	try {
		int userId = SessionUtil.getUserId(request);
		int pairId = Integer.parseInt(request.getParameter("id"));

		JobPair jp = JobPairs.getPairDetailed(pairId);
		if (jp == null) {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND, "Job does not exist");
			return;
		} else if (Permissions.canUserSeeJob(jp.getJobId(), userId)
		                      .isSuccess()) {
			Job j = Jobs.get(jp.getJobId());
			for (JoblineStage stage : jp.getStages()) {
				Optional<String> pairOutput =
						JobPairs.getStdOut(jp.getId(), stage.getStageNumber(),
						                   100
						);
				String tempOutput = "not available";
				if (pairOutput.isPresent()) {
					tempOutput = pairOutput.get();
				}
				String output = GeneralSecurity.getHTMLSafeString(tempOutput);
				stage.setOutput(output);
			}
			User u = Users.get(j.getUserId());
			String pairlog = GeneralSecurity
					.getHTMLSafeString(JobPairs.getJobLog(jp.getId()));
			boolean canRerun = (JobSecurity.canUserRerunPairs(j.getId(), userId,
			                                                  jp.getStatus()
			                                                    .getCode()
			                                                    .getVal()
			).isSuccess());
			boolean moreThanOneStage = jp.getStages().size() > 1;
			request.setAttribute(
					"isBenchExec", j.getBenchmarkingFramework() ==
							BenchmarkingFramework.BENCHEXEC);
			request.setAttribute(
					"isRunsolver", j.getBenchmarkingFramework() ==
							BenchmarkingFramework.RUNSOLVER);

			// List<BenchmarkDependency> benchDependencies = jp.getBench().getDependencies();
			List<BenchmarkDependency> benchDependencies = Benchmarks.getBenchDependencies(jp.getBench().getId());
			ArrayList<Integer> benchDependencyIds = new ArrayList<Integer>();
			for(BenchmarkDependency depend : benchDependencies){
				benchDependencyIds.add(depend.getSecondaryBench().getId());
			}
			request.setAttribute("benchDependencyIds", benchDependencyIds);

			request.setAttribute("moreThanOneStage", moreThanOneStage);
			request.setAttribute("pair", jp);
			request.setAttribute("job", j);
			request.setAttribute("usr", u);
			request.setAttribute("log", pairlog);
			request.setAttribute("rerun", canRerun);
		} else {
			response.sendError(
					HttpServletResponse.SC_FORBIDDEN,
					"You do not have permission to view this job pair"
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		log.error(nfe.getMessage(), nfe);
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given pair id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		log.error(e.getMessage(), e);
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		return;
	}
%>
<star:template title="${job.name} pair #${pair.id}"
               js="lib/jquery.dataTables.min, lib/prettify, lib/lang-log, details/pair, details/shared"
               css="common/table, details/shared, details/pair, prettify/prettify">
	<span id="pairId" value="${pair.id}"></span>
	<fieldset id="fieldDetails">
		<legend>details</legend>
		<table id="detailTable" class="shaded">
			<thead>
			<tr>
				<th>property</th>
				<th>value</th>
			</tr>
			</thead>
			<tbody>
			<tr title="${pair.status.getDescription()}">
				<td>status</td>
				<td>${pair.status.getStatus()}</td>
			</tr>
			<tr>
				<td>benchmark</td>
					<c:choose>
						<c:when test="${isLocalJobPage}">
							<td>${pair.bench.getName()}</td>
						</c:when>
						<c:otherwise>
							<td><star:benchmark value="${pair.bench}"/></td>
						</c:otherwise>
					</c:choose>
				
			</tr>
			<tr>
				<td>ran by</td>
				<c:choose>
						<c:when test="${isLocalJobPage}">
							<td>${usr}</td>
						</c:when>
						<c:otherwise>
							<td><star:user value="${usr}"/></td>
						</c:otherwise>
					</c:choose>
				
			</tr>
			<tr>
				<td>cpu timeout</td>
				<td>${job.cpuTimeout} seconds</td>
			</tr>
			<tr>
				<td>wallclock timeout</td>
				<td>${job.wallclockTimeout} seconds</td>
			</tr>
			<tr>
				<td>memory limit</td>
				<td>${job.maxMemory} bytes</td>
			</tr>
			<c:if test="${pair.status.code == 'STATUS_COMPLETE'}">
				<tr>
					<td>execution host</td>
					<c:choose>
						<c:when test="${isLocalJobPage}">
							<td>${pair.node.name}</td>
						</c:when>
						<c:otherwise>
							<td><a href="${starexecRoot}/secure/explore/cluster.jsp">${pair.node.name}
								<img class="extLink"
									 src="${starexecRoot}/images/external.png"/></a>
						</td>
						</c:otherwise>
					</c:choose>
					
						
				</tr>
			</c:if>
			<tr>
				<td>space</td>
				<td>${pair.jobSpaceName}</td>
			</tr>
			</tbody>
		</table>
	</fieldset>
	<c:forEach var="stage" items="${pair.getStages()}">
		<c:if test="${moreThanOneStage}">
			<%-- This fieldset is terminated in an identical <c:if> element further down --%>
			<fieldset class="fieldStats">
			<legend>stage ${stage.stageNumber} statistics</legend>
		</c:if>
		<fieldset class="stageStats">
			<legend>run statistics</legend>
			<table id="pairStats" class="shaded">
				<thead>
				<tr>
					<th>property</th>
					<th>value</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>solver</td>
					<c:choose>
						<c:when test="${isLocalJobPage}">
							<td>${stage.solver.getName()}</td>
						</c:when>
						<c:otherwise>
							<td><star:solver value="${stage.solver}"/></td>
						</c:otherwise>
					</c:choose>
				</tr>
				<tr>
					<td>configuration</td>
					<c:choose>
						<c:when test="${isLocalJobPage}">
							<td>${stage.solver.configurations[0].getName()}</td>
						</c:when>
						<c:otherwise>
							<td><star:config
								value="${stage.solver.configurations[0]}"/></td>
						</c:otherwise>
					</c:choose>
					
				</tr>
				<tr>
					<td>runtime (wallclock)</td>
					<td>${stage.wallclockTime} seconds</td>
				</tr>
				<tr title="the cpu time usage in seconds">
					<td>cpu usage</td>
					<td>${stage.cpuTime}</td>
				</tr>
				<c:if test="${isRunsolver}">
					<tr title="the total amount of time spent executing in user mode, expressed in microseconds">
						<td>user time</td>
						<td>${stage.userTime}</td>
					</tr>
				</c:if>
				<c:if test="${isRunsolver}">
					<tr title="the total amount of time spent executing in kernel mode, expressed in microseconds">
						<td>system time</td>
						<td>${stage.systemTime}</td>
					</tr>
				</c:if>
				<c:if test="${isBenchExec}">
					<tr title="the maximum memory size in bytes">
						<td>max memory</td>
						<td>${stage.maxVirtualMemory}</td>
					</tr>
				</c:if>
				<c:if test="${isRunsolver}">
					<tr title="the maximum vmem size in bytes">
						<td>max virtual memory</td>
						<td>${stage.maxVirtualMemory}</td>
					</tr>
				</c:if>
				<c:if test="${isRunsolver}">
					<tr title="the maximum resident set size used (in kilobytes)">
						<td>max residence set size</td>
						<td>${stage.maxResidenceSetSize}</td>
					</tr>
				</c:if>
				</tbody>
			</table>
		</fieldset>

		<fieldset class="fieldAttrs">
			<legend>stage attributes</legend>
			<c:choose>
				<c:when test="${stage.status.code == 'STATUS_COMPLETE' && empty stage.attributes}">
					<p>none</p>
				</c:when>
				<c:when test="${stage.status.code == 'STATUS_COMPLETE'}">
					<table id="pairAttrs" class="shaded">
						<thead>
						<tr>
							<th>key</th>
							<th>value</th>
						</tr>
						</thead>
						<tbody>
						<c:forEach var="entry" items="${stage.attributes}">
							<tr>
								<td>${entry.key}</td>
								<td>${entry.value}</td>
							</tr>
						</c:forEach>
						</tbody>
					</table>
				</c:when>
				<c:otherwise>
					<p>unavailable</p>
				</c:otherwise>
			</c:choose>
		</fieldset>
		<fieldset class="fieldOutput">
			<legend><img alt="loading" src="${starexecRoot}/images/loader.gif">
				output
			</legend>
			<textarea class=contentTextarea id="jpStdout"
			          readonly="readonly">${stage.output}</textarea>
			<c:choose>
				<c:when test="${!isLocalJobPage}">
					<a href="${starexecRoot}/services/jobs/pairs/${pair.id}/stdout/${stage.stageNumber}?limit=-1"
					target="_blank" class="popoutLink">popout</a>
				</c:when>
				<c:otherwise>
					<a href="./output/${pair.id}/${stage.stageNumber}.txt"
					target="_blank" class="popoutLink">popout</a>
				</c:otherwise>
			</c:choose>
			
			<p class="caption">output may be truncated. 'popout' for the full
				output.</p>
		</fieldset>
		<c:if test="${moreThanOneStage}">
			</fieldset>
		</c:if>
	</c:forEach>

	<c:choose>
		<c:when test="${!isLocalJobPage}">
			<fieldset id="fieldLog">
				<legend><img alt="loading" src="${starexecRoot}/images/loader.gif"> job
					log
				</legend>
				<star:displayTextContents text="${log}" lang="log"/>
				<a href="${starexecRoot}/services/jobs/pairs/${pair.id}/log"
				   target="_blank" class="popoutLink">popout</a>
			</fieldset>
		
		</c:when>
	</c:choose>
	
	<fieldset id="fieldActions">
		<legend>actions</legend>
		<c:if test="${!isLocalJobPage}">
			<a href="${starexecRoot}/secure/download?type=jp_output&id=${pair.id}"
			id="downLink">all output</a>
		</c:if>
		<c:choose>
			<c:when test="${isLocalJobPage}">
				<a href="./job.html"
				id="returnLink">return to ${job.name}</a>
			</c:when>
			<c:otherwise>
				<a href="${starexecRoot}/secure/details/job.jsp?id=${job.id}"
		   id="returnLink">return to ${job.name}</a>
			</c:otherwise>
		</c:choose>
		<c:if test="${!isLocalJobPage}">
			<c:if test="${rerun}">
				<button id="rerunPair">rerun pair</button>
			</c:if>
		</c:if>

		<!-- IDV and GDV stuff from here down -->
		<c:if test="${pair.status.getStatus() == 'complete'}">
			<script>
				window.benchmarkIds = JSON.parse("${benchDependencyIds}").concat([${pair.bench.id}]);

				function findProof(output){
					let lines = output.split("\n");
					lines = lines.map(l => l.split("\t")[1]);
					window.lines = lines;

					let hasProof = false;
					let start = -1;
					let end = -1;
					for(let [i,line] of lines.entries()){
						if(line === undefined)
							continue;
						if (line.includes("SZS status Theorem") || line.includes("SZS status Unsatisfiable"))
							hasProof = true;
						else if (line.includes("SZS output start"))
							start = i+1;
						else if (line.includes("SZS output end"))
							end = i;
					}

					if(hasProof){
						lines = lines.splice(start, end-start);
						return lines.join("\n");
					}
					else{
						return null;
					}
				}

				function submitProofToIDV(proof) {
					let form = document.createElement("form");
					form.id = "form"
					form.method = "POST"
					form.enctype = "multipart/form-data"
					form.action = "http://tptp.org/idv/idv"

					let proofInput = document.createElement("textarea");
					proofInput.value = proof
					proofInput.name = "proof"
					proofInput.form = "form"

					let button = document.createElement("input");
					button.type = "submit"

					form.appendChild(proofInput)
					form.appendChild(button)
					document.body.appendChild(form);
					form.submit();
				}

				function submitProofToGDV(proof){

					textContent = [
						"% SZS output start ListOfFormulae",
						benchmarkContents.join("\n\n"),
						"% SZS output end ListOfFormulae",
						"% SZS output start Proof",
						proof,
						"% SZS output end Proof"
					].join("\n\n")

					let form = document.createElement("form");
					form.id = "form";
					form.method = "POST";
					form.enctype = "multipart/form-data";
					form.action = "http://www.tptp.org/cgi-bin/SystemOnTPTPFormReply";

					
					let textContentInput = document.createElement("textarea");
					textContentInput.name = "FORMULAEProblem";
					textContentInput.value = textContent;
					textContentInput.innerHTML = textContent;
					textContentInput.form = "form";

					let GDVInputs = `
						<input type="radio" name="ProblemSource" value="FORMULAE" checked>
						<input type="radio" name="QuietFlag" value="-q01" checked>
						<input type="checkbox" name="System___GDV---0.0" value="GDV---0.0" checked>
						<input type="text" name="TimeLimit___GDV---0.0" tabindex="20" value="300" size="3" maxlength="3" />
						<input type="text" name="Transform___GDV---0.0" tabindex="20" value="fofify:!" size="20" />
						<input type="text" name="Format___GDV---0.0" tabindex="20" value="tptp:raw" size="20" />
						<input type="text" name="Command___GDV---0.0" tabindex="20" value="run_GDV %s" size="20" />
						<input id="GDVSubmitButton" type="submit" name="SubmitButton" value="ProcessSolution" form="form">
					`;

					form.appendChild(textContentInput);
					form.innerHTML += GDVInputs;

					document.body.appendChild(form);
					document.querySelector("#GDVSubmitButton").click()
				}

				// Fetch the complete output from this job pair.
				let outputPath = "${starexecRoot}/services/jobs/pairs/${pair.id}/stdout/1?limit=-1";
				fetch(outputPath)
					.then(response => response.text())
					.then(function(output){
						window.proof = findProof(output);
						if(window.proof !== null){

							let idvButton = document.createElement("button");
							idvButton.id = "idvButton";
							idvButton.innerText = "visualize proof with IDV";
							idvButton.addEventListener("click", () => window.submitProofToIDV(window.proof));
							document.querySelector("#fieldActions > .expdContainer").appendChild(idvButton);
							$("#idvButton").button({icons: {primary: "ui-icon-lightbulb-1-e"}});

							let gdvButton = document.createElement("button");
							gdvButton.id = "gdvButton";
							gdvButton.innerText = "verify proof with GDV";
							gdvButton.addEventListener("click", () => window.submitProofToGDV(window.proof));
							document.querySelector("#fieldActions > .expdContainer").appendChild(gdvButton);
							$("#gdvButton").button({icons: {primary: "ui-icon-check-1-e"}});
							
						}
					})



				// Fetch the complete TPTP source from the benchmarks.
				let benchmarkPromises = window.benchmarkIds.map(function(id){
					return fetch(`${starexecRoot}/services/benchmarks/\${id}/contents?limit=-1`);
				})

				Promise.all(benchmarkPromises)
					.then(responses => Promise.all(responses.map(r => r.text())))
					.then(function(responses){
						window.benchmarkContents = responses;
						// Remove include lines now that I've imported all benchmarks dependencies.
						// This will not be good if dependencies can have dependencies...if so, fix the JSP stuff above.
						benchmarkContents = benchmarkContents.map(function(text){
							let lines = text.split("\n");
							let regex = /\s*include\(.+\)\./
							lines = lines.map(function(l){
								while(match = l.match(regex)){
									l = l.substr(0,match.index) + l.substr(match.index + match[0].length)
								}
								return l;
							})
							return lines.join("\n");
						})
					})


			</script>
		</c:if>

	</fieldset>
</star:template>
