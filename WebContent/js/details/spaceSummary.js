var summaryTable;
var pairTable;
var spaceId;
var jobId;
$(document).ready(function(){
	initUI();
	initDataTables();
	spaceId=$("#spaceId").attr("value");
	jobId=$("#jobId").attr("value");
});

/**
 * Initializes the user-interface
 */
function initUI(){
	
	$("#goToParent").button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	$("#goToRoot").button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	
	//we don't need the last rule, as there is no table below the last one
	$("hr").last().remove();
	
	// Set the selected post processor to be the default one
	defaultSolver1 = $('#solverChoice1').attr('default');
	$('#solverChoice1 option[value=' + defaultSolver1 + ']').attr('selected', 'selected');
	
	// Set the selected post processor to be the default one
	defaultSolver2 = $('#solverChoice2').attr('default');
	$('#solverChoice2 option[value=' + defaultSolver2 + ']').attr('selected', 'selected');
	
	//set all fieldsets as expandable
	$('fieldset').expandable(true);
	
	$("#logScale").change(function() {
		logY=false;
		if ($("#logScale").prop("checked")) {
			logY=true;
		}
		
		$.post(
				starexecRoot+"services/jobs/" + jobId + "/" + spaceId+"/graphs/spaceOverview",
				{logY : logY},
				function(returnCode) {
					
					switch (returnCode) {
					
					case 1:
						showMessage('error',"an internal error occured while processing your request: please try again",5000);
						break;
					case 2:
						showMessage('error',"You do not have sufficient permission to view job pair details for this job in this space",5000);
						break;
					default:
						$("#spaceOverview").attr("src",returnCode);
						$("#spaceOverviewLink").attr("src",returnCode+"600");
					}
				},
				"text"
		);
	});
	
	$("#solverChoice1").change(function() {
		updateSolverComparison();
	});
	$("#solverChoice2").change(function() {
		updateSolverComparison();
	});
}

function updateSolverComparison() {
	config1=$("#solverChoice1 option:selected").attr("value");
	
	config2=$("#solverChoice2 option:selected").attr("value");
	$.post(
			starexecRoot+"services/jobs/" + jobId + "/" + spaceId+"/graphs/solverComparison/"+config1+"/"+config2,
			{},
			function(returnCode) {
				
				switch (returnCode) {
				
				case 1:
					showMessage('error',"an internal error occured while processing your request: please try again",5000);
					break;
				case 2:
					showMessage('error',"You do not have sufficient permission to view job pair details for this job in this space",5000);
					break;
				default:
					jsonObject=$.parseJSON(returnCode);
					src=jsonObject.src;
					map=jsonObject.map;
					
					$("#solverComparison").attr("src",src);
					$("#solverComparisonLink").attr("href",src+"600");
					$("#solverComparisonMap").remove();
					$("#graphField").append(map);
				}
			},
			"text"
	);
}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	//summary table
	summaryTable=$('#solveTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
    });
	
	$(".subspaceTable").dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
	});
}


