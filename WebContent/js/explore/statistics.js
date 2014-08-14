
var statsTable;

var communityInfo;
var communityGraphs;
var lastUpdate = null;
var loadingMessage = "Please wait while we count stars.  This may take a while..."

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
