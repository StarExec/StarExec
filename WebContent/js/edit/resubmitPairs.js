"use strict";

jQuery(function($) {
	var jobId = $("#jobId").attr("value"); //the ID of the job being viewed

	$(".rerun").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	});

	var postTo = function(url) {
		return (function() {
			$.post(url, parseReturnCode, "json");
		});
	};

	$("#rerunPairs").click(function() {
		if(confirm("are you sure you want to rerun the selected pairs?")) {
			var statusCode = $('#statusCodeSelect').find(":selected").attr("value");
			(postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId + "/" + statusCode))();
		}
	});

	$("#rerunTimelessPairs").click(function() {
		if(confirm("are you sure you want to rerun selected pairs with a time of 0?")){
			(postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId))();
		}
	});

	$("#rerunAllPairs").click(function() {
		if(confirm("are you sure you ant to rerun all pairs?")) {
			(postTo(starexecRoot + "services/jobs/rerunallpairs/" + jobId))();
		}
	});
});
