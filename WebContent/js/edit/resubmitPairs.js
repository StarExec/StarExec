var jobId; //the ID of the job being viewed
$(document).ready(function(){
	jobId=$("#jobId").attr("value");
	initUI();
});



function initUI() {
	$("#rerunPairs").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	});
	
	$("#rerunTimelessPairs").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	});
	
	$("#rerunPairs").click(function() {
		statusCode = $('#statusCodeSelect').find(":selected").attr("value");
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId+"/"+statusCode,
				function(returnCode) {
					if (returnCode==0) {
						showMessage('success',"Pairs were successfully submitted to be rerun",5000);
					} else {
						showMessage('error',"There was an error rerunning pairs for this job",5000);
					}
				},
				"json");
	});
	
	$("#rerunTimelessPairs").click(function() {
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId,
				function(returnCode) {
					if (returnCode==0) {
						showMessage('success',"Pairs were successfully submitted to be rerun",5000);
					} else {
						showMessage('error',"There was an error rerunning pairs for this job",5000);
					}
				},
				"json");
	});
}

