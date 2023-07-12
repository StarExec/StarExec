var nodeTable;
var numNodes;

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

	$("#btnDone").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});

	$("#selectNRows").button({
		icons: {
			primary: "ui-icon-carat-2-n-s"
		}
	});

	$("#selectBetween").button({
		icons: {
			primary: "ui-icon-carat-2-n-s"
		}
	});

	$("#selectBetween").click(function() {
		selectAllBetween(nodeTable);
	});

	$("#selectNRows").click(function() {
		selectFirstN(nodeTable,numNodes);
	});

	// Set up datatables
	nodeTable = $('#tblNodes').dataTable({
		"sDom": 'rt<"bottom"f><"clear">',
		"bPaginate": false,
		"bSort": true
	});

	$("#tblNodes").on("click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

}

function numNodesOnBlur() {
	var input = document.getElementById("numNodes");
	numNodes = input.value;
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
				jspregex: "DUMMY REGEX"
			}
		},
		messages: {
			name: {
				required: "enter a queue name",
				minlength: "2 characters minimum",
				maxlength: $("#txtQueueName")
				.attr("length") + " characters maximum",
				jspregex: "invalid character(s)"
			}
		}
	});
}
