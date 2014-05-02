$(document).ready(function(){
	initDataTables();
	
	$("#addUser").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
    });
	
});

function initDataTables() {
	// Setup the DataTable objects
	userTable = $('#users').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "users/pagination",
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

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

function editPermissions(userId) {
	window.location.replace("permissions.jsp?id=" + userId);
}

function suspendUser(userId) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			starexecRoot+"services/suspend/user/" + userId,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 0: 
					showMessage('success',"user(s) suspended successfully",5000);
					setTimeout(function(){document.location.reload(true);}, 1000);
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:
					showMessage('error', "insufficient privileges; you must be an administrator to do that");
					break;
				default:
					showMessage('error', "cannot suspend another administrator");
					break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

function reinstateUser(userId) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			starexecRoot+"services/reinstate/user/" + userId,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 0: 
					showMessage('success',"user(s) suspended successfully",5000);
					setTimeout(function(){document.location.reload(true);}, 1000);
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:
					showMessage('error', "insufficient privileges; you must be an administrator to do that");
					break;
				default:
					showMessage('error', "cannot suspend another administrator");
					break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}
