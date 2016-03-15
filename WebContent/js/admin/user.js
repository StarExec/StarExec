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
			sSource + "users/admin/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {

					// Update the number displayed in this DataTable's fieldset
					$('#userExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				}

			},  
			"json"
	)
}

function editPermissions(userId) {
	window.location.replace("permissions.jsp?id=" + userId);
}

function postAndReloadPageIfSuccessful(postPath) {
	$.post(  
		postPath,
		function(returnCode){
			s=parseReturnCode(returnCode);
			if (s) {
				setTimeout(function(){document.location.reload(true);}, 1000);
			}
		},  
		"json"
	)
}

function suspendUser(userId) {
	postAndReloadPageIfSuccessful(starexecRoot+"services/suspend/user/"+userId);
}

function reinstateUser(userId) {
	postAndReloadPageIfSuccessful(starexecRoot+"services/reinstate/user/"+userId);
}

function subscribeUserToReports(userId) {
	postAndReloadPageIfSuccessful(starexecRoot+"services/subscribe/user/"+userId);
}

function unsubscribeUserFromReports(userId) {
	postAndReloadPageIfSuccessful(starexecRoot+"services/unsubscribe/user/"+userId);
}

function grantDeveloperStatus(userId) {
	log("grantDeveloperStatus clicked.");
	postAndReloadPageIfSuccessful(starexecRoot+"services/grantDeveloperStatus/user/"+userId);
}

function suspendDeveloperStatus(userId) {
	log("suspendDeveloperStatus clicked.");
	postAndReloadPageIfSuccessful(starexecRoot+"services/suspendDeveloperStatus/user/"+userId);
}
