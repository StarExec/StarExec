$(document).ready(function(){
	initButton();

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
	var config = new star.DataTableConfig({
		"sAjaxSource"   : starexecRoot+"services/jobs/admin/pagination",
		"aoColumns"     : [
			{"mRender"  : formatName },
			{"mRender"  : formatStatus },
			{"mRender"  : formatUser },
			{"mRender"  : formatQueue },
		]
	});
	$("#jobs").dataTable(config);

	$('#jobs tbody').on('click', "tr", function () {
		   $(this).toggleClass( 'row_selected' );
		} );

});

function initButton() {
	$("#pauseAll").button({
		icons: {
			primary: "ui-icon-pause"
		}
	});

	$("#resumeAll").button({
		icons: {
			primary: "ui-icon-play"
		}
	});

	$("#pauseAll").click(function() {
		$('#dialog-confirm-pause-txt').text('are you sure you want to pause all running jobs?');

		$('#dialog-confirm-pause').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed to pause all running jobs');
					$('#dialog-confirm-pause').dialog('close');
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
					log('user canceled pause all running jobs');
					$(this).dialog("close");
				}
			}
		});
	});

	$("#resumeAll").click(function() {
		$('#dialog-confirm-pause-txt').text('are you sure you want to resume all admin paused jobs?');

		$('#dialog-confirm-pause').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed to resume all admin paused jobs');
					$('#dialog-confirm-pause').dialog('close');
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
					log('user canceled resume all admin paused jobs');
					$(this).dialog("close");
				}
			}
		});
	});
}
