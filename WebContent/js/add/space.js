$(document).ready(function() {
	initUI();
	attachFormValidation();
});

/**
 * Attaches form validation to the name & description fields of add/space.jsp
 */
function attachFormValidation() {

	// Adds regular expression handling to validator
	$.validator.addMethod(
		"jsregex",
		function(value, element, regexp) {
			var re = new RegExp(regexp);
			return this.optional(element) || re.test(value);
		}
	);
	$.validator.addMethod(
		"regex",
		function(value, element, str) {
			return !element.validity.patternMismatch;
		});
	// Form validation rules/messages
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				maxlength: $("#txtName").attr("length"),
				regex: "DUMMY REGEX"
			},
			desc: {
				required: false,
				maxlength: $("#txtDesc").attr("length"),
				jsregex: getPrimDescRegex()
			}
		},
		messages: {
			name: {
				required: "a name is required",
				maxlength: $("#txtName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				required: "description required",
				maxlength: $("#txtDesc").attr("length") + " characters maximum",
				jsregex: "invalid character(s)"
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
