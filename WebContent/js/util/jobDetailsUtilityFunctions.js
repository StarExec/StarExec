$(document).ready(function(){
	$(".stageSelector").first(function() {
		//TODO: Change the number below to a 2. This is jut for testing
		if ($(this).children().size()<=1) {
			hideStageSelectors();
		}
	});
});

function hideStageSelectors() {
	$(".stageSelector").hide();
	$(".stageSelectorLabel").hide();
}
function showStageSelectors() {
	$(".stageSelector").show();
	$(".stageSelectorLabel").show();
}

function setTimeButtonText(){
	if (useWallclock){
		$(".changeTime .ui-button-text").html("use CPU time");
	} else {
		$(".changeTime .ui-button-text").html("use wall time");
	}
}
function setSelectedStage(stage) {
	$(".stageSelector").val(stage);
}

//gets the selected stage. If there is not one, defaults to 0
function getSelectedStage() {
	value = $("#subspaceSummaryStageSelector").val();
	if (!stringExists(value)) {
		return "0";
	}
	
	return value;
}

//Sets the stages dropdown menu with all needed options
function setMaxStagesDropdown(maximum) {
	//TODO: Remove this line, just for testing
	maximum=maximum+1;
	
	
	$('.stageSelector').empty();
	
	$('.stageSelector').append($("<option></option>").attr("value","0").text("Primary")); 
	setInputToValue(".stageSelector","0");
	x=1;
	while (x<=maximum) {
		$('.stageSelector').append($("<option></option>").attr("value",x).text(x)); 
		x=x+1;
	}
	if (maximum==1) {
		hideStageSelectors();
	} else {
		showStageSelectors();
	}
	
}