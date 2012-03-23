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
	$('#cancelBtn').button({
		icons: {
			secondary: "ui-icon-closethick"
    }});
	
	// If user clicks 'cancel', redirect to solver's details page
	$('#cancelBtn').click(function(){
		window.location = "/starexec/secure/details/solver.jsp?id=" + getParameterByName("sid");
	});
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
				required: true
			},
			description: { 
				required: true
			}
		},
		messages: {
			file: {
				required: "please select a file"
			},
			name: {
				required: "input a name"
			},
			description: {
				required: "description required"
			}
		}
	});
}