$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	
	// Attach icons
	$('#cancel').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	$('#delete').button({
		icons: {
			secondary: "ui-icon-trash"
		}
	});
	
	
	$("#cancel").click(function() {
		window.location=href="/starexec/secure/edit/community.jsp?cid="+$("#cid").attr("value");
	});
	
	// Updates the database to reflect the newly inputed processor details
	// when the 'update' button is pressed 
	
	$("#update").click(function(){
		var isFormValid = $("#editProcForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			$.post(
					"/starexec/services/edit/processor/" + getParameterByName("id"),
					{ name: name, desc: description},
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/edit/community.jsp?cid=' + $("#cid").attr("value");
								break;
							case 1:
								showMessage('error', "there was an error entering the updated information into the database", 5000);
								break;
							case 2:
								showMessage('error', "only the leader of the community containing this processor can update it", 5000);
								break;
							case 3:
								showMessage('error', "invalid parameters; please ensure you fill out all of the processor file's fields", 5000);
								break;
						}
					},
					"json"
			);
		}
	});
	
	$("#delete").click(function(){
		var isFormValid = $("#editProcForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			$.post(
					"/starexec/services/delete/processor/" + getParameterByName("id"),
					{ name: name, description: description},
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/edit/community.jsp?cid=' + $("#cid").attr("value");
								break;
							case 1:
								showMessage('error', "there was an error entering the updated information into the database", 5000);
								break;
							case 2:
								showMessage('error', "only the leader of the community containing this processor can update it", 5000);
								break;
							case 3:
								showMessage('error', "invalid parameters; please ensure you fill out all of the processor file's fields", 5000);
								break;
						}
					},
					"json"
			);
		}
	});
}

/**
 * Attaches form validation to the 'edit processor' fields
 */
function attachFormValidation(){
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editProcForm").submit(function(e){
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
	$("#editProcForm").validate({
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
			}
		}
	});
}