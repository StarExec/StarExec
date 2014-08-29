
var statsTable;

var communityInfo;
var communityGraphs;
var lastUpdate = null;
var loadingMessage = "Please wait while the information is retrieved... ";

var usersMessage = "users are counted in a community if they are a member of at least one of that community's subspaces";
var solversMessage = "solvers are counted in a community if they are a uniquely identifiable primitive of at least one of that communtiy's subspaces";
var benchesMessage = "benchmarks are counted in a community if they are a uniquely identifiable primitive of at least one of that community's subspaces;";
var jobsMessage = "jobs are counted in a community if they are a uniquely identifiable primitive of at least one of that community's subspaces";
var jobPairsMessage = "job pairs are counted in a community if they are a uniquely identifiable primitive of at least one of that communtiy's subspaces";
var diskUseMessage = "disk use = space used by solvers + space used by benchmarks, where solvers and benchmarks are uniquely identifiable primitives of at least one of that community's subspaces";

// When the document is ready to be executed on
$(document).ready(function(){

	initButtonUI();

	statsTable = $('#statsTable').dataTable({
		"sDom": 'rt<"bottom"flpi><"clear">'
	    });
	
	
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
	);
}

function changeCommunityOverviewGraph(type){
    $("#communityOverview").attr("src",communityGraphs[type]);
    $("#communityOverview").show();
}
