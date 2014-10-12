<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.to.*,java.util.*,org.starexec.servlets.CreateJob,org.apache.log4j.*, org.starexec.constants.*,java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.Processor.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%	
	Logger log=Logger.getLogger(CreateJob.class);

	try {		
		// Get parent space info for display
		int spaceId = Integer.parseInt(request.getParameter("sid"));
		int userId = SessionUtil.getUserId(request);
		// Verify this user can add jobs to this space
		Permission p=null;
		if (spaceId>0) {
			p = SessionUtil.getPermission(request, spaceId);
		}
		
		//having a negative space ID just means that no space was given
		if(spaceId>0 && !p.canAddJob()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to create a job here");
		} else {
			if (spaceId>0) {
				request.setAttribute("spaceName", Spaces.getName(spaceId));
			} else {
				request.setAttribute("spaceName", "job space");
			}
			request.setAttribute("spaceId", spaceId);
			request.setAttribute("jobNameLen", R.JOB_NAME_LEN);
			request.setAttribute("jobDescLen", R.JOB_DESC_LEN);
			request.setAttribute("benchNameLen",R.BENCH_NAME_LEN);
			int commId=-1;
			List<DefaultSettings> listOfDefaultSettings=new ArrayList<DefaultSettings>();
			List<Space> comms=Communities.getAll();
			//TODO: Allow users to choose and also set up default settings for the entire system
			int settingCounter=0;
			if (comms.size()>0) {
				for (int i=0;i<comms.size();i++) {
					DefaultSettings s=Communities.getDefaultSettings(comms.get(i).getId());
					s.setTempId(settingCounter);
					settingCounter++;
					listOfDefaultSettings.add(s);

				}
				commId=comms.get(0).getId();
			}
			
			List<Processor> ListOfPostProcessors = Processors.getByUser(userId,ProcessorType.POST);
			List<Processor> ListOfPreProcessors = Processors.getByUser(userId,ProcessorType.PRE);
			List<Processor> ListOfBenchProcessors = Processors.getByUser(userId,ProcessorType.BENCH);
			List<Solver> listOfSolvers = Solvers.getByUser(userId);
			ListOfBenchProcessors.add(Processors.getNoTypeProcessor());

			request.setAttribute("queues", Queues.getQueuesForUser(userId));

			request.setAttribute("postProcs", ListOfPostProcessors);
			request.setAttribute("preProcs", ListOfPreProcessors);
			request.setAttribute("benchProcs",ListOfBenchProcessors);
			request.setAttribute("solvers",listOfSolvers);
			request.setAttribute("defaultSettings",listOfDefaultSettings);
		}
	} catch (NumberFormatException nfe) {
		log.error(nfe.getMessage(),nfe);
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The space id was not in the correct format");
	} catch (Exception e) {
		
		log.error(e.getMessage(),e);
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to add to this space or the space does not exist");		
	}
%>

