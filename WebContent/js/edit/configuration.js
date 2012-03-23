$(document).ready(function(){
	
	// Setup the UI buttons/actions
	initUI();
	
	
});


/**
 * Sets up the UI buttons/actions 
 */
function initUI(){
	
	/* setup button icons and actions */
	initButtons();
	
}



/**
 * Sets up button icons and actions
 */
function initButtons(){
	
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
	
	// Setup form validation requirments
	initFormValidation();
	
	// Updates the database to reflect the newly inputed configuration details
	// when the 'update' button is pressed
	$("#updateConfig").click(function(){
		var isFormValid = $("#editConfigForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			var contents = $("#contents").val();
			$.post(
					"/starexec/services/edit/configuration/" + getParameterByName("id"),
					{ name: name, description: description, contents: contents },
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/details/configuration.jsp?id=' + getParameterByName("id");
								break;
							case 1:
//								showMessage('error', "configuration details and contents were not updated due to an internal error; please try again", 5000);
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

function initFormValidation(){
	
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
	
	// Configuration form validation constrains
	$("#editConfigForm").validate({
		rules : {
			name : {
				required : true,
				regex : "^[\\w\\-\\.\\s]+$"
			},
			description : {
				required : true
			},
			contents : { 
				required: true
			}
		},
		messages : {
			name : {
				required : "name required",
				regex : "invalid characters"
			},
			description : {
				required : "description required"
			},
			contents : {
				required : "input script's contents"
			}
		}
	});
}