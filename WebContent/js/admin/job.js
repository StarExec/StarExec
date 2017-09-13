"use strict";

jQuery(function($) {
	var formatJob = function(row, type, val) {
		return star.format.jobLink(val);
	};

	var formatUser = function(row, type, val) {
		return star.format.userLink(val["user"]);
	};

	var formatStatus = function(row, type, val) {
		return val["status"];
	};

	var formatQueue = function(row, type, val) {
		if (val["queue"] === undefined) {
			return "NULL";
		} else {
			return val["queue"]["name"];
		}
	};

	var formatComplete = function(row, type, val) {
		return star.format.heatcolor((val["totalPairs"] - val["pendingPairs"]) * 100 / val["totalPairs"]);
	};

	var formatCreated = function(row, type, val) {
		return star.format.timestamp(val["created"]);
	};

	// Setup the DataTable objects
	$("#jobs").dataTable(
		new star.DataTableConfig({
			"sAjaxSource"   : starexecRoot+"services/jobs/admin/pagination",
			"sServerMethod" : "GET",
			"order"         : [[5, "desc"]],
			"aoColumns"     : [
				{"mRender"  : formatJob,
				 "title"    : "Job"},
				{"mRender"  : formatUser,
				 "title"    : "User"},
				{"mRender"  : formatQueue,
				 "title"    : "Queue"},
				{"mRender"  : formatComplete,
				 "width"    : "95px",
				 "title"    : "Complete"},
				{"mRender"  : formatStatus,
				 "width"    : "115px",
				 "title"    : "Status"},
				{"mRender"  : formatCreated,
				 "width"    : "140px",
				 "title"    : "Created"},
			]
		})
	);

	$("#pauseAll")
		.button({
			icons: {
				primary: "ui-icon-pause"
			}
		})
		.click(function() {
			star.openDialog({
				title: "Confirm Pause",
				buttons: {
					"OK": function() {
						$("#dialog-confirm-pause").dialog("close");
						$.post(
								starexecRoot+"services/admin/pauseAll/",
								function(returnCode) {
									if (parseReturnCode(returnCode)) {
										setTimeout(function(){document.location.reload(true);}, 1000);
									}
								},
								"json"
						);
					},
					"cancel": function() {
						$(this).dialog("close");
					}
				}
			}, "Are you sure you want to pause all running jobs?");
		})
	;

	$("#resumeAll")
		.button({
			icons: {
				primary: "ui-icon-play"
			}
		})
		.click(function() {
			star.openDialog({
				title: "Confirm Resume",
				buttons: {
					"OK": function() {
						$("#dialog-confirm-pause").dialog("close");
						$.post(
								starexecRoot+"services/admin/resumeAll/",
								function(returnCode) {
									if (parseReturnCode(returnCode)) {
										setTimeout(function(){document.location.reload(true);}, 1000);
									}
								},
								"json"
						);
					},
					"cancel": function() {
						$(this).dialog("close");
					}
				}
			}, "Are you sure you want to resume all admin paused jobs?");
		})
	;
});
