var jobId; //the ID of the job being viewed
$(document).ready(function(){
	jobId=$("#jobId").attr("value");
	initUI();
});



function initUI() {
	$(".rerun").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	});
	

	$("#rerunPairs").click(function() {
		statusCode = $('#statusCodeSelect').find(":selected").attr("value");
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId+"/"+statusCode,
				function(returnCode) {
					parseReturnCode(returnCode);
				},
				"json");
	});
	
	$("#rerunTimelessPairs").click(function() {
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId,
				function(returnCode) {
					parseReturnCode(returnCode);

				},
				"json");
	});
	
	$("#rerunAllPairs").click(function() {
		$.post(
				starexecRoot+"services/jobs/rerunallpairs/"+jobId,
				function(returnCode) {
					parseReturnCode(returnCode);
				},
				"json");
	});
}

