$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	
	// Attach icons
	$('#cancelConfig').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('#updateConfig').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	
	// Updates the database to reflect the newly inputed configuration details
	// when the 'update' button is pressed
	$("#updateConfig").click(function(){
		var isFormValid = $("#editConfigForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			var contents = $("#contents").val();
			$.post(
					starexecRoot+"services/edit/configuration/" + getParameterByName("id"),
					{ name: name, description: description, contents: contents },
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/details/configuration.jsp?id=' + getParameterByName("id");

						}
					},
					"json"
			);
		}
	});
}

/**
 * Attaches form validation to the 'edit configuration' fields
 */
function attachFormValidation(){
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editConfigForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds regular expression 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Form validation rules/messages
	$("#editConfigForm").validate({
		rules : {
			name : {
				required : true,
				maxlength: $("#name").attr("maxlength"),
				regex 	 : getPrimNameRegex()
			},
			description : {
				maxlength: $("#description").attr("length"),
				regex	 : getPrimDescRegex()
			},
			contents : { 
				required : true
			}
		},
		messages : {
			name : {
				required : "name required",
				maxlength: $("#name").attr("maxlength") + " characters maximum",
				regex 	 : "invalid character(s)"
			},
			description : {
				required : "description required",
				maxlength: $("#description").attr("length") + " characters maximum",
				regex 	 : "invalid character(s)"
			},
			contents : {
				required : "input script's contents"
			}
		}
	});
}