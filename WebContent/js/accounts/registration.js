$(document).ready(function() {
	initUI();
	attachFormValidation();
});

/**
 * Initializes the user-interface
 */
function initUI(){
	checkForJavascript('#javascriptDisabled', '.registration');
	monitorTextarea("#reason", "describe your motivation for joining this community");

	$('#submit').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
		}
	});
	
	// Don't permit the default text as a message to the leaders by clearing it
	$("#submit").click(function(){
		if ($("#reason").val() == $("#reason").data('default')){
			$("#reason").val("");
	    }
	});
}

/**
 * Sets a default message for a textarea that is cleared when the textarea
 * receives focus and only returns if the textarea is empty when it loses focus
 */
function monitorTextarea(textarea, defaultText){
	
	// Set the default text
	$(textarea).data("default", defaultText);
	$(textarea).val($(textarea).data("default"));

	// Clear the textarea when clicked on if the text in there == the default text
	// and re-insert that default text if the user doesn't input anything
	$(textarea)
	  .focus(function() {
	        if (this.value === $(textarea).data("default")) {
	            this.value = "";
	        }
	  })
	  .blur(function() {
	        if (this.value === "") {
	            this.value = $(textarea).data("default");
	        }
	});
}

/**
 * How this works:
 * By default an error is show on the page and the desired element(s) have their visibility set to hidden.
 * This function, when called, hides the error element and displays the desired element(s) via javascript.
 * Therefore, users without javascript enabled will only ever see the error and never see the desired element(s).
 */
function checkForJavascript(errorElementToHide, desiredElementToShow){
	$(errorElementToHide).hide();
	$(desiredElementToShow).css('visibility','visible');
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
	
	// Hide the password strength meter initially and display it when a password's
	// strength has been calculated
	$.validator.passwordStrengthMeter("#pwd-meter");
	
	$("#regForm").validate({
		rules : {
			fn : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength 	: 32
			},
			ln : {
				required 	: true,
				regex 		: getUserNameRegex(),
				minlength 	: 2,
				maxlength 	: 32
			},
			em : {
				required 	: true,
				email 		: true
			},
			inst : {
				required 	: true,
				regex		: "^[\\w\\-\\s']+$",
				minlength 	: 2,
				maxlength 	: 64
			},
			pwd : {
				required: true,
				maxlength : 20,
				minlength : 5
			},
			confirm_password : {
				required 	: true,
				equalTo 	: "#password"
			},
			pat:{
				required	: true
			},
			cm: {
				required	: true
			},
			msg: {
				required	: true,
				regex 		: getPrimDescRegex(),
				minlength	: 2,
				maxlength	: 512
			}			
		},
		messages : {
			fn : {
				required  	: "enter a first name",
				minlength 	: "2 characters minimum",
				maxlength 	: "32 characters maximum",
				regex 		: "invalid character(s)"
			},
			ln : {
				required 	: "enter a last name",
				minlength 	: "2 characters minimum",
				maxlength 	: "32 characters maximum",
				regex 		: "invalid character(s)"
			},
			inst : {
				required 	: "enter your institution's name",
				minlength 	: "2 characters minimum",
				maxlength 	: "64 characters maximum",
				regex 		: "invalid character(s)"
			},
			pwd : {
				required : "please provide a password",
				maxlength : "20 characters maximum"
			},
			confirm_password : {
				required 	: "please provide a password",
				equalTo 	: "passwords don't match"
			},
			pat: {
				required	: "select an archive type"
			},
			em : {
				required 	: "enter a valid email address",
				email 		: "invalid email format"
			},
			cm : {
				required 	: "select a community to join"
			},
			msg : {
				minlength 	: "2 characters minimum",
				maxlength 	: "512 characters maximum",
				regex		: "unsafe character(s)",
				required 	: "enter your reason for joining"
			}
		},
		// Don't display an error for passwords, the password strength meter takes care of that
		errorPlacement : function(error, element) {
			if($(element).attr("id") != "password"){
				error.insertAfter(element);
			}
		}
	});
}
