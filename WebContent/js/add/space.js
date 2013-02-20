$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Attaches form validation to the name & description fields of add/space.jsp
 */
function attachFormValidation(){
	
	// Adds regular expression handling to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
			}
	);
	
	// Form validation rules/messages
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				maxlength: 64,
				regex : getPrimNameRegex()
			},
			desc: {
				required: false,
				maxlength: 1024,
				regex: getPrimDescRegex()
			}
		},
		messages: {
			name:{
				required: "a name is required",
				maxlength: "64 characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				required: "description required",
				maxlength: "1024 characters maximum",
				regex: "invalid character(s)"
			}
		}
	});
}


/**
 * Initializes the user-interface
 */
function initUI(){
	$('#btnCreate').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
}