<jsp:useBean id="now" class="java.util.Date" />
<star:template title="run quick job" css="common/delaySpinner, common/table, add/quickJob" js="common/delaySpinner, lib/jquery.validate.min, add/quickJob, lib/jquery.dataTables.min, lib/jquery.qtip.min">
	<c:forEach items="${defaultSettings}" var="setting">
		<span class="defaultSettingsProfile" name="${setting.name}" value="${setting.tempId}">
			<span class="cpuTimeout" value="${setting.cpuTimeout}" />
			<span class="clockTimeout" value="${setting.wallclockTimeout}"/>
			<span class="maxMemory" value="${setting.getRoundedMaxMemoryAsDouble()}"/>
			<span class="solverId" value="${setting.solverId}"/>
			<span class="solverName" value="${setting.getSolverName()}"/>
			
			<span class="preProcessorId" value="${setting.preProcessorId}"/>
			<span class="postProcessorId" value="${setting.postProcessorId}"/>
			<span class="benchProcessorId" value="${setting.benchProcessorId}"/>
			<span class="benchContents" value="${setting.getBenchmarkContents()}"/>
			
		</span>
	</c:forEach>
	<form id="addForm" method="post" action="/${starexecRoot}/secure/add/job">	
		<input type="hidden" name="runChoice" value="quickJob" />
		<input type="hidden" name="seed" value="0" />
		<input type="hidden" name="sid" value="${spaceId}"/>
		<fieldset id="baseSettings">
			<legend>configure job</legend>
			<table id="tblConfig" class="shaded contentTbl">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr class="noHover" title="what default settings profile would you like to use?">
						<td>setting profile</td>
						<td>
							<select id="settingProfile">
									<c:if test="${empty defaultSettings}">
										<option value="" />
									</c:if>				
									<c:forEach var="setting" items="${defaultSettings}">
		                                <option value="${setting.tempId}">${setting.name}</option>
									</c:forEach>
							</select>
						</td>
					</tr>
					<tr class="noHover" title="how do you want this job to be displayed in StarExec?">
						<td class="label"><p>job name</p></td>
						<td><input length="${jobNameLen}" id="txtJobName" name="name" type="text" value="${spaceName} <fmt:formatDate pattern="MM-dd-yyyy HH.mm" value="${now}" />"/></td>
					</tr>
					<tr class="noHover" title="what solver would you like to run during this job?">
						<td class="label"><p>solver</p></td>
						<td><input id="solver" type="hidden" name="solver"/><p><span id="solverNameSpan"></span></td>
					</tr>
					<tr class="noHover" title="what benchmark would you like to use?">
						<td>benchmark selection</td>
						<td><textarea id="benchmarkField" name="bench"></textarea></td>
					</tr>
				</tbody>
			</table>
			</fieldset>
			<fieldset id= "advancedSettings">
				<legend>advanced settings</legend>
				<table id="tblAdvancedConfig" class="shaed contentTbl">
					<thead>
						<tr>
							<th>attribute</th>
							<th>value</th>
						</tr>
					</thead>
					<tbody>
						<tr class="noHover" title="what name would you like to give this benchmark in StarExec?">
							<td class="label"><p>benchmark name</p></td>
							<td><input length="${benchNameLen}" id="txtBenchName" name="benchName" type="text" value="${spaceName} <fmt:formatDate pattern="MM-dd-yyyy HH.mm" value="${now}" />"/></td>
						</tr>
						<tr class="noHover" title="are there any additional details that you want to document with the job?">
							<td class="label"><p>job description</p></td>
							<td><textarea length="${jobDescLen}" id="txtDesc" name="desc" rows="6" draggable="false"></textarea></td>
						</tr>
						<tr class="noHover" title="do you want to alter benchmarks before they are fed into the solvers?">
							<td class="label"><p>pre processor</p></td>
							<td>					
								<select id="preProcess" name="preProcess"">
									<option value="-1" selected="selected">none</option>
									<c:forEach var="proc" items="${preProcs}">
											<option value="${proc.id}">${proc.name} (${proc.id})</option>
									</c:forEach>
								</select>
							</td>
						</tr>
						<tr class="noHover" title="do you want to extract any attributes from your benchmark?">
							<td class="label"><p>bench processor</p></td>
							<td>					
								<select id="benchProcess" name="benchProcess" >
									<c:forEach var="proc" items="${benchProcs}">
											<option value="${proc.id}">${proc.name} (${proc.id})</option>
									</c:forEach>
								</select>
							</td>
						</tr>
						
						<tr class="noHover" title="do you want to extract any custom attributes from the job results?">
							<td class="label"><p>post processor</p></td>
							<td>					
								<select id="postProcess" name="postProcess">
									<option value="-1" selected="selected">none</option>
									<c:forEach var="proc" items="${postProcs}">
											<option value="${proc.id}">${proc.name} (${proc.id})</option>
									</c:forEach>
								</select>
							</td>
						</tr>
						
						<tr class="noHover" title="which queue should this job be submitted to?">
							<td class="label"><p>worker queue</p></td>
							<td>
								<select id="workerQueue" name="queue">
									<c:if test="${empty queues}">
										<option value="" />
									</c:if>				
									<c:forEach var="q" items="${queues}">
		                                <option cpumax="${q.cpuTimeout}" wallmax="${q.wallTimeout}" value="${q.id}">${q.name} (${q.id})</option>
									</c:forEach>
								</select>
							</td>
						</tr>
						<tr class="noHover" title="the maximum wallclock time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
							<td class="label"><p>wallclock timeout</p></td>
							<td>	
								<input type="text" name="wallclockTimeout" id="wallclockTimeout"/>
							</td>
						</tr>
						<tr class="noHover" title="the maximum CPU time (in seconds) that each pair can execute before it is terminated (max is any value less than 1)">
							<td class="label"><p>cpu timeout</p></td>
							<td>	
								<input type="text" name="cpuTimeout" id="cpuTimeout" />
							</td>
						</tr>
						<tr class="noHover" title="the maximum memory usage (in gigabytes) that each pair can use before it is terminated. The minimum of this value and half the available memory on the nodes will be used.">
							<td class="label"><p>maximum memory</p></td>
							<td>	
								<input type="text" name="maxMem" id="maxMem"/>
							</td>
						</tr>
						<tr class="noHover" title="Would you like to immediately pause the job upon creation?">
							<td class="label"><p>Create Paused</p></td>
							<td>
								Yes<input type="radio" id="radioYesPause" name="pause" value="yes"/> 	
								No<input type="radio" id="radioNoPause" name="pause" value="no"/>	
							</td>
						</tr>
					
					</tbody>
				</table>
			
		</fieldset>
		<fieldset id="solverField">
			<legend>solvers</legend>
			<table id="solverList">
				<thead>
					<tr>
						<th>name</th>
						<th>id</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach var="solver" items="${solvers}">
					<tr>
						<td>${solver.name}</td>
						<td>${solver.id}</td>
					</tr>
					
				</c:forEach>
			</tbody>
			</table>
			<button id="useSolver">use selected solver</button>
		</fieldset>
		<fieldset id="actionField">
			<legend>actions</legend>
			<div id="actionBar">
				<button type="submit" class="round" id="btnDone">submit</button>			
				<button type="button" class="round" id="btnBack">cancel</button>	
				<button type="button" class="round" id="btnSave">save profile</button>		
			</div>	
		</fieldset>		
	</form>		
	
</star:template>