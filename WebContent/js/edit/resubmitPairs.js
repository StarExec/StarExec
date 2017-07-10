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
		var statusCode = $('#statusCodeSelect').find(":selected").attr("value");
		(postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId + "/" + statusCode))();
	});

	$("#rerunTimelessPairs").click(
		postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId)
	);

	$("#rerunAllPairs").click(
		postTo(starexecRoot + "services/jobs/rerunallpairs/" + jobId)
	);
});
