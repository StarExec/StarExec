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
		$("#dialog-confirm-txt").text("are you sure you want to rerun the selected pairs?");

		$("#dialog-confirm").dialog({
			modal: true,
			width: 380,
			height: 265,
			buttons: {
				'YES': function() {
					$('#dialog-confirm').dialog('close');
					var statusCode = $('#statusCodeSelect').find(":selected").attr("value");
			                (postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId + "/" + statusCode))();
				},
				'cancel': function() {
					$(this).dialog("close");
				}
			}
		});
	});

	$("#rerunTimelessPairs").click(function() {
		$("#dialog-confirm-txt").text("are you sure you want to rerun the selected pairs with time zero?");

                $("#dialog-confirm").dialog({
                        modal: true,
                        width: 380,
                        height: 265,
                        buttons: {
                                'YES': function() {
                                        $('#dialog-confirm').dialog('close');
			                (postTo(starexecRoot + "services/jobs/rerunpairs/" + jobId))();
                                },
                                'cancel': function() {
                                        $(this).dialog("close");
                                }
                        }
                });
	});

	$("#rerunAllPairs").click(function() {
		$("#dialog-confirm-txt").text("are you sure you want to rerun all pairs?");

                $("#dialog-confirm").dialog({
                        modal: true,
                        width: 380,
                        height: 265,
                        buttons: {
                                'YES': function() {
                                        $('#dialog-confirm').dialog('close');
			                (postTo(starexecRoot + "services/jobs/rerunallpairs/" + jobId))();
                                },
                                'cancel': function() {
                                        $(this).dialog("close");
                                }
                        }
                });
	});
});
