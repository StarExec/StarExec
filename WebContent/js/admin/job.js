$(document).ready(function(){
	initButton();
	
	initDataTables();
	
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

function initDataTables() {
	// Setup the DataTable objects
	jobTable = $('#jobs').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "jobs/admin/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {

					// Update the number displayed in this DataTable's fieldset
					$('#userExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
								
				colorizeJobStatistics();
				}

			},  
			"json"
	)
}


/**
 * Colorize the job statistics in the jobTable
 */
function colorizeJobStatistics(){
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.asc").heatcolor(
			function() {
				// Return the floating point value of the stat
				var value = $(this).text();
				return eval(value.slice(0, -1));				
			},
			{ 
				maxval: 100,
				minval: 0,
				colorStyle: 'greentored',
				lightness: 0 
			}
	);
	//colorize the unchanging totals
	$("#jobs p.static").heatcolor(
			function() {
				// Return the floating point value of the stat
				return eval(1);				
			},
			{ 
				maxval: 1,
				minval: 0,
				colorStyle: 'greentored',
				lightness: 0 
			}
	);
	// Colorize the statistics in the job table (for pending and error which use reverse color schemes)
	$("#jobs p.desc").heatcolor(
			function() {
				var value = $(this).text();
				return eval(value.slice(0, -1));	
			},
			{ 
				maxval: 100,
				minval: 0,
				colorStyle: 'greentored',
				reverseOrder: true,
				lightness: 0 
			}
	);



}
