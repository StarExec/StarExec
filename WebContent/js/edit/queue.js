$(document).ready(function() {
	onDescBoxUpdate();
	initUI();
	attachFormValidation();
	attachButtonActions();
});

/**
 * Initializes the user-interface
 */
function initUI() {
	// Setup JQuery button icons
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
}

function attachFormValidation() {
	
	addValidators();

	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editQueueForm").submit(function(e) {
		e.preventDefault();
	});

	// Form validation rules/messages
	$("#editQueueForm").validate({
		rules: {
			cpuTimeout: {
				required: true,
				min: 1
			},
			wallTimeout: {
				required: true,
				min: 1
			}
		},
		messages: {
			cpuTimeout: {
				required: "timeout required",
				min: "minimum of 1 second timeout"
			},
			wallTimeout: {
				required: "timeout required",
				min: "minimum of 1 second timeout"
			}
		}
	});
}

/**
 * Attaches the action for updating the queue
 */
function attachButtonActions() {
	$("#update").click(function() {
		var isFormValid = $("#editQueueForm").valid();
		if (isFormValid == true) {
			//Extract Relevant Data from Page
			var data =
				{
					cpuTimeout: $("#cpuTimeout").val(),
					wallTimeout: $("#wallTimeout").val(),
					description: $("#descTextBox").val()
				};
			//Pass data to server via AJAX
			$.post(
				starexecRoot + "services/edit/queue/" + getParameterByName("id"),
				data,
				function(returnCode) {
					s = parseReturnCode(returnCode);
					if (s) {
						window.location = starexecRoot + 'secure/admin/cluster.jsp';
					}
				},
				"json"
			);
		}
	});
}

function onDescBoxUpdate() {
	var descBox = $('#descTextBox');
	var numChars = descBox.val().length;
	var numCharsLable = $('#descCharRemaining');
	descCharRemaining.textContent = numChars + "/200 chars remaining";
}

