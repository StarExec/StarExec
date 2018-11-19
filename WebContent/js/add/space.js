$(document).ready(function() {
	initUI();
	attachFormValidation();
});

/**
 * Attaches form validation to the name & description fields of add/space.jsp
 */
function attachFormValidation() {
	// Form validation rules/messages
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				maxlength: $("#txtName").attr("length"),
				jspregex: "DUMMY REGEX"
			},
			desc: {
				required: false,
				maxlength: $("#txtDesc").attr("length"),
				regex: getPrimDescRegex()
			}
		},
		messages: {
			name: {
				required: "a name is required",
				maxlength: $("#txtName").attr("length") + " characters maximum",
				jspregex: "invalid character(s)"
			},
			desc: {
				required: "description required",
				maxlength: $("#txtDesc").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			}
		}
	});
}

/**
 * Initializes the user-interface
 */
function initUI() {
	$('#btnCreate').button({
		icons: {
			secondary: "ui-icon-plus"
		}
	});

	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function() {
		history.back(-1);
	});
}
