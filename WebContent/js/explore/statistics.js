
var statsTable;

var communityInfo;
var communityGraphs;
var lastUpdate = null;
var loadingMessage = "Please wait while the information is retrieved... ";

var usersMessage = "a user is counted in a community if he/she is a member of at least one of that community's subspaces";
var solversMessage = "a solver is counted as part of a community if it appears in at least one of that community's subspaces, whether or not it appears in a community is based on id so if a solver is copied, it's counted as a new solver because it has a new id";
var benchesMessage = "a benchmark is counted as part of a community if it appears in at least one of that community's subspaces, whether or not it appears in a community is based on id so if a benchmark is copied, it's counted as a new benchmark because it has a new id";
var jobsMessage = "a job is counted as part of a community if it appears in at least one of that community's subspaces";
var jobPairsMessage = "a job pair is counted as part of a community if it appears in at least one of that community's subspaces";
var diskUseMessage = "disk use = space used by solvers + space used by benchmarks, where solvers and benchmarks belong to the given community";

// When the document is ready to be executed on
$(document).ready(function(){

	initButtonUI();
	initTableHeaderUI();

	statsTable = $('#statsTable').dataTable(new star.DataTableConfig());


	// Make leaders and members expandable
	$('.expd').parent().expandable(true);



       	$("#compareUsers").click(function(){
		changeCommunityOverviewGraph('users');
	    });

	$("#compareSolvers").click(function(){
		changeCommunityOverviewGraph('solvers');
	    });

	$("#compareBenches").click(function(){
		changeCommunityOverviewGraph('benchmarks');
	    });

	$("#compareJobs").click(function(){
		changeCommunityOverviewGraph('jobs');
	    });

	$("#compareJobPairs").click(function(){
		changeCommunityOverviewGraph('job_pairs');
	    });

	$("#compareDiskUse").click(function(){
		changeCommunityOverviewGraph('disk_usage');
	    });

	$("#lastUpdate").text(loadingMessage);

	updateCommunityOverview();

});

function initTableHeaderUI(){
    $('#userHeader').qtip({
	    content: usersMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#solverHeader').qtip({
	    content: solversMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#benchHeader').qtip({
	    content: benchesMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#jobHeader').qtip({
	    content: jobsMessage,
		show: "mouseover",
		hide: "mouseout"
		});


    $('#jobPairHeader').qtip({
	    content: jobPairsMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#diskUseHeader').qtip({
	    content: diskUseMessage,
		show: "mouseover",
		hide: "mouseout"
		});
}
function initButtonUI(){
    $('.compareBtn').button({
	    icons : {
		secondary : "ui-icon-refresh"
	    }});

    $('#compareUsers').qtip({
	    content: usersMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#compareSolvers').qtip({
	    content: solversMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#compareBenches').qtip({
	    content: benchesMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#compareJobs').qtip({
	    content: jobsMessage,
		show: "mouseover",
		hide: "mouseout"
		});


    $('#compareJobPairs').qtip({
	    content: jobPairsMessage,
		show: "mouseover",
		hide: "mouseout"
		});

    $('#compareDiskUse').qtip({
	    content: diskUseMessage,
		show: "mouseover",
		hide: "mouseout"
		});
}

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

		console.log("users: " + users);

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
				    jsonObject=$.parseJSON(returnCode);

				    console.log("about to process graphs and info");

				    lastUpdate = jsonObject.date;
				    $("#lastUpdate").text("Last Updated: " + lastUpdate);

				    communityGraphs=jsonObject.graphs;

				    console.log(communityGraphs);

				    communityInfo=jsonObject.info;

				    console.log(communityInfo);

				    updateCommunityStatsTable(communityInfo);

				    $("#communityOverview").attr("src",communityGraphs.users);
				    $("#communityOverview").show();

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
    $("#communityOverview").attr("src",communityGraphs[type]);
    $("#communityOverview").show();
}
