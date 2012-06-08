$(document).ready(function() {
	initUI();
	attachFormValidation();
});

/**
 * Initializes the user-interface
 */
function initUI(){
	$('#submit').button({
		icons: {
			secondary: "ui-icon-arrowrefresh-1-s"
    }});
}

/**
 * Attaches form validation to the password reset fields
 */
function attachFormValidation(){
	// Adds regular expression handling to the jQuery Validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});

	
	// Form validation rules/messages	
	$("#resetForm").validate({
		rules : {
			fn : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength	: 32
			},
			ln : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength	: 32
			},
			em : {
				required 	: true,
				email 		: true
			}			
		},
		messages : {
			fn : {
				required 	: "enter your first name",
				minlength 	: "2 characters minimum",
				maxlength	: "32 characters maximum",
				regex 		: "invalid character(s)"
			},
			ln : {
				required 	: "enter your first name",
				minlength 	: "2 characters minimum",
				maxlength	: "32 characters maximum",
				regex 		: "invalid character(s)"
			},
			em : {
				required 	: "enter a valid email address",
				email 		: "invalid email format"
			}
		}
	});

}