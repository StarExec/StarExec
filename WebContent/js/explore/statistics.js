var statsTable;
var communityGraphs;
var lastUpdate = null;
var loadingMessage = "Please wait while the information is retrieved…";
var usersMessage = "a user is counted in a community if he/she is a member of at least one of that community's subspaces";
var solversMessage = "a solver is counted as part of a community if it appears in at least one of that community's subspaces, whether or not it appears in a community is based on id so if a solver is copied, it's counted as a new solver because it has a new id";
var benchesMessage = "a benchmark is counted as part of a community if it appears in at least one of that community's subspaces, whether or not it appears in a community is based on id so if a benchmark is copied, it's counted as a new benchmark because it has a new id";
var jobsMessage = "a job is counted as part of a community if it appears in at least one of that community's subspaces";
var jobPairsMessage = "a job pair is counted as part of a community if it appears in at least one of that community's subspaces";
var diskUseMessage = "disk use = space used by solvers + space used by benchmarks, where solvers and benchmarks belong to the given community";

// When the document is ready to be executed on
jQuery(function($) {
	statsTable = $('#statsTable').dataTable(new star.DataTableConfig());

	// Make leaders and members expandable
	$('.expd').parent().expandable(true)

	;$('#benchHeader').qtip({
		content: benchesMessage,
		show: "mouseover",
		hide: "mouseout"
	});

	$('.compareBtn')
		.button({
			icons : {
				secondary : "ui-icon-refresh"
			}
		})
	;

	$('#compareBenches')
		.qtip({
			content: benchesMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function(){
			changeCommunityOverviewGraph('benchmarks');
		})
	;

	$('#compareDiskUse')
		.qtip({
			content: diskUseMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function(){
			changeCommunityOverviewGraph('disk_usage');
		})
	;

	$('#compareJobs')
		.qtip({
			content: jobsMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function(){
			changeCommunityOverviewGraph('jobs');
		})
	;

	$('#compareJobPairs')
		.qtip({
			content: jobPairsMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function(){
			changeCommunityOverviewGraph('job_pairs');
		})
	;

	$('#compareSolvers')
		.qtip({
			content: solversMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function() {
			changeCommunityOverviewGraph('solvers');
		})
	;

	$('#compareUsers')
		.qtip({
			content: usersMessage,
			show: "mouseover",
			hide: "mouseout"
		})
		.click(function() {
			changeCommunityOverviewGraph('users');
		})
	;

	$('#diskUseHeader')
		.qtip({
			content: diskUseMessage,
			show: "mouseover",
			hide: "mouseout"
		})
	;

	$('#solverHeader')
		.qtip({
			content: solversMessage,
			show: "mouseover",
			hide: "mouseout"
		})
	;

	$('#jobHeader')
		.qtip({
			content: jobsMessage,
			show: "mouseover",
			hide: "mouseout"
		})
	;

	$('#jobPairHeader')
		.qtip({
			content: jobPairsMessage,
			show: "mouseover",
			hide: "mouseout"
		})
	;

	$('#userHeader')
		.qtip({
			content: usersMessage,
			show: "mouseover",
			hide: "mouseout"
		})
	;

	$("#lastUpdate").text(loadingMessage);
	updateCommunityOverview();
});

function updateCommunityStatsTable(info){
	statsTable.fnClearTable();

	$.each(info, function(key, value) {
		var name = key;
		var users = value.users;
		var solvers = value.solvers;
		var benchmarks = value.benchmarks;
		var jobs = value.jobs;
		var job_pairs = value.job_pairs;
		var disk_usage = value.disk_usage;
		statsTable.fnAddData([name,users,solvers,benchmarks,jobs,job_pairs,disk_usage]);
	});
}
/**
 * refreshes graph in community page
 */
function updateCommunityOverview() {
	$.post(
		starexecRoot+"services/secure/explore/community/overview",
		function(returnCode) {
			switch (returnCode) {
			case "1":
				showMessage('error',"an internal error occured while processing your request: please try again",5000);
				$("#communityOverview").attr("src",starexecRoot+"/images/noDisplayGraph.png");
				$("#graph").hide();
				break;
			default:
				var jsonObject = $.parseJSON(returnCode);
				communityGraphs = jsonObject.graphs;

				updateCommunityStatsTable(jsonObject.info);
				changeCommunityOverviewGraph("users")

				$("#lastUpdate").text("Last Updated: " + jsonObject.date);
				$("#options").show();
				$("#graph").show();
				$("#statsTableField").show();
			}
		},
		"text"
	).fail(function() {
		showMessage('error',"an internal error occured while processing your request: please try again",5000);
		$("#lastUpdate").text("There was a problem loading data. Please try reloading the page.");
	});
}

function changeCommunityOverviewGraph(type){
	$("#communityOverview")
		.show()
		.attr("src", communityGraphs[type])
	;
}
