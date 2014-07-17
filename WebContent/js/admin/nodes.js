var needToConfirm = false;


$(document).ready(function(){

	refreshUpdates();
	
	$( "#dialog-confirm-move" ).hide();

	
	initDataTables();

	
	updateButtonActions();
	

	
	
	  
	window.onbeforeunload = confirmExit;
	function confirmExit() {
		if (needToConfirm) {
			return "You have attempted to leave this page.  If you have made any changes to the fields without clicking the Update button, your changes will be lost.  Are you sure you want to exit this page?";
		}
	}
	
	
});


function refreshUpdates() {
	$.post(  
			starexecRoot+"services/nodes/refresh",
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					break;
				case 2:		
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client
				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}


function updateButtonActions() {
	$('#btnUpdate').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	}).click(function(){
		needToConfirm = false;
		$.post(  
				starexecRoot+"services/nodes/update",
				function(nextDataTablePage){
					switch(nextDataTablePage){
					case 1:
						break;
					case 2:		
						break;
					default:	// Have to use the default case since this process returns JSON objects to the client
						showMessage("success", "Node counts successfully updated" , 3000);
					}
				},  
				"json"
		).error(function(){
			//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
		});
	});
	
	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(){
		
		history.back(-1);
	});
	
	$('#btnDateChange').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	}).click(function(){
		nodeTable.fnDraw();
	});
}

function initDataTables() {
	
	// Setup the DataTable objects
	nodeTable = $('#nodes').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"bFilter"		: false,
		"bInfo"			: false,
		"bPaginate"		: false,
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnDrawCallback": function( oSettings ) {
			var conflictNumber = 0;

			for ( var i=0, iLen=oSettings.aoData.length ; i<iLen ; i++ ) {
				var columnNumber = nodeTable.fnGetData(0).length;
				var conflict = oSettings.aoData[i]._aData[columnNumber-1];
				var colorCSS = 'statusNeutral';
				if(conflict === 'clear') {
					colorCSS = 'statusClear';
				} else if(conflict === 'CONFLICT') {
					colorCSS = 'statusConflict';
					conflictNumber = conflictNumber + 1;
				} else if(conflict === 'ZERO') {
					colorCSS = 'statusZero';
					conflictNumber = conflictNumber + 1;
				}
			oSettings.aoData[i].nTr.className += " "+ colorCSS;
			}
			if (conflictNumber > 0) {
				$('#btnUpdate').hide();
			} else {
				$('#btnUpdate').show();
			}
		},
		"fnServerData"	: fnPaginationHandler
	});
	
	nodeTable.makeEditable({
		"sUpdateURL": starexecRoot + "secure/update/nodeCount",
		"fnStartProcessingMode": function() {
			needToConfirm = true;
			nodeTable.fnDraw();

			setTimeout(function(){nodeTable.fnDraw();}, 1000);

		},
	  });
	
}

function fnPaginationHandler(sSource, aoData, fnCallback) {

	var last_date = document.getElementById("last").value;
	var string_last_date = last_date.replace(/\//g , "");
	
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "nodes/dates/pagination/" + string_last_date,
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:	
					showMessage('error', "not a valid date; please try again", 5000);
					break;
				case 4: 
					showMessage('error', "the date must be greater than provided dates; please try again", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#nodeExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

	
	
	
	