$(document).ready(function() {
	initUI();
	attachFormValidation();

	$("#radioLocal").attr("checked", "checked");
	$("#fileLoc").show();
	$("#fileURL").hide();

	$("#radioUpload").attr("checked", "checked");
	$("#default").show();
	$("#fileLoc2").hide();
	$("#description").hide();
	$(".testJobRadio").change(function() {
		handleTestJobVisibility();
	});
	handleTestJobVisibility();
});

function handleTestJobVisibility() {
	if ($("#noTestJob").prop("checked")) {
		$("#settingRow").hide();
	} else {
		$("#settingRow").show();

	}
}

/**
 * Initializes user-interface
 */
function initUI() {
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}
	});

	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function(e) {
		e.preventDefault();
		history.back(-1);
	});

}

/**
 * Attaches form validation to the solver upload form
 */
function attachFormValidation() {

	$("#radioLocal").change(function() {
		if ($("#radioLocal").is(":checked")) {
			$("#fileURL").stop(true, true, true);
			$("#fileURL").fadeOut('fast', function() {
				if ($("#radioLocal").is(":checked")) {
					$("#fileLoc").fadeIn('fast');
					$("#upForm").validate().element("#fileURL");
				}

			});

		}
	});

	$("#radioURL").change(function() {
		if ($("#radioURL").is(":checked")) {
			$("#fileLoc").stop(true, true, true);
			$("#fileLoc").fadeOut('fast', function() {
				if ($("#radioURL").is(":checked")) {
					$("#fileURL").fadeIn('fast');
					$("#upForm").validate().element("#fileLoc");
				}

			});

		}
	});

	$("#radioUpload").change(function() {
		if ($("#radioUpload").is(":checked")) {
			$("#description").fadeOut('fast', function() {
				$("#fileLoc2").fadeOut('fast', function() {
					$("#default").fadeIn('fast');
				});
			});
		}
	});

	$("#radioText").change(function() {
		if ($("#radioText").is(":checked")) {
			$("#fileLoc2").fadeOut('fast', function() {
				$("#default").fadeOut('fast', function() {
					$("#description").fadeIn('fast');
				});
			});
		}
	});

	$("#radioFile").change(function() {
		if ($("#radioFile").is(":checked")) {
			$("#description").fadeOut('fast', function() {
				$("#default").fadeOut('fast', function() {
					$("#fileLoc2").fadeIn('fast');
				});
			});
		}
	});

	// Add regular expression handler to jQuery validator
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
	// Re-validate the 'solver location' field when it loses focus
	$("#fileLoc").change(function() {
		$("#fileLoc").blur().focus();
	});

	// Form validation rules/messages	
	$("#upForm").validate({
		rules: {
			f: {
				required: "#radioLocal:checked",
				jsregex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			url: {
				required: "#radioURL:checked",
				jsregex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			sn: {
				required: true,
				maxlength: $("#name").attr("length"),
				regex: "DUMMY REGEX"
			},
			desc: {
				maxlength: $("#description").attr("length"),
				jsregex: getPrimDescRegex()
			},
			d: {
				jsregex: "(\.txt$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				jsregex: ".zip, .tar and .tar.gz only"
			},
			url: {
				required: "please enter a URL",
				jsregex: "URL must be .zip, .tar, or .tar.gz"
			},
			sn: {
				required: "solver name required",
				maxlength: $("#name").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				maxlength: $("#description")
				.attr("length") + " characters maximum",
				jsregex: "invalid character(s)"
			},
			d: {
				jsregex: ".txt file only"
			}
		},
		submitHandler: function(form) {
			createDialog(
				"Uploading solver, please wait. This will take some time for large files.");
			form.submit();
		}
	});

}
