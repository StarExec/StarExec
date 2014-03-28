//This is for Approving/Declining a queue request

var progress = 0;


$(document).ready(function() {
	
	refreshUpdates();
	$('#dialog-warning').hide();



	
	InitUI();
	
	$('#btnDone').button({
		icons: {
			secondary: "ui-icon-circle-check"
		}
	});
	
	initDataTables();
	
	
	var queueName = document.getElementById("queueName").value;

	document.getElementById('qName').innerHTML = queueName;
	
	var start_date = document.getElementById("start").value;
	dateComponents = start_date.split("/");
	start_date = new Date(dateComponents[2], dateComponents[0] - 1, dateComponents[1]);
	var today = new Date();
	today.setHours(0, 0, 0, 0);
	if (start_date < today) {
		$('#dialog-warning-txt').text('WARNING: This request has expired. Please adjust dates accordingly.');
		
		$('#dialog-warning').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-warning').dialog('close');
				}
			}
		});
		$('#btnDone').hide();
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

function InitUI() {

	
	$('#btnDecline').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	}).click(function(){
		$.post(
				starexecRoot+"services/cancel/request/" + getParameterByName("code"),
				function(returnCode) {
					switch (returnCode) {
						case 0:
							history.back(-1);
							//showMessage('success', "queue request was successfully declined", 3000);
							break;
						case 1:
							showMessage('error', "queue request was not declined; please try again", 5000);
							break;
						case 2:
							showMessage('error', "only the admin can decline a queue request", 5000);
							break;
						default:
							showMessage('error', "invalid parameters", 5000);
							break;
					}
				},
				"json"
		);
	});
	
    
	$('#btnUpdate').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	}).click(function(){
		var code = window.location.search.split('code=')[1]; //Check to see if flag was set
		var queueName = document.getElementById("queueName").value;
		var nodeCount = document.getElementById("nodeCount").value;
		var start_date = document.getElementById("start").value;
		var string_start_date = start_date.replace(/\//g , "");
		var end_date = document.getElementById("end").value;
		var string_end_date = end_date.replace(/\//g, "");

	
		$.post(  
				starexecRoot+"services/edit/request/" + code + "/" + queueName + "/" + nodeCount + "/" + string_start_date + "/" + string_end_date ,
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		nodeTable.fnDraw();
			    		showMessage('success', "successfully updated values", 3000);
			    		document.getElementById('qName').innerHTML = queueName;
			    	} else if (returnCode == '2') {
			    		showMessage('error', "invalid permissions", 5000);
			    	} else if (returnCode == '4') {
			    		showMessage('error', "date must be after today's date", 5000);
			    	} else if (returnCode == '5') {
			    		showMessage('error', "end date must be after start date", 5000);
			    	} else if (returnCode == '6') {
			    		showMessage('error', "The requested queue name is already in use. Please select another", 5000);
			    	} else {
			    		showMessage('error', "There was an error processing your updates; please try again", 5000);
			    	}
			     },  
			     "json"  
		).error(function(){
			showMessage('error',"Internal error updating user information",5000);
		});	
	});
   
	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(){
		
		history.back(-1);
	});

}

function initDataTables() {
	
	// Setup the DataTable objects
	nodeTable = $('#nodes').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"bFilter"		: false,
		"bInfo"			: false,
		"bPaginate"		: false,
		//"bAutoWidth"	: false,
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
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
				$('#btnDone').hide();
			} else {
				$('#btnDone').show();
			}
		},
		"fnServerData"	: fnPaginationHandler
	});
	nodeTable.makeEditable({
		"sUpdateURL": starexecRoot + "secure/update/nodeCount",
		"fnStartProcessingMode": function() {
			nodeTable.fnDraw();
			nodeTable.fnDraw();
			nodeTable.fnDraw();
		}
	  });

	
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	var code = window.location.search.split('code=')[1]; //Check to see if flag was set
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "nodes/dates/reservation/" + code + "/pagination",
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



