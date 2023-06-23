$(document).ready(function() {
	attachFormValidation();

	initUI();

	// Remove all unselected rows from the DOM before submitting
	$('#addForm').submit(function() {
		$('#tblNodes tbody')
		.children('tr')
		.not('.row_selected')
		.find('input')
		.remove();
	});

});

function initUI() {
	onDescBoxUpdate();

	$("#btnDone").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});

	// Set up datatables
	$('#tblNodes').dataTable({
		"sDom": 'rt<"bottom"f><"clear">',
		"bPaginate": false,
		"bSort": true
	});

	$("#tblNodes").on("click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

}

function attachFormValidation() {

	// Add regular expression capabilities to the validator
	addValidators();

	// Set up form validation
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: $("#txtQueueName").attr("length"),
				regex: getQueueNameRegex()
			},
			wallTimeout: {
				required: true,
				min: 1
			},
			cpuTimeout: {
				required: true,
				min: 1
			}
		},
		messages: {
			name: {
				required: "enter a queue name",
				minlength: "2 characters minimum",
				maxlength: $("#txtQueueName")
				.attr("length") + " characters maximum",
				regex: "invalid character(s)"
			}
		}
	});
}

	





function onDescBoxUpdate() {
	var descBox = $('#descTextBox');
	var numChars = descBox.val().length;
	var numCharsLable = $('#descCharRemaining');
	descCharRemaining.textContent = numChars + "/200 chars remaining";
}