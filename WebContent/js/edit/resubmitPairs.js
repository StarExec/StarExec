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
		statusCode = $('#statusCodeSelect').find(":selected").text();
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId+"/"+statusCode,
				function(returnCode) {
					if (returnCode<0) {
						showMessage('error',"There was an rerunning pairs for this job",5000);
					}
				});
	});
	
	$("#rerunTimelessPairs").click(function() {
		$.post(
				starexecRoot+"services/jobs/rerunpairs/"+jobId+,
				function(returnCode) {
					if (returnCode<0) {
						showMessage('error',"There was an rerunning pairs for this job",5000);
					}
				});
	});
}

