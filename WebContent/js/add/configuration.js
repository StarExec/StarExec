$(document).ready(function() {
	initUI();
	attachFormValidation();
});

/**
 * Setup the user interface buttons & actions
 */
function initUI() {
	// Setup button icons
	$('.uploadBtn').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});
	$('.cancelBtn').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('.saveBtn').button({
		icons: {
			secondary: "ui-icon-disk"
		}
	});

	// If user clicks 'cancel', redirect to solver's details page
	$('.cancelBtn').click(function() {
		window.location = starexecRoot + "secure/details/solver.jsp?id=" + getParameterByName(
			"sid");
	});

	$('#upload').expandable(false);
	$('#save').expandable(true);
}

/**
 * Attaches validation to the configuration upload form
 */
function attachFormValidation() {

	// Add regular expression handling to the JQuery validator
	$.validator.addMethod(
		"jsregex",
		function(value, element, regexp) {
			var re = new RegExp(regexp);
			return this.optional(element) || re.test(value);
		});
	$.validator.addMethod(
		"regex",
		function(value, element, str) {
			return !element.validity.patternMismatch;
		});
	// Re-validate the 'file location' field when it loses focus
	$("#configFile").change(function() {
		$("#configFile").blur().focus();
	});

	// Form validation rules/messages
	$("#uploadConfigForm").validate({
		rules: {
			file: {
				required: true
			},
			uploadConfigName: {
				required: true,
				regex: "DUMMY REGEX"
			},
			uploadConfigDesc: {
				maxlength: $("#uploadConfigDesc").attr("maxlength"),
				jsregex: getPrimDescRegex()
			}
		},
		messages: {
			file: {
				required: "please select a file"
			},
			uploadConfigName: {
				required: "name required",
				regex: "invalid character(s)"
			},
			uploadConfigDesc: {
				required: "description required",
				maxlength: "max length is " + $("#uploadConfigDesc")
				.attr("maxlength"),
				jsregex: "invalid character(s)"
			}
		}
	});

	// Add validation to the configuration save form
	$("#saveConfigForm").validate({
		rules: {
			saveConfigName: {
				required: true,
				regex: "DUMMY REGEX"
			},
			saveConfigDesc: {
				maxlength: $("#saveConfigDesc").attr("maxlength"),
				jsregex: getPrimDescRegex()
			},
			saveConfigContents: {
				required: true
			}
		},
		messages: {
			saveConfigName: {
				required: "name required",
				regex: "invalid characters"
			},
			saveConfigDesc: {
				required: "description required",
				maxlength: "max length is " + $("#saveConfigDesc")
				.attr("maxlength"),
				jsregex: "invalid characters"
			},
			saveConfigContents: {
				required: "file can't be empty"
			}
		}
	});
}
