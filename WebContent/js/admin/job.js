"use strict";

jQuery(function($) {
	var formatName = function(row, type, val) {
		return val["name"];
	};

	var formatStatus = function(row, type, val) {
		return val["status"];
	};

	var formatUser = function(row, type, val) {
		return val["user"]["name"];
	};

	var formatQueue = function(row, type, val) {
		return val["queue"]["name"];
	};

	// Setup the DataTable objects
	$("#jobs").dataTable(
		new star.DataTableConfig({
			"sAjaxSource"   : starexecRoot+"services/jobs/admin/pagination",
			"aoColumns"     : [
				{"mRender"  : formatName },
				{"mRender"  : formatUser },
				{"mRender"  : formatStatus },
				{"mRender"  : formatQueue },
			]
		})
	);

	$("#jobs tbody").on("click", "tr", function() {
//		.click(function() {
			$(this).toggleClass("row_selected");
		})
	;

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
									s=parseReturnCode(returnCode);
									if (s) {
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
									s=parseReturnCode(returnCode);
									if (s) {
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
