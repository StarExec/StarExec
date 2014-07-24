$(document).ready(function(){
	initButton();
	
	initDataTables();
	
	$('#jobs tbody tr').live('click', function () {
		   $(this).toggleClass( 'row_selected' );
		} );
	
});

function initButton() {
	$('#dialog-confirm-pause').hide();

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
								switch (returnCode) {
									case 0:
										showMessage('success', "all running jobs have been paused", 5000);
										setTimeout(function(){document.location.reload(true);}, 1000);
										break;
									case 1:
										showMessage('error', "jobs were not paused; please try again", 5000);
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
								switch (returnCode) {
									case 0:
										showMessage('success', "all admin paused jobs have been resumed", 5000);
										setTimeout(function(){document.location.reload(true);}, 1000);
										break;
									case 1:
										showMessage('error', "jobs were not resumed; please try again", 5000);
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
			sSource + "jobs/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#userExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
								
				colorizeJobStatistics();

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
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
