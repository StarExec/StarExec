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
			secondary: "ui-icon-arrowthick-1-e"
		}
	});
}


/**
 * Attaches form validation to the user registration fields
 */
function attachFormValidation(){	
	// Adds regular expression handling to jQuery Validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
		
	$("#regForm").validate({
		rules : {
			fn : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength 	: $("#firstname").attr("maxlength")
			},
			ln : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength 	: $("#lastname").attr("maxlength")
			},
			em : {
				required 	: true,
				email 		: true
			},
			inst : {
				required 	: true,
				regex		: "^[\\w\\-\\s']+$",
				minlength 	: 2,
				maxlength 	: $("#institution").attr("maxlength")
			},
			pwd : {
				required: true,
				maxlength : 20,
				minlength : 5
			},
			cm: {
				required	: true
			}			
		},
		messages : {
			fn : {
				required  	: "enter a first name",
				minlength 	: "2 characters minimum",
				maxlength 	: $("#firstname").attr("maxlength") + " characters maximum",
				regex 		: "invalid character(s)"
			},
			ln : {
				required 	: "enter a last name",
				minlength 	: "2 characters minimum",
				maxlength 	: $("#lastname").attr("maxlength") + " characters maximum",
				regex 		: "invalid character(s)"
			},
			inst : {
				required 	: "enter your institution's name",
				minlength 	: "2 characters minimum",
				maxlength 	: $("#institution").attr("maxlength") + " characters maximum",
				regex 		: "invalid character(s)"
			},
			pwd : {
				required : "please provide a password",
				maxlength : $("#password").attr("length") + " characters maximum"
			},			
			em : {
				required 	: "enter a valid email address",
				email 		: "invalid email format"
			},
			cm : {
				required 	: "select a community to join"
			}
		},
	});
}
