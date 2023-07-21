$(document).ready(function() {
	initUI();
	attachFormValidation();
	//if this is not done, we have random tab characters in the textarea
	$("#saveConfigContents").text("");
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
	addValidators();

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
				jspregex: "DUMMY REGEX"
			},
			uploadConfigDesc: {
				maxlength: $("#uploadConfigDesc").attr("maxlength"),
				regex: getPrimDescRegex()
			}
		},
		messages: {
			file: {
				required: "please select a file"
			},
			uploadConfigName: {
				required: "name required",
				jspregex: "invalid character(s)"
			},
			uploadConfigDesc: {
				required: "description required",
				maxlength: "max length is " + $("#uploadConfigDesc")
				.attr("maxlength"),
				regex: "invalid character(s)"
			}
		}
	});

	// Add validation to the configuration save form
	$("#saveConfigForm").validate({
		rules: {
			saveConfigName: {
				required: true,
				jspregex: "DUMMY REGEX"
			},
			saveConfigDesc: {
				maxlength: $("#saveConfigDesc").attr("maxlength"),
				regex: getPrimDescRegex()
			},
			saveConfigContents: {
				required: true
			}
		},
		messages: {
			saveConfigName: {
				required: "name required",
				jspregex: "invalid characters"
			},
			saveConfigDesc: {
				required: "description required",
				maxlength: "max length is " + $("#saveConfigDesc")
				.attr("maxlength"),
				regex: "invalid characters"
			},
			saveConfigContents: {
				required: "file can't be empty"
			}
		}
	});
}
