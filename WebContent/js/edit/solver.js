$(document).ready(function() {
	initUI();
	attachFormValidation();
	attachWebsiteMonitor();
	attachButtonActions();
});

/**
 * Initializes the user-interface
 */
function initUI() {
	// Setup which fields are expanded and which are not
	$('fieldset:first').expandable(false);
	$('fieldset:not(:first)').expandable(true);

	$("#dialog-confirm-delete").css("display", "none");
	// Setup JQuery button icons
	$('#delete').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
		}
	});

	// Setup '+ add new' animation
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');
		togglePlusMinus(this);
	});
	$('#new_website').hide();
}

/**
 * Attaches form validation to the 'edit solver' fields
 */
function attachFormValidation() {
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editSolverForm").submit(function(e) {
		e.preventDefault();
	});

	// Adds regular expression handling to JQuery's validator
	addValidators();

	// Form validation rules/messages
	$("#editSolverForm").validate({
		rules: {
			name: {
				required: true,
				maxlength: $("#name").attr("length"),
				jspregex: getPrimNameRegex()
			},
			description: {
				required: false,
				maxlength: $("#description").attr("length"),
				regex: getPrimDescRegex()
			}
		},
		messages: {
			name: {
				required: "name required",
				maxlength: $("#name").attr("length") + " characters maximum",
				jspregex: "invalid character(s)"
			},
			description: {
				required: "description required",
				maxlength: $("#description")
				.attr("length") + " characters maximum",
				regex: "invalid character(s)"
			}
		}
	});
}

/**
 * Attaches actions to the 'update', 'delete', 'add new website' buttons
 */
function attachButtonActions() {
	// Prompts user to confirm deletion and, if they confirm,
	// deletes the solver via AJAX, then redirects to explore/spaces.jsp
	$("#delete").click(function() {
		$('#dialog-confirm-delete-txt')
		.text('Are you sure you want to move this solver to your trash bin?');

		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed solver deletion.');
					$('#dialog-confirm-delete').dialog('close');

					$.post(
						starexecRoot + "services/recycle/solver",
						{selectedIds: [getParameterByName("id")]},
						function(returnCode) {
							s = parseReturnCode(returnCode);
							if (s) {
								window.location = starexecRoot + 'secure/explore/spaces.jsp';
							}
						},
						"json"
					);
				},
				"cancel": function() {
					log('user canceled solver deletion');
					$(this).dialog("close");
				}
			}
		});
	});

	// Triggers validation and, if that passes,
	// updates the solver details via AJAX, then redirects to edit/solver.jsp
	$("#update").click(function() {
		var isFormValid = $("#editSolverForm").valid();
		if (isFormValid == true) {
			//Extract Relevant Data from Page
			var data =
				{
					name: $("#name").val(),
					description: $("#description").val(),
					downloadable: $("#downloadable").is(':checked')
				};

			//Pass data to server via AJAX
			$.post(
				starexecRoot + "services/edit/solver/" + getParameterByName("id"),
				data,
				function(returnCode) {
					s = parseReturnCode(returnCode);
					if (s) {
						window.location = starexecRoot + 'secure/details/solver.jsp?id=' + getParameterByName(
							"id");

					}

				},
				"json"
			);
		}
	});

}

/**
 * Toggles the plus-minus text of the "+ add new" website button
 */
function togglePlusMinus(addSiteButton) {
	if ($(addSiteButton).children('span:first-child').text() == "+") {
		$(addSiteButton).children('span:first-child').text("-");
	} else {
		$(addSiteButton).children('span:first-child').text("+");
	}
}

/**
 * Refreshes the list of websites associated with this solver
 */
function refreshSolverWebsites() {
	location.reload();
}

/**
 * Processes website data received from the server by creating HTML
 * for each website, adding a 'delete' button, and then injecting that HTML into the DOM

 function processWebsiteData(jsonData) {
	// Ensures the websites table is empty
	$('#websites tbody tr').remove();
	
	// Injects the clickable delete button that's always present
	$.each(jsonData, function(i, site) {
		$('#websites tbody').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src="'+starexecRoot+'images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
}*/

/**
 * Monitors the solver's "websites" and updates the server if the client adds/deletes any
 */
function attachWebsiteMonitor() {
	// Handles deleting an existing website
	$("#websites").on("click", ".delWebsite", function() {
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		$('#dialog-confirm-delete-txt')
		.text('Are you sure you want to delete this website?');

		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-confirm-delete').dialog('close');

					$.post(
						starexecRoot + "services/websites/delete/" + id,
						function(returnData) {
							s = parseReturnCode(returnData);
							if (s) {
								parent.remove();
							}

						},
						"json"
					).error(function() {
						showMessage('error',
							"Internal error updating websites",
							5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});

	});

	// Handles adding a new website
	$("#addWebsite").click(function() {
		var name = $("#website_name").val();
		var url = $("#website_url").val();

		if (name.trim().length == 0) {
			showMessage('error', 'please enter a website name', 6000);
			return;
		} else if (url.indexOf("http://") != 0 && url.indexOf("https://") != 0) {
			showMessage('error', 'url must start with http://', 6000);
			return;
		} else if (url.trim().length <= 12) {
			showMessage('error', 'the given url is not long enough', 6000);
			return;
		}

		var data = {name: name, url: url};
		$.post(
			starexecRoot + "services/website/add/solver/" + getParameterByName(
			"id"),
			data,
			function(returnCode) {
				s = parseReturnCode(returnCode);
				if (s) {
					$("#website_name").val("");
					$("#website_url").val("");
					$('#websites li').remove();
					refreshSolverWebsites();
				}
			},
			"json"
		);

	});
}
