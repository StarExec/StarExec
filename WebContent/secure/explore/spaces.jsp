<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.*,org.starexec.data.security.*,org.starexec.util.*, org.starexec.data.to.*, org.starexec.data.database.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	int uid=SessionUtil.getUserId(request);

	try {
		int spaceId=Integer.parseInt(request.getParameter("id"));
		if (SpaceSecurity.canUserSeeSpace(spaceId,uid).isSuccess() && spaceId > 0) {
			List<Integer> idChain=Spaces.getChainToRoot(spaceId);
			StringBuilder stringChain=new StringBuilder();
			for (Integer id : idChain) {
				stringChain.append(id);
				stringChain.append(",");
			}
			stringChain.delete(stringChain.length()-1,stringChain.length());
			request.setAttribute("spaceChain",stringChain.toString());
		} else {
			request.setAttribute("spaceChain","1");

		}

	} catch (Exception e) {
		// we don't need the id, so we can just ignore errors here. It may not exist
	}

	request.setAttribute("userId",uid);
	request.setAttribute("isAdmin",Users.isAdmin(uid));


%>
<star:template title="Space Explorer" js="util/draggable, util/spaceTree, util/sortButtons, common/delaySpinner, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, explore/spaces, util/datatablesUtility, lib/jquery.heatcolor.0.0.1.min, lib/jquery.ba-throttle-debounce.min, shared/sharedFunctions" css="common/delaySpinner, common/table, explore/common, explore/jquery.qtip, explore/spaces">
	<span id="userId" value="${userId}" ></span>
	<span id="spaceChain" value="${spaceChain}"></span>
	<div id="explorer">
		<h3>Spaces</h3>

		<ul id="exploreList">
		</ul>
	</div>

	<div id="detailPanel">
		<h3 class="spaceName"></h3>

		<star:userLoggedIn>
			<a id="trashcan" class="active"></a>
		</star:userLoggedIn>

		<p id="spaceDesc" class="accent"></p>
		<p id="spaceID" class="accent"></p>
		<fieldset id="jobField">
			<legend class="expd" id="jobExpd"><span>0</span> jobs</legend>
			<ul class="actionList">
				<li><a class="btnRun" id="addJob" href="${starexecRoot}/secure/add/job.jsp">create job</a></li>
				<li><a class="btnRun" id="addQuickJob" href="${starexecRoot}/secure/add/quickJob.jsp">quick job</a></li>
				<li><a class="btnUp" id="uploadJobXML" href="${starexecRoot}/secure/add/batchJob.jsp">upload job xml</a></li>
			</ul>
			<table id="jobs">
				<thead>
					<tr>
						<th id="jobNameHead">name</th>
						<th id="jobStatusHead">status</th>
						<th id="jobCompletedHead"><span title="Job pairs that ran successfully">completed</span></th>
						<th id="jobTotalHead"><span title="The total number of job pairs in this job">total</span></th>
						<th id="jobFailedHead"><span title="Job pairs for which there was a timeout, mem-out, or internal error">failed</span></th>
						<th id="jobTimeHead">time</th>
					</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllJobs">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllJobs">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>

		<fieldset id="solverField">
			<legend class="expd" id="solverExpd"><span>0</span> solvers</legend>
			<ul class="actionList">
				<li><a class="btnUp" id="uploadSolver" href="${starexecRoot}/secure/add/solver.jsp">upload solver</a></li>
			</ul>
			<table id="solvers">
				<thead>
					<tr>
						<th id="solverNameHead">name</th>
						<th id="solverDescHead">description</th>
						<th id="solverTypeHead">Type</th>
					</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllSolvers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllSolvers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>

		<fieldset id="benchField">
			<legend class="expd" id="benchExpd"><span>0</span> benchmarks</legend>
			<ul class="actionList">
				<li><button title="sorts benchmarks in the order they were added to this space" asc="true" class="sortButton" id="additionSort" value="2">sort by addition order</button></li>
				<li><a class="btnUp" id="uploadBench" href="${starexecRoot}/secure/add/benchmarks.jsp">upload benchmarks</a></li>
				<li><a class="btnRun" id="processBenchmarks" href="${starexecRoot}/edit/processBenchmarks.jsp">process benchmarks</a></li>
			</ul>
			<table id="benchmarks">
				<thead>
					<tr>
						<th id="benchNameHead">name</th>
						<th id="benchTypeHead">type</th>
					</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllBenchmarks">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllBenchmarks">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>

		<fieldset  id="userField">
			<legend class="expd" id="userExpd"><span>0</span> users</legend>
			<table id="users">
				<thead>
					<tr>
						<th id="userNameHead">name</th>
						<th id="userInstitutionHead">institution</th>
						<th id="userEmailHead">email</th>
					</tr>
				</thead>
			</table>
			<div class="selectWrap">
				<p class="selectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>All
				</p> |
				<p class="unselectAllUsers">
					<span class="ui-icon ui-icon-circlesmall-plus"></span>None
			</div>
		</fieldset>

		<fieldset id="spaceField">
			<legend class="expd" id="spaceExpd"><span>0</span> subspaces</legend>
			<ul class="actionList">
				<li><a class="btnAdd" id="addSpace" href="${starexecRoot}/secure/add/space.jsp">add subspace</a></li>
				<li><a class="btnUp" id="uploadXML" href="${starexecRoot}/secure/add/batchSpace.jsp">upload space xml</a></li>
			</ul>
			<table id="spaces">
				<thead>
					<tr>
						<th id="spaceNameHead">name</th>
						<th id="spaceDescriptionHead">description</th>
					</tr>
				</thead>
			</table>
		</fieldset>

		<fieldset class="actions">
			<legend>space actions</legend>
			<ul class="actionList">
				<li><a class="btnEdit" id="editSpace" href="${starexecRoot}/secure/edit/space.jsp">edit space</a></li>
				<li><a class="btnEdit" id="editSpacePermissions" href="${starexecRoot}/secure/edit/spacePermissions.jsp">edit space permissions</a></li>
				<li><a class="btnDown" id="downloadXML" >download space xml</a></li>
				<li><a class="btnDown" id="downloadSpace">download space</a></li>
			</ul>
			<p style="color:#008c03;clear:both">Some action buttons have moved.<br />They are now higher on this page, grouped with the primitives they act on.</p>
		</fieldset>

	</div>
	<br class=".clear" />

	<div id="dialog-confirm-copy" title="confirm copy" class="hiddenDialog">
        <p><span class="ui-icon ui-icon-info"></span><span id="dialog-confirm-copy-txt"></span></p>
        <div id="hier-copy-options" class="copy-options-hidden">
            <input type="radio" value="false" name="copySpace" checked />copy space
            <input type="radio" value="true" name="copySpace" />copy hierarchy
        </div>
        <br class=".clear" />
        <div id="copy-primitives-options" class="copy-options-hidden">
            <span class="ui-icon ui-icon-info"></span>
            <span>would you like to copy or link primitives to new space?<span>
            <div>
                <input type="radio" value="false" name="copyPrimitives" checked />link primitives
                <input type="radio" value="true" name="copyPrimitives" />copy primitives
            </div>
        </div>
	</div>
	<div id="dialog-confirm-delete" title="confirm delete" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-spacexml" title="xml properties" class="hiddenDialog">
	  <p><span id="dialog-spacexml-attributes-txt"></span></p>
	  <input type="radio" value="true" name="att" />Yes
	  <input type="radio" value="false" name="att" checked />No
	  <div style='position: absolute; left: 0; width: 100%; bottom: 0;'>
	  <p><span ><a id="showUpdateDialog" href="#" style="color:
							     rgb(0,0,255)">Download
	  with updates (advanced)</a></span></p>
	  </div>
	</div>
	<div id="dialog-spaceUpdateXml" title="Updates" class="hiddenDialog">
	  <p><span id="dialog-spacexml-updates-txt"></span></p>
	  <div><input  type="text" id="updateID" /></div>
	</div>
	<div id="dialog-download-space" title="download space" class="hiddenDialog">
		<div id="downloadHierarchyOptionContainer">
			<p><span class="ui-icon ui-icon-alert"></span>do you want to download the single space or the hierarchy?</p><br>
			<input type="radio" name="downloadHierarchyOption" id="downloadSingleSpace" checked="checked"/>space<br>
			<input type="radio" name="downloadHierarchyOption" id="downloadSpaceHierarchy"/>hierarchy</p>
			<hr>
		</div>
		<p><span class="ui-icon ui-icon-alert"></span>do you want to download the benchmarks and/or the solvers?</p><br>
		<p><input type="radio" name="downloadOption" id="downloadSolvers"/> solvers only<br>
		<input type="radio" name="downloadOption" id="downloadBenchmarks"/> benchmarks only<br>
		<input type="radio" name="downloadOption" id="downloadBoth" checked="checked"/> solvers + benchmarks<br></p>
		<hr>
		<p><span class="ui-icon ui-icon-alert"></span>do you want to store benchmarks/solvers in id directories?</p><br>
		<input type="radio" name="idDirectoriesOption" id="yesIdDirectories"/> yes<br>
		<input type="radio" name="idDirectoriesOption" id="noIdDirectories" checked="checked"/> no<br>
	</div>
	<div id="dialog-warning" title="warning" class="hiddenDialog">
		<p><span class="ui-icon ui-icon-alert" ></span><span id="dialog-warning-txt"></span></p>
	</div>



</star:template>
