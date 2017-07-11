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

	var formatCreated = function(row, type, val) {
		return star.format.timestamp(val["created"]);
	};

	// Setup the DataTable objects
	$("#jobs").dataTable(
		new star.DataTableConfig({
			"sAjaxSource"   : starexecRoot+"services/jobs/admin/pagination",
			"sServerMethod" : "GET",
			"order"         : [[4, "desc"]],
			"aoColumns"     : [
				{"mRender"  : formatJob },
				{"mRender"  : formatUser },
				{"mRender"  : formatStatus },
				{"mRender"  : formatQueue },
				{"mRender"  : formatCreated },
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
			$("#dialog-confirm-pause-txt").text("are you sure you want to pause all running jobs?");

			$("#dialog-confirm-pause").dialog({
				modal: true,
				width: 380,
				height: 165,
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
			});
		})
	;

	$("#resumeAll")
		.button({
			icons: {
				primary: "ui-icon-play"
			}
		})
		.click(function() {
			$("#dialog-confirm-pause-txt").text("are you sure you want to resume all admin paused jobs?");

			$("#dialog-confirm-pause").dialog({
				modal: true,
				width: 380,
				height: 165,
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
			});
		})
	;
});
