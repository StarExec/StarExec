var needToConfirm = false;


$(document).ready(function(){

	
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



function updateButtonActions() {
	$('#btnUpdate').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	})
	$("#btnUpdate").click(function(){
		needToConfirm = false;
		$.post(  
				starexecRoot+"services/nodes/update",
				function(nextDataTablePage){
					s=parseReturnCode(nextDataTablePage);
					if (s) {
						showMessage("success", "Node counts successfully updated" , 3000);
					}

				},  
				"json"
		)
	});
	
	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}})
	$("#btnBack").click(function(){
		
		history.back(-1);
	});
	
	$('#btnDateChange').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	})
	$("#btnDateChange").click(function(){
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

		}
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
				s=parseReturnCode(nextDataTablePage);
				if (s) {

					// Update the number displayed in this DataTable's fieldset
					$('#nodeExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},  
			"json"
	)
}

	
	
	
	