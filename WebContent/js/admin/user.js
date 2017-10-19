jQuery(function($) {
	"use strict";

	var fnPaginationHandler = function(sSource, aoData, fnCallback) {
		// Request the next page of primitives from the server via AJAX
		$.post(
			sSource,
			aoData,
			function(nextDataTablePage) {
				if (parseReturnCode(nextDataTablePage)) {
					// Update the number displayed in this DataTable's fieldset
					$('#userExpd')
					.children('span:first-child')
					.text(nextDataTablePage.iTotalRecords);
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},
			"json"
		)
	};

	// Setup the DataTable objects
	$('#users').dataTable(new window.star.DataTableConfig({
		"sAjaxSource": starexecRoot + "services/users/admin/pagination",
		"bServerSide": true,
		"fnServerData": fnPaginationHandler,
		"columns": [
			null,
			null,
			null,
			{"searchable": false, "orderable": false},
			{"searchable": false, "orderable": false},
			{"searchable": false, "orderable": false},
			{"searchable": false, "orderable": false}
		]
	}));

	$("#addUser").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
	});
});

function editPermissions(userId) {
	window.location.replace("permissions.jsp?id=" + userId);
}

function postAndReloadPageIfSuccessful(postPath) {
	$.post(
		postPath,
		function(returnCode) {
			if (parseReturnCode(returnCode)) {
				setTimeout(function() {document.location.reload(true);}, 1000);
			}
		},
		"json"
	)
}

function suspendUser(userId) {
	postAndReloadPageIfSuccessful(starexecRoot + "services/suspend/user/" + userId);
}

function reinstateUser(userId) {
	postAndReloadPageIfSuccessful(starexecRoot + "services/reinstate/user/" + userId);
}

function subscribeUserToReports(userId) {
	postAndReloadPageIfSuccessful(starexecRoot + "services/subscribe/user/" + userId);
}

function unsubscribeUserFromReports(userId) {
	postAndReloadPageIfSuccessful(starexecRoot + "services/unsubscribe/user/" + userId);
}

function grantDeveloperStatus(userId) {
	log("grantDeveloperStatus clicked.");
	postAndReloadPageIfSuccessful(starexecRoot + "services/grantDeveloperStatus/user/" + userId);
}

function suspendDeveloperStatus(userId) {
	log("suspendDeveloperStatus clicked.");
	postAndReloadPageIfSuccessful(starexecRoot + "services/suspendDeveloperStatus/user/" + userId);
}
