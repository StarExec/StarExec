$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachButtonActions();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	$('#delete').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
}


/**
 * Attaches form validation to the 'edit benchmark' fields
 */
function attachFormValidation(){
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editBenchmarkForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds regular expression handling to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Form validation rules/messages
	$("#editBenchmarkForm").validate({
		rules : {
			name : {
				required : true,
				maxlength: 64,
				regex : getPrimNameRegex()
			},
			description : {
				required : true,
				maxlength: 1024,
				regex: getPrimDescRegex()
			}
		},
		messages : {
			name : {
				required : "name required",
				maxlength: "64 characters maximum",
				regex 	 : "invalid character(s)"
			},
			description : {
				required : "description required",
				maxlength: "1024 characters maximum",
				regex	 : "invalid character(s)"
			}
		}
	});
}

/**
 * Attaches actions to the 'update' and 'delete' buttons
 */
function attachButtonActions(){
	// If client clicks delete button first prompt them and, if they agree, then 
	// delete the benchmark via AJAX and redirect to /explore/spaces.jsp
	$("#delete").click(function(){
		var confirm = window.confirm("are you sure you want to delete this benchmark?");
		if(confirm == true){
			$.post(
					"/starexec/services/delete/benchmark/" + getParameterByName("id"),
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/explore/spaces.jsp';
								break;
							case 1:
								showMessage('error', "benchmark was not deleted; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this benchmark can modify its details", 5000);
								break;
							default:
								showMessage('error', "invalid parameters", 5000);
								break;
						}
					},
					"json"
			);
		}
	});
	

	// If the 'update' button is pressed then trigger validation and, if that passes,
	// update the benchmark details via AJAX and redirect to /details/benchmark.jsp 
	$("#update").click(function(){
		var isFormValid = $("#editBenchmarkForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isDownloadable = $("#downloadable").is(':checked');
			var type = $("#benchType").val();
			var data = {name: name, description: description, downloadable: isDownloadable, type: type};
			$.post(
					"/starexec/services/edit/benchmark/" + getParameterByName("id"),
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/details/benchmark.jsp?id=' + getParameterByName("id");
								break;
							case 1:
								showMessage('error', "benchmark details were not updated; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this benchmark can modify its details", 5000);
								break;
							default:
								showMessage('error', "invalid parameters", 5000);
								break;
						}
					},
					"json"
			);
		}
	});
}