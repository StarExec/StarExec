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
						switch (returnCode) {
							case 0:
								window.location = starexecRoot+'secure/details/configuration.jsp?id=' + getParameterByName("id");
								break;
							case 1:
								showMessage('error', "a configuration already exists for this solver with the name \""+name+"\"", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this configuration file's solver can modify these details and contents", 5000);
								break;
							case 3:
								showMessage('error', "invalid parameters; please ensure you fill out all of the configuration file's fields", 5000);
								break;
							case 4:
								showMessage('error', "a configuration already exists for this solver with the name \""+name+"\"", 5000);
								break;
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
				required : true,
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