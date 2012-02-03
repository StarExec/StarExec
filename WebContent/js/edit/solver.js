$(document).ready(function(){
	// Attach click listeners to both buttons
	initButtons();
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editSolverForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Validates solver fields
	$("#editSolverForm").validate({
		rules : {
			name : {
				required : true,
				regex : "^[\\w\\-\\.\\s]+$"
			},
			description : {
				required : true
			}
		},
		messages : {
			name : {
				required : "name required",
				regex : "invalid characters"
			},
			description : {
				required : "description required"
			}
		}
	});
	
	$('#delete').button({
		icons: {
			secondary: "ui-icon-minus"
    }});
	
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
    }});
});

function initButtons(){
	// Prompts user to confirm deletion and, if they confirm,
	// deletes the solver via AJAX, then redirects to explore/spaces.jsp
	$("#delete").click(function(){
		var confirm = window.confirm("are you sure you want to delete this solver?");
		if(confirm == true){
			$.post(
					"/starexec/services/delete/solver/" + getParameterByName("id"),
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/explore/spaces.jsp';
								break;
							case 1:
								showMessage('error', "solver was not deleted; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this solver can modify its details", 5000);
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

	// Triggers validation and, if that passes,
	// updates the solver details via AJAX, then redirects to edit/solver.jsp
	$("#update").click(function(){
		var isFormValid = $("#editSolverForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isDownloadable = $("#downloadable").is(':checked');
			var data = {name: name, description: description, downloadable: isDownloadable};
			$.post(
					"/starexec/services/edit/solver/" + getParameterByName("id"),
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/details/solver.jsp?id=' + getParameterByName("id");
								break;
							case 1:
								showMessage('error', "solver details were not updated; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this solver can modify its details", 5000);
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