$(document).ready(function(){
	
	// Setup user interface
	initGUI();
	
	// Attach from validation to the configuration upload form
	attachFormValidation();

});


/**
 * Setup the user interface buttons & actions
 */
function initGUI() {
	// Setup button icons
	$('#uploadBtn').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
	$('.cancelBtn').button({
		icons: {
			secondary: "ui-icon-closethick"
    }});
	$('#saveBtn').button({
		icons:{
			secondary: "ui-icon-disk"
		}
	});
	
	// If user clicks 'cancel', redirect to solver's details page
	$('.cancelBtn').click(function(){
		window.location = "/starexec/secure/details/solver.jsp?id=" + getParameterByName("sid");
	});
	
	$('#upload').expandable(false);
	$('#save').expandable(true);
}

/**
 * Validate the configuration upload form
 */
function attachFormValidation() {
	
	// Add regular expression function 'regex' to the JQuery validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Add validation to the configuration upload form
	$("#uploadConfigForm").validate({
		rules: {
			file: {
				required: true
			},
			name: {
				required: true,
				regex: "^[\\w\\-\\.\\s]+$"
			},
			description: { 
				required: true,
				regex: "^[a-zA-Z0-9\\-\\s_.!?/,\\\\+=\"'#$%&*()\\[{}\\]]+$"
			}
		},
		messages: {
			file: {
				required: "please select a file"
			},
			name: {
				required: "name required"
			},
			description: {
				required: "description required"
			}
		}
	});
	
	// Add validation to the configuration save form
	$("#saveConfigForm").validate({
		rules: {
			name: {
				required: true,
				regex: "^[\\w\\-\\.\\s]+$"
			},
			description: { 
				required: true,
				regex: "^[a-zA-Z0-9\\-\\s_.!?/,\\\\+=\"'#$%&*()\\[{}\\]]+$"
			},
			contents: {
				required: true
			}
		},
		messages: {
			name: {
				required: "name required",
				regex: "invalid characters"
			},
			description: {
				required: "description required",
				regex: "invalid characters"
			},
			contents: {
				required: "file can't be empty"
			}
		}
	});
